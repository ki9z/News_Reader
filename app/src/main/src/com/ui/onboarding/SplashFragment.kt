package com.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.R
import com.data.preferences.OnboardingPreferenceRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashFragment : Fragment(R.layout.fragment_splash) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            delay(SPLASH_DELAY_MS)
            val navController = findNavController()
            if (navController.currentDestination?.id != R.id.splashFragment) return@launch

            val onboardingPrefs = OnboardingPreferenceRepository(requireContext())
            val action = if (onboardingPrefs.isOnboardingCompleted()) {
                R.id.action_splashFragment_to_homeFragment
            } else {
                R.id.action_splashFragment_to_onboardingFragment
            }
            navController.navigate(action)
        }
    }

    companion object {
        private const val SPLASH_DELAY_MS = 850L
    }
}
