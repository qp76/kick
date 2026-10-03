package com.kicklite.domain.usecase

import com.kicklite.data.local.ThemePreferences
import com.kicklite.domain.model.ThemePreference
import javax.inject.Inject

class SaveThemePreferenceUseCase @Inject constructor(
    private val themePreferences: ThemePreferences
) {
    suspend operator fun invoke(themePreference: ThemePreference) {
        themePreferences.saveThemePreference(themePreference.value)
    }
}
