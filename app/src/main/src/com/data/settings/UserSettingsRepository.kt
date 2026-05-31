package com.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userSettingsDataStore by preferencesDataStore(name = "user_settings")

class UserSettingsRepository(
    private val context: Context
) {

    val userSettingsFlow: Flow<UserSettings> = context.userSettingsDataStore.data.map { prefs ->
        UserSettings(
            darkModeEnabled = prefs[KEY_DARK_MODE] ?: false,
            notificationsEnabled = prefs[KEY_NOTIFICATIONS] ?: true,
            dataSaverEnabled = prefs[KEY_DATA_SAVER] ?: false,
            languageCode = prefs[KEY_LANGUAGE] ?: DEFAULT_LANGUAGE,
            textSize = prefs[KEY_TEXT_SIZE] ?: DEFAULT_TEXT_SIZE,
            displayName = prefs[KEY_DISPLAY_NAME] ?: DEFAULT_DISPLAY_NAME,
            email = prefs[KEY_EMAIL] ?: DEFAULT_EMAIL,
            avatarUrl = prefs[KEY_AVATAR_URL] ?: "",
            occupation = prefs[KEY_OCCUPATION] ?: "",
            location = prefs[KEY_LOCATION] ?: "",
            birthday = prefs[KEY_BIRTHDAY] ?: "",
            bio = prefs[KEY_BIO] ?: "",
            interests = prefs[KEY_INTERESTS] ?: "",
            isSignedIn = prefs[KEY_IS_SIGNED_IN] ?: false,
            primaryAuthProvider = prefs[KEY_PRIMARY_PROVIDER] ?: "",
            linkedProviders = decodeProviders(prefs[KEY_LINKED_PROVIDERS]),
            maskedPhone = prefs[KEY_MASKED_PHONE] ?: "",
            trackReadingHistory = prefs[KEY_TRACK_READING_HISTORY] ?: true,
            personalizationEnabled = prefs[KEY_PERSONALIZATION] ?: true,
            regionCountry = prefs[KEY_REGION_COUNTRY] ?: DEFAULT_REGION_COUNTRY,
            defaultStartTab = prefs[KEY_DEFAULT_START_TAB] ?: DEFAULT_START_TAB,
            articleStyle = prefs[KEY_ARTICLE_STYLE] ?: DEFAULT_ARTICLE_STYLE,
            breakingNewsEnabled = prefs[KEY_BREAKING_NEWS] ?: true,
            dailyDigestEnabled = prefs[KEY_DAILY_DIGEST] ?: true,
            syncHistoryEnabled = prefs[KEY_SYNC_HISTORY] ?: false,
            currentAuthType = prefs[KEY_AUTH_TYPE] ?: "user"
        )
    }

    suspend fun setDarkModeEnabled(enabled: Boolean) {
        updateBoolean(KEY_DARK_MODE, enabled)
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        updateBoolean(KEY_NOTIFICATIONS, enabled)
    }

    suspend fun setLanguage(languageCode: String) {
        context.userSettingsDataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = languageCode
        }
    }

    suspend fun setTextSize(textSize: String) {
        context.userSettingsDataStore.edit { prefs ->
            prefs[KEY_TEXT_SIZE] = textSize
        }
    }

    suspend fun setDataSaverEnabled(enabled: Boolean) {
        updateBoolean(KEY_DATA_SAVER, enabled)
    }

    suspend fun setTrackReadingHistory(enabled: Boolean) {
        updateBoolean(KEY_TRACK_READING_HISTORY, enabled)
    }

    suspend fun setPersonalizationEnabled(enabled: Boolean) {
        updateBoolean(KEY_PERSONALIZATION, enabled)
    }

    suspend fun setRegionCountry(countryCode: String) {
        context.userSettingsDataStore.edit { prefs ->
            prefs[KEY_REGION_COUNTRY] = countryCode
        }
    }

    suspend fun setDefaultStartTab(tab: String) {
        context.userSettingsDataStore.edit { prefs ->
            prefs[KEY_DEFAULT_START_TAB] = tab
        }
    }

    suspend fun setArticleStyle(style: String) {
        context.userSettingsDataStore.edit { prefs ->
            prefs[KEY_ARTICLE_STYLE] = style
        }
    }

    suspend fun setBreakingNewsEnabled(enabled: Boolean) {
        updateBoolean(KEY_BREAKING_NEWS, enabled)
    }

    suspend fun setDailyDigestEnabled(enabled: Boolean) {
        updateBoolean(KEY_DAILY_DIGEST, enabled)
    }

    suspend fun setSyncHistoryEnabled(enabled: Boolean) {
        updateBoolean(KEY_SYNC_HISTORY, enabled)
    }

    suspend fun clearLocalAccountData() {
        context.userSettingsDataStore.edit { prefs ->
            prefs.clear()
        }
    }

    suspend fun updateProfile(
        displayName: String,
        email: String,
        avatarUrl: String,
        occupation: String,
        location: String,
        birthday: String,
        bio: String,
        interests: String
    ) {
        context.userSettingsDataStore.edit { prefs ->
            prefs[KEY_DISPLAY_NAME] = displayName
            prefs[KEY_EMAIL] = email
            prefs[KEY_AVATAR_URL] = avatarUrl
            prefs[KEY_OCCUPATION] = occupation
            prefs[KEY_LOCATION] = location
            prefs[KEY_BIRTHDAY] = birthday
            prefs[KEY_BIO] = bio
            prefs[KEY_INTERESTS] = interests
        }
    }

    suspend fun signInWithProvider(provider: AuthProvider, phone: String? = null) {
        context.userSettingsDataStore.edit { prefs ->
            val linked = decodeProviders(prefs[KEY_LINKED_PROVIDERS]).toMutableSet()
            linked.add(provider.code)
            prefs[KEY_IS_SIGNED_IN] = true
            prefs[KEY_PRIMARY_PROVIDER] = provider.code
            prefs[KEY_LINKED_PROVIDERS] = encodeProviders(linked)
            if (provider == AuthProvider.PHONE) {
                prefs[KEY_MASKED_PHONE] = maskPhone(phone)
            }
        }
    }

    suspend fun linkProvider(provider: AuthProvider) {
        context.userSettingsDataStore.edit { prefs ->
            val linked = decodeProviders(prefs[KEY_LINKED_PROVIDERS]).toMutableSet()
            linked.add(provider.code)
            prefs[KEY_LINKED_PROVIDERS] = encodeProviders(linked)
            if (prefs[KEY_PRIMARY_PROVIDER].isNullOrBlank()) {
                prefs[KEY_PRIMARY_PROVIDER] = provider.code
            }
            if (linked.isNotEmpty()) {
                prefs[KEY_IS_SIGNED_IN] = true
            }
        }
    }

    suspend fun unlinkProvider(provider: AuthProvider) {
        context.userSettingsDataStore.edit { prefs ->
            val linked = decodeProviders(prefs[KEY_LINKED_PROVIDERS]).toMutableSet()
            linked.remove(provider.code)

            prefs[KEY_LINKED_PROVIDERS] = encodeProviders(linked)
            if (prefs[KEY_PRIMARY_PROVIDER] == provider.code) {
                prefs[KEY_PRIMARY_PROVIDER] = linked.firstOrNull().orEmpty()
            }
            if (linked.isEmpty()) {
                prefs[KEY_IS_SIGNED_IN] = false
                prefs[KEY_PRIMARY_PROVIDER] = ""
                prefs[KEY_MASKED_PHONE] = ""
            }
        }
    }

    suspend fun logout() {
        context.userSettingsDataStore.edit { prefs ->
            prefs[KEY_IS_SIGNED_IN] = false
            prefs[KEY_PRIMARY_PROVIDER] = ""
            prefs[KEY_LINKED_PROVIDERS] = ""
            prefs[KEY_MASKED_PHONE] = ""
        }
    }

    suspend fun setAuthType(authType: String) {
        context.userSettingsDataStore.edit { prefs ->
            prefs[KEY_AUTH_TYPE] = authType
        }
    }

    private suspend fun updateBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        context.userSettingsDataStore.edit { prefs ->
            prefs[key] = value
        }
    }

    companion object {
        const val DEFAULT_LANGUAGE = "en"
        const val DEFAULT_TEXT_SIZE = "M"
        const val DEFAULT_DISPLAY_NAME = "Guest User"
        const val DEFAULT_EMAIL = "guest@newsreader.app"

        const val DEFAULT_REGION_COUNTRY = "us"
        const val DEFAULT_START_TAB = "home"
        const val DEFAULT_ARTICLE_STYLE = "comfortable"

        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode_enabled")
        private val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        private val KEY_DATA_SAVER = booleanPreferencesKey("data_saver_enabled")
        private val KEY_LANGUAGE = stringPreferencesKey("language_code")
        private val KEY_TEXT_SIZE = stringPreferencesKey("text_size")
        private val KEY_DISPLAY_NAME = stringPreferencesKey("profile_display_name")
        private val KEY_EMAIL = stringPreferencesKey("profile_email")
        private val KEY_AVATAR_URL = stringPreferencesKey("profile_avatar_url")
        private val KEY_OCCUPATION = stringPreferencesKey("profile_occupation")
        private val KEY_LOCATION = stringPreferencesKey("profile_location")
        private val KEY_BIRTHDAY = stringPreferencesKey("profile_birthday")
        private val KEY_BIO = stringPreferencesKey("profile_bio")
        private val KEY_INTERESTS = stringPreferencesKey("profile_interests")
        private val KEY_IS_SIGNED_IN = booleanPreferencesKey("is_signed_in")
        private val KEY_PRIMARY_PROVIDER = stringPreferencesKey("primary_provider")
        private val KEY_LINKED_PROVIDERS = stringPreferencesKey("linked_providers")
        private val KEY_MASKED_PHONE = stringPreferencesKey("masked_phone")
        private val KEY_TRACK_READING_HISTORY = booleanPreferencesKey("track_reading_history")
        private val KEY_PERSONALIZATION = booleanPreferencesKey("personalization_enabled")

        private val KEY_REGION_COUNTRY = stringPreferencesKey("region_country")
        private val KEY_DEFAULT_START_TAB = stringPreferencesKey("default_start_tab")
        private val KEY_ARTICLE_STYLE = stringPreferencesKey("article_style")
        private val KEY_BREAKING_NEWS = booleanPreferencesKey("breaking_news_enabled")
        private val KEY_DAILY_DIGEST = booleanPreferencesKey("daily_digest_enabled")
        private val KEY_SYNC_HISTORY = booleanPreferencesKey("sync_history_enabled")
        private val KEY_AUTH_TYPE = stringPreferencesKey("current_auth_type")

        private fun decodeProviders(raw: String?): Set<String> {
            if (raw.isNullOrBlank()) return emptySet()
            return raw.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
        }

        private fun encodeProviders(values: Set<String>): String {
            return values.joinToString(",")
        }

        private fun maskPhone(phone: String?): String {
            val digits = phone.orEmpty().filter { it.isDigit() }
            if (digits.length < 4) return "***"
            return "***${digits.takeLast(4)}"
        }
    }
}