package com.data.settings

enum class AuthProvider(val code: String) {
    PHONE("phone"),
    GOOGLE("google"),
    FACEBOOK("facebook"),
    APPLE("apple"),
    EMAIL("email"),
    OTHER("other");

    companion object {
        fun fromCode(code: String?): AuthProvider? {
            if (code.isNullOrBlank()) return null
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
        }
    }
}

