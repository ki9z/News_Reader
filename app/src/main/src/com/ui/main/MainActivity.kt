package com.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.R
import com.data.preferences.OnboardingPreferenceRepository
import com.data.settings.UserSettings
import com.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var defaultStartTabApplied = false
    private var latestSettings: UserSettings? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showLastCrashIfAny()
        setupNavigation()
        observeUserSettings()
        handleNotificationIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun showLastCrashIfAny() {
        val crashText = com.util.CrashLogger.readAndClear(this) ?: return
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.app_name))
            .setMessage("Previous crash detected:\n\n$crashText")
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isTopLevel = destination.id in setOf(
                R.id.homeFragment,
                R.id.localFragment,
                R.id.searchFragment,
                R.id.profileFragment
            )
            binding.bottomNavigation.visibility = if (isTopLevel) View.VISIBLE else View.GONE

            val settings = latestSettings
            if (destination.id == R.id.homeFragment && settings != null) {
                applyDefaultStartTab(settings.defaultStartTab)
            }
        }
    }

    private fun observeUserSettings() {
        val app = application as com.NewsApp
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                app.userSettingsRepository.userSettingsFlow.collect { settings ->
                    latestSettings = settings
                    AppCompatDelegate.setDefaultNightMode(
                        if (settings.darkModeEnabled) {
                            AppCompatDelegate.MODE_NIGHT_YES
                        } else {
                            AppCompatDelegate.MODE_NIGHT_NO
                        }
                    )
                    AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.forLanguageTags(settings.languageCode)
                    )

                    if (navController.currentDestination?.id == R.id.homeFragment) {
                        applyDefaultStartTab(settings.defaultStartTab)
                    }
                }
            }
        }
    }

    private fun applyDefaultStartTab(tab: String) {
        if (defaultStartTabApplied) return
        if (!OnboardingPreferenceRepository(this).isOnboardingCompleted()) return
        defaultStartTabApplied = true

        val destinationId = when (tab.lowercase()) {
            "local" -> R.id.localFragment
            "search" -> R.id.searchFragment
            "bookmark" -> R.id.bookmarkFragment
            "profile" -> R.id.profileFragment
            else -> R.id.homeFragment
        }

        if (navController.currentDestination?.id != destinationId) {
            navController.navigate(destinationId)
        }
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val articleUrl = intent?.getStringExtra(EXTRA_ARTICLE_URL)?.trim().orEmpty()
        if (articleUrl.isBlank()) return
        if (!articleUrl.startsWith("https://", ignoreCase = true)) return

        intent?.removeExtra(EXTRA_ARTICLE_URL)
        navController.navigate(
            R.id.articleWebViewFragment,
            bundleOf(
                "title" to getString(R.string.profile_push_notifications),
                "articleUrl" to articleUrl,
                "startInReaderMode" to false
            )
        )
    }

    companion object {
        const val EXTRA_ARTICLE_URL = "articleUrl"
    }
}
