package com.ui.profile

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
import com.databinding.FragmentAccountSettingsBinding
import com.viewmodel.profile.ProfileViewModel
import com.viewmodel.profile.ProfileViewModelFactory
import kotlinx.coroutines.launch

class AccountSettingsFragment : Fragment(R.layout.fragment_account_settings) {

    private var _binding: FragmentAccountSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels {
        val app = requireActivity().application as com.NewsApp
        ProfileViewModelFactory(
            app.userSettingsRepository,
            app.appDatabase.userDao(),
            app.tokenManager
        )
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

    private val regionItems = listOf(
        "us" to R.string.profile_region_us,
        "vn" to R.string.profile_region_vn,
        "gb" to R.string.profile_region_gb,
        "au" to R.string.profile_region_au
    )

    private val defaultTabItems = listOf(
        "home" to R.string.profile_default_tab_home,
        "local" to R.string.profile_default_tab_local,
        "search" to R.string.profile_default_tab_search,
        "bookmark" to R.string.profile_default_tab_bookmark
    )

    private val articleStyleItems = listOf(
        "compact" to R.string.profile_article_style_compact,
        "comfortable" to R.string.profile_article_style_comfortable,
        "reader" to R.string.profile_article_style_reader
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAccountSettingsBinding.bind(view)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        setupActions()
        observeSettings()
    }

    private fun setupActions() {
        binding.btnEditProfile.setOnClickListener {
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

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isBindingFromState) return@setOnCheckedChangeListener
            if (isChecked) NotificationPermissionHelper.ensurePermission(this)
            viewModel.setNotificationsEnabled(isChecked)
        }

        binding.switchPersonalization.setOnCheckedChangeListener { _, isChecked ->
            if (isBindingFromState) return@setOnCheckedChangeListener
            viewModel.setPersonalizationEnabled(isChecked)
        }

        binding.switchBreaking.setOnCheckedChangeListener { _, isChecked ->
            if (isBindingFromState) return@setOnCheckedChangeListener
            viewModel.setBreakingNewsEnabled(isChecked)
        }

        binding.switchDigest.setOnCheckedChangeListener { _, isChecked ->
            if (isBindingFromState) return@setOnCheckedChangeListener
            viewModel.setDailyDigestEnabled(isChecked)
        }

        binding.switchSyncHistory.setOnCheckedChangeListener { _, isChecked ->
            if (isBindingFromState) return@setOnCheckedChangeListener
            viewModel.setSyncHistoryEnabled(isChecked)
        }

        binding.rowLanguage.setOnClickListener {
            showSingleChoiceDialog(
                titleRes = R.string.profile_language,
                options = languageItems,
                selectedCode = viewModel.settings.value.languageCode
            ) { selectedCode ->
                viewModel.setLanguage(selectedCode)
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(selectedCode))
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

        binding.rowRegion.setOnClickListener {
            showSingleChoiceDialog(
                titleRes = R.string.profile_region_country,
                options = regionItems,
                selectedCode = viewModel.settings.value.regionCountry
            ) { selectedRegion ->
                viewModel.setRegionCountry(selectedRegion)
                toast(getString(R.string.profile_setting_updated))
            }
        }

        binding.rowDefaultTab.setOnClickListener {
            showSingleChoiceDialog(
                titleRes = R.string.profile_default_start_tab,
                options = defaultTabItems,
                selectedCode = viewModel.settings.value.defaultStartTab
            ) { selectedTab ->
                viewModel.setDefaultStartTab(selectedTab)
                toast(getString(R.string.profile_setting_updated))
            }
        }

        binding.rowArticleStyle.setOnClickListener {
            showSingleChoiceDialog(
                titleRes = R.string.profile_article_view_style,
                options = articleStyleItems,
                selectedCode = viewModel.settings.value.articleStyle
            ) { selectedStyle ->
                viewModel.setArticleStyle(selectedStyle)
                toast(getString(R.string.profile_setting_updated))
            }
        }

        binding.rowDataPrivacy.setOnClickListener {
            findNavController().navigate(R.id.privacyPolicyFragment)
        }

        binding.rowManageBlocked.setOnClickListener {
            findNavController().navigate(R.id.followingTopicsFragment)
        }

        binding.rowExportData.setOnClickListener {
            toast(getString(R.string.profile_export_data))
        }

        binding.rowDeleteAccount.setOnClickListener {
            viewModel.clearLocalAccountData()
            toast(getString(R.string.profile_local_data_deleted))
        }
    }

    private fun observeSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Luồng 1: Lắng nghe Cài đặt (Theme, Ngôn ngữ, Thông báo...)
                launch {
                    viewModel.settings.collect { settings ->
                        isBindingFromState = true

                        // Lưu ý: Không lấy Tên và Email từ settings nữa
                        binding.switchNotifications.isChecked = settings.notificationsEnabled
                        binding.switchPersonalization.isChecked = settings.personalizationEnabled
                        binding.switchBreaking.isChecked = settings.breakingNewsEnabled
                        binding.switchDigest.isChecked = settings.dailyDigestEnabled
                        binding.switchSyncHistory.isChecked = settings.syncHistoryEnabled
                        binding.tvLanguage.text = languageLabel(settings.languageCode)
                        binding.tvTextSize.text = textSizeLabel(settings.textSize)
                        binding.tvRegion.text = regionLabel(settings.regionCountry)
                        binding.tvDefaultTab.text = defaultTabLabel(settings.defaultStartTab)
                        binding.tvArticleStyle.text = articleStyleLabel(settings.articleStyle)

                        isBindingFromState = false
                    }
                }

                // Luồng 2: Lắng nghe thông tin User thực tế từ Database
                launch {
                    viewModel.currentUser.collect { user ->
                        if (user != null) {
                            // Đã đăng nhập: Lấy tên và email từ DB
                            binding.tvDisplayName.text = user.fullName
                            binding.tvEmail.text = user.email
                        } else {
                            // Chưa đăng nhập (Khách)
                            binding.tvDisplayName.text = "Khách"
                            binding.tvEmail.text = "Vui lòng đăng nhập"
                        }
                    }
                }
            }
        }
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

    private fun regionLabel(code: String): String {
        return when (code) {
            "vn" -> getString(R.string.profile_region_vn)
            "gb" -> getString(R.string.profile_region_gb)
            "au" -> getString(R.string.profile_region_au)
            else -> getString(R.string.profile_region_us)
        }
    }

    private fun defaultTabLabel(code: String): String {
        return when (code) {
            "local" -> getString(R.string.profile_default_tab_local)
            "search" -> getString(R.string.profile_default_tab_search)
            "bookmark" -> getString(R.string.profile_default_tab_bookmark)
            else -> getString(R.string.profile_default_tab_home)
        }
    }

    private fun articleStyleLabel(code: String): String {
        return when (code) {
            "compact" -> getString(R.string.profile_article_style_compact)
            "reader" -> getString(R.string.profile_article_style_reader)
            else -> getString(R.string.profile_article_style_comfortable)
        }
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}