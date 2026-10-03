package com.kicklite.domain.usecase

import com.kicklite.data.local.LanguagePreferences
import javax.inject.Inject

class SaveSelectedLanguageUseCase @Inject constructor(
    private val languagePreferences: LanguagePreferences
) {
    suspend operator fun invoke(language: String?) {
        languagePreferences.saveSelectedLanguage(language)
    }
}
