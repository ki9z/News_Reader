package com.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.NewsApp
import com.R
import com.databinding.FragmentHelpSupportBinding
import com.util.Constants
import kotlinx.coroutines.launch

class HelpSupportFragment : Fragment(R.layout.fragment_help_support) {

    private var _binding: FragmentHelpSupportBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHelpSupportBinding.bind(view)

        val app = requireActivity().application as NewsApp

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.tvContent.setOnLongClickListener {
            openSupportMail(
                subject = getString(R.string.profile_help_email_subject),
                body = buildSupportBody("Support request")
            )
            true
        }

        binding.faqAccount.setOnClickListener {
            findNavController().navigate(R.id.accountSettingsFragment)
        }
        binding.faqHistory.setOnClickListener {
            findNavController().navigate(R.id.readingHistoryFragment)
        }
        binding.faqDownloads.setOnClickListener {
            findNavController().navigate(R.id.downloadsFragment)
        }
        binding.faqNotifications.setOnClickListener {
            findNavController().navigate(R.id.accountSettingsFragment)
        }
        binding.faqPrivacy.setOnClickListener {
            findNavController().navigate(R.id.privacyPolicyFragment)
        }

        binding.btnContactSupport.setOnClickListener {
            openSupportMail(
                subject = getString(R.string.profile_help_email_subject),
                body = buildSupportBody("Support request")
            )
        }
        binding.btnReportBug.setOnClickListener {
            openSupportMail(
                subject = getString(R.string.profile_help_report_bug),
                body = buildSupportBody("Bug report")
            )
        }
        binding.btnSendFeedback.setOnClickListener {
            openSupportMail(
                subject = getString(R.string.profile_help_send_feedback),
                body = buildSupportBody("Feedback")
            )
        }

        binding.btnClearCache.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val removed = app.profileRepository.clearNewsCache()
                Toast.makeText(
                    requireContext(),
                    getString(R.string.profile_help_cache_cleared, removed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        binding.btnRefreshRecommendations.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                app.profileRepository.resetFollowingTopics()
                app.followPreferenceRepository.clear()
                Toast.makeText(
                    requireContext(),
                    R.string.profile_help_recommendations_refreshed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        binding.btnResyncAccount.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                app.profileRepository.refreshLocalAccount()
                Toast.makeText(
                    requireContext(),
                    R.string.profile_help_account_resynced,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun openSupportMail(subject: String, body: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:support@newsreader.app")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(requireContext(), R.string.profile_help_no_mail_app, Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildSupportBody(type: String): String {
        return buildString {
            appendLine("Type: $type")
            appendLine("App: News Reader Android")
            appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("API mode: ${if (Constants.USES_BACKEND_PROXY) "Backend proxy" else "Direct NewsAPI"}")
            appendLine("Base URL: ${Constants.BASE_URL}")
            appendLine()
            appendLine("Describe your issue or feedback here:")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
