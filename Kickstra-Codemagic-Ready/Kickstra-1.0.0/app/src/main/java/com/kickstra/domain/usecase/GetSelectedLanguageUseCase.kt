package com.kickstra.domain.usecase

import com.kickstra.data.local.LanguagePreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSelectedLanguageUseCase @Inject constructor(
    private val languagePreferences: LanguagePreferences
) {
    operator fun invoke(): Flow<String?> {
        return languagePreferences.getSelectedLanguage()
    }
}
