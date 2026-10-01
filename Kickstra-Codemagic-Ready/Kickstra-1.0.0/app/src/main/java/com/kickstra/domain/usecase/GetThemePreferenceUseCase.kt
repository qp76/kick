package com.kickstra.domain.usecase

import com.kickstra.data.local.ThemePreferences
import com.kickstra.domain.model.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetThemePreferenceUseCase @Inject constructor(
    private val themePreferences: ThemePreferences
) {
    operator fun invoke(): Flow<ThemePreference> {
        return themePreferences.getThemePreference().map { value ->
            ThemePreference.fromValue(value)
        }
    }
}
