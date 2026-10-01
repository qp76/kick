package com.kickstra.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kickstra.domain.model.ThemePreference
import com.kickstra.domain.usecase.GetThemePreferenceUseCase
import com.kickstra.domain.usecase.SaveThemePreferenceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ThemeViewModel @Inject constructor(
    getThemePreferenceUseCase: GetThemePreferenceUseCase,
    private val saveThemePreferenceUseCase: SaveThemePreferenceUseCase
) : ViewModel() {

    private val _themePreference = MutableStateFlow(ThemePreference.AUTO)
    val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()

    init {
        viewModelScope.launch {
            getThemePreferenceUseCase().collect { preference ->
                _themePreference.value = preference
            }
        }
    }

    fun updateThemePreference(themePreference: ThemePreference) {
        viewModelScope.launch {
            _themePreference.value = themePreference
            saveThemePreferenceUseCase(themePreference)
        }
    }
}
