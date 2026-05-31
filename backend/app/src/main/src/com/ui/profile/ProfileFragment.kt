package com.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.R
import com.notifications.NotificationPermissionHelper
import com.databinding.FragmentProfileBinding
import com.ui.main.AdminActivity
import com.util.loadImage
import com.viewmodel.profile.ProfileViewModel
import com.viewmodel.profile.ProfileViewModelFactory
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels {
        val app = requireActivity().application as com.NewsApp
        ProfileViewModelFactory(app.userSettingsRepository, app.appDatabase.userDao(), app.tokenManager)
    }

    private var isBindingFromState = false

    private val languageItems = listOf(
        "en" to R.string.profile_language_english,
        "vi" to R.string.profile_language_vietnamese
    )

    private val textSizeItems = listOf(
        "S" to R.string.profile_text_size_small,
        "M" to R.string.profile_text_size_medium,
        "L" to R.string.profile_text_size_large
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        setupActions()
        observeData()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Luồng 1: Lắng nghe cài đặt người dùng
                launch {
                    viewModel.settings.collect { settings ->
                        isBindingFromState = true

                        binding.tvLanguageValue.text = languageLabel(settings.languageCode)
                        binding.tvTextSizeValue.text = textSizeLabel(settings.textSize)
                        binding.switchDarkMode.isChecked = settings.darkModeEnabled
                        binding.switchNotifications.isChecked = settings.notificationsEnabled
                        binding.switchDataSaver.isChecked = settings.dataSaverEnabled

                        // Render avatar nếu đang có user đăng nhập
                        if (viewModel.currentUser.value != null) {
                            renderAvatar(settings.avatarUrl)
                        }

                        isBindingFromState = false
                    }
                }

                // Luồng 2: Lắng nghe thay đổi User để "biến hình" giao diện (Khách/Đã đăng nhập)
                launch {
                    viewModel.currentUser.collect { user ->
                        if (user == null) {
                            // ==============================
                            // TRẠNG THÁI KHÁCH
                            // ==============================
                            binding.tvDisplayName.text = "Khách"
                            binding.tvEmail.text = "Vui lòng đăng nhập để sử dụng đầy đủ tính năng"

                            binding.btnLogin.visibility = View.VISIBLE
                            binding.btnLogout.visibility = View.GONE
                            binding.rowAdmin.visibility = View.GONE

                            renderAvatar("")
                        } else {
                            // ==============================
                            // TRẠNG THÁI ĐÃ ĐĂNG NHẬP
                            // ==============================
                            binding.tvDisplayName.text = user.fullName
                            binding.tvEmail.text = user.email

                            binding.btnLogin.visibility = View.GONE
                            binding.btnLogout.visibility = View.VISIBLE

                            renderAvatar(viewModel.settings.value.avatarUrl)

                            // Cập nhật quyền Admin
                            val currentAuthType = viewModel.settings.value.currentAuthType
                            val isAdmin = user.role == "admin" || currentAuthType == "admin"
                            binding.rowAdmin.visibility = if (isAdmin) View.VISIBLE else View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun setupActions() {
        binding.btnLogin.setOnClickListener {
            val intent = Intent(requireContext(), AdminActivity::class.java)
            startActivity(intent)
        }

        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_accountSettingsFragment)
        }

        binding.rowSavedArticles.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_bookmarkFragment)
        }
        binding.rowReadingHistory.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_readingHistoryFragment)
        }
        binding.rowFollowingTopics.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_followingTopicsFragment)
        }
        binding.rowDownloads.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_downloadsFragment)
        }

        binding.rowAdmin.setOnClickListener {
            val intent = Intent(requireContext(), AdminActivity::class.java)
            startActivity(intent)
        }

        binding.rowLanguage.setOnClickListener {
            showSingleChoiceDialog(
                titleRes = R.string.profile_language,
                options = languageItems,
                selectedCode = viewModel.settings.value.languageCode
            ) { selectedCode ->
                viewModel.setLanguage(selectedCode)
                applyLanguage(selectedCode)
                toast(getString(R.string.profile_language_updated))
            }
        }

        binding.rowTextSize.setOnClickListener {
            showSingleChoiceDialog(
                titleRes = R.string.profile_text_size,
                options = textSizeItems,
                selectedCode = viewModel.settings.value.textSize
            ) { selectedSize ->
                viewModel.setTextSize(selectedSize)
                toast(getString(R.string.profile_text_size_updated))
            }
        }

        binding.rowAccountSettings.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_accountSettingsFragment)
        }
        binding.rowPrivacyPolicy.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_privacyPolicyFragment)
        }
        binding.rowHelpSupport.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_helpSupportFragment)
        }

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
            toast(getString(R.string.profile_logout_success))
            // SỬA LỖI: Đã xóa lệnh popBackStack về Home. Khi ấn Logout, giao diện sẽ tự chuyển sang Khách ngay tại tab Profile này luôn!
        }

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isBindingFromState) return@setOnCheckedChangeListener
            viewModel.setDarkModeEnabled(isChecked)
            applyDarkMode(isChecked)
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isBindingFromState) return@setOnCheckedChangeListener
            if (isChecked) NotificationPermissionHelper.ensurePermission(this)
            viewModel.setNotificationsEnabled(isChecked)
        }

        binding.switchDataSaver.setOnCheckedChangeListener { _, isChecked ->
            if (isBindingFromState) return@setOnCheckedChangeListener
            viewModel.setDataSaverEnabled(isChecked)
        }
    }

    private fun openEditProfileDialog() {
        val currentSettings = viewModel.settings.value
        val currentUser = viewModel.currentUser.value

        val nameToShow = currentUser?.fullName ?: currentSettings.displayName
        val emailToShow = currentUser?.email ?: currentSettings.email

        showEditProfileDialog(currentSettings, nameToShow, emailToShow) { input ->
            viewModel.updateProfile(
                displayName = input.displayName,
                email = input.email,
                avatarUrl = input.avatarUrl,
                occupation = input.occupation,
                location = input.location,
                birthday = input.birthday,
                bio = input.bio,
                interests = input.interests
            )
            toast(getString(R.string.profile_saved))
        }
    }

    private fun renderAvatar(url: String) {
        if (url.isBlank()) {
            binding.ivAvatar.setImageResource(android.R.drawable.ic_menu_myplaces)
            return
        }
        binding.ivAvatar.loadImage(url)
    }

    private fun languageLabel(code: String): String {
        return when (code) {
            "vi" -> getString(R.string.profile_language_vietnamese)
            else -> getString(R.string.profile_language_english)
        }
    }

    private fun textSizeLabel(code: String): String {
        return when (code) {
            "S" -> getString(R.string.profile_text_size_small)
            "L" -> getString(R.string.profile_text_size_large)
            else -> getString(R.string.profile_text_size_medium)
        }
    }

    private fun applyLanguage(languageCode: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode))
    }

    private fun applyDarkMode(enabled: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}