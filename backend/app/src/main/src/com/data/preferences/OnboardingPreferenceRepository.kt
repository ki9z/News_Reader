package com.data.preferences

import android.content.Context

/**
 * Stores first-run onboarding state outside Room so the splash flow can decide
 * where to route before database initialization completes.
 */
class OnboardingPreferenceRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun isOnboardingCompleted(): Boolean = prefs.getBoolean(KEY_COMPLETED, false)

    fun completeOnboarding() {
        prefs.edit().putBoolean(KEY_COMPLETED, true).apply()
    }

    fun reset() {
        prefs.edit().remove(KEY_COMPLETED).apply()
    }

    companion object {
        private const val PREF_NAME = "onboarding_preferences"
        private const val KEY_COMPLETED = "completed"
    }
}
