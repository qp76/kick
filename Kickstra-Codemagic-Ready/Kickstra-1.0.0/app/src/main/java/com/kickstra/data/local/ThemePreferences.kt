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

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

@Singleton
class ThemePreferences @Inject constructor(
    private val context: Context
) {
    private val THEME_KEY = stringPreferencesKey("selected_theme")

    fun getThemePreference(): Flow<String?> {
        return context.themeDataStore.data.map { preferences ->
            preferences[THEME_KEY]
        }
    }

    suspend fun saveThemePreference(themeValue: String) {
        context.themeDataStore.edit { preferences ->
            preferences[THEME_KEY] = themeValue
        }
    }
}
