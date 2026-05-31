package com.ui.profile

import android.app.AlertDialog
import android.util.Patterns
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import com.R
import com.data.settings.UserSettings
import com.databinding.DialogEditProfileBinding

data class EditProfileInput(
    val displayName: String,
    val email: String,
    val avatarUrl: String,
    val occupation: String,
    val location: String,
    val birthday: String,
    val interests: String,
    val bio: String
)

fun Fragment.showEditProfileDialog(
    current: UserSettings,
    realName: String,
    realEmail: String,
    onSave: (EditProfileInput) -> Unit
) {
    val binding = DialogEditProfileBinding.inflate(LayoutInflater.from(requireContext()))

    binding.etName.setText(realName)
    binding.etEmail.setText(realEmail)

    binding.etAvatarUrl.setText(current.avatarUrl)
    binding.etOccupation.setText(current.occupation)
    binding.etLocation.setText(current.location)
    binding.etBirthday.setText(current.birthday)
    binding.etInterests.setText(current.interests)
    binding.etBio.setText(current.bio)

    val dialog = AlertDialog.Builder(requireContext())
        .setTitle(R.string.profile_edit_dialog_title)
        .setView(binding.root)
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(R.string.profile_save, null)
        .create()

    dialog.setOnShowListener {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val input = EditProfileInput(
                displayName = binding.etName.text?.toString()?.trim().orEmpty(),
                email = binding.etEmail.text?.toString()?.trim().orEmpty(),
                avatarUrl = binding.etAvatarUrl.text?.toString()?.trim().orEmpty(),
                occupation = binding.etOccupation.text?.toString()?.trim().orEmpty(),
                location = binding.etLocation.text?.toString()?.trim().orEmpty(),
                birthday = binding.etBirthday.text?.toString()?.trim().orEmpty(),
                interests = binding.etInterests.text?.toString()?.trim().orEmpty(),
                bio = binding.etBio.text?.toString()?.trim().orEmpty()
            )

            binding.tilName.error = null
            binding.tilEmail.error = null
            binding.tilAvatarUrl.error = null
            binding.tilBirthday.error = null

            if (input.displayName.isBlank()) {
                binding.tilName.error = getString(R.string.profile_error_name_required)
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(input.email).matches()) {
                binding.tilEmail.error = getString(R.string.profile_error_email_invalid)
                return@setOnClickListener
            }
            if (input.avatarUrl.isNotBlank() && !isValidAvatarUrl(input.avatarUrl)) {
                binding.tilAvatarUrl.error = getString(R.string.profile_error_avatar_invalid)
                return@setOnClickListener
            }
            if (input.birthday.isNotBlank() && !isValidBirthday(input.birthday)) {
                binding.tilBirthday.error = getString(R.string.profile_error_birthday_invalid)
                return@setOnClickListener
            }

            onSave(input)
            dialog.dismiss()
        }
    }

    dialog.show()
}

fun Fragment.showSingleChoiceDialog(
    titleRes: Int,
    options: List<Pair<String, Int>>,
    selectedCode: String,
    onPick: (String) -> Unit
) {
    val codes = options.map { it.first }
    val labels = options.map { getString(it.second) }.toTypedArray()
    val checkedIndex = codes.indexOf(selectedCode).takeIf { it >= 0 } ?: 0

    AlertDialog.Builder(requireContext())
        .setTitle(titleRes)
        .setSingleChoiceItems(labels, checkedIndex) { dialog, which ->
            onPick(codes[which])
            dialog.dismiss()
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
}

private fun isValidAvatarUrl(url: String): Boolean {
    val isHttp = url.startsWith("http://", ignoreCase = true)
    val isHttps = url.startsWith("https://", ignoreCase = true)
    return (isHttp || isHttps) && Patterns.WEB_URL.matcher(url).matches()
}

private fun isValidBirthday(value: String): Boolean {
    return Regex("^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/\\d{4}$").matches(value)
}

