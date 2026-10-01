package com.kickstra.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.languageDataStore: DataStore<Preferences> by preferencesDataStore(name = "language_preferences")

@Singleton
class LanguagePreferences @Inject constructor(
    private val context: Context
) {
    private val SELECTED_LANGUAGE_KEY = stringPreferencesKey("selected_language")

    fun getSelectedLanguage(): Flow<String?> {
        return context.languageDataStore.data.map { preferences ->
            preferences[SELECTED_LANGUAGE_KEY]
        }
    }

    suspend fun saveSelectedLanguage(language: String?) {
        context.languageDataStore.edit { preferences ->
            if (language != null) {
                preferences[SELECTED_LANGUAGE_KEY] = language
            } else {
                preferences.remove(SELECTED_LANGUAGE_KEY)
            }
        }
    }

    suspend fun clearSelectedLanguage() {
        context.languageDataStore.edit { preferences ->
            preferences.remove(SELECTED_LANGUAGE_KEY)
        }
    }
}
