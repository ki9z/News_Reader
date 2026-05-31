package com.data.settings

data class UserSettings(
    val darkModeEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val dataSaverEnabled: Boolean = false,
    val languageCode: String = "en",
    val textSize: String = "M",
    val displayName: String = "Guest User",
    val email: String = "guest@newsreader.app",
    val avatarUrl: String = "",
    val occupation: String = "",
    val location: String = "",
    val birthday: String = "",
    val bio: String = "",
    val interests: String = "",
    val isSignedIn: Boolean = false,
    val primaryAuthProvider: String = "",
    val linkedProviders: Set<String> = emptySet(),
    val maskedPhone: String = "",
    val trackReadingHistory: Boolean = true,
    val personalizationEnabled: Boolean = true,

    // Local app preferences
    val regionCountry: String = "us",
    val defaultStartTab: String = "home",
    val articleStyle: String = "comfortable",
    val breakingNewsEnabled: Boolean = true,
    val dailyDigestEnabled: Boolean = true,
    val syncHistoryEnabled: Boolean = false,
    
    // Auth type
    val currentAuthType: String = "user"
)