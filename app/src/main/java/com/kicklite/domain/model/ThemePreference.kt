package com.kicklite.domain.model

enum class ThemePreference(val value: String) {
    AUTO("auto"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromValue(value: String?): ThemePreference {
            return when (value) {
                LIGHT.value -> LIGHT
                DARK.value -> DARK
                else -> AUTO
            }
        }
    }
}
