package com.kickstra.domain.usecase

import com.kickstra.data.local.ThemePreferences
import com.kickstra.domain.model.ThemePreference
import javax.inject.Inject

class SaveThemePreferenceUseCase @Inject constructor(
    private val themePreferences: ThemePreferences
) {
    suspend operator fun invoke(themePreference: ThemePreference) {
        themePreferences.saveThemePreference(themePreference.value)
    }
}
