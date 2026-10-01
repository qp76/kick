package com.kickstra.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kickstra.domain.common.Resource
import com.kickstra.domain.usecase.GetPopularStreamsUseCase
import com.kickstra.domain.usecase.GetSelectedLanguageUseCase
import com.kickstra.domain.usecase.SaveSelectedLanguageUseCase
import com.kickstra.common.DebugLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getPopularStreamsUseCase: GetPopularStreamsUseCase,
    private val getSelectedLanguageUseCase: GetSelectedLanguageUseCase,
    private val saveSelectedLanguageUseCase: SaveSelectedLanguageUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private var isInitialized = false

    init {
        // Load saved language preference first, then load streams
        viewModelScope.launch {
            getSelectedLanguageUseCase().collect { savedLanguage ->
                if (!isInitialized) {
                    DebugLogger.logPagination("Loaded saved language: $savedLanguage")
                    _state.value = _state.value.copy(selectedLanguage = savedLanguage)
                    isInitialized = true
                    // Load streams only if we don't have any data yet
                    if (_state.value.streams.isEmpty() && !_state.value.isLoading) {
                        loadStreams()
                    }
                }
            }
        }
    }

    fun loadStreams() {
        val currentLanguage = _state.value.selectedLanguage
        viewModelScope.launch {
            DebugLogger.logPagination("Loading initial streams with language: $currentLanguage")
            getPopularStreamsUseCase(language = currentLanguage).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        DebugLogger.logPagination("Initial streams loading...")
                        _state.value = _state.value.copy(
                            isLoading = true,
                            error = null
                        )
                    }
                    is Resource.Success -> {
                        resource.data?.let { response ->
                            DebugLogger.logPagination("Loaded ${response.livestreams.size} initial streams")
                            DebugLogger.logPagination("Initial cursor: ${response.pagination.nextCursor}")
                            
                            _state.value = _state.value.copy(
                                isLoading = false,
                                streams = response.livestreams,
                                nextCursor = response.pagination.nextCursor,
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        DebugLogger.logPagination("Error loading initial streams: ${resource.message}")
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = resource.message
                        )
                    }
                }
            }
        }
    }

    fun loadMoreStreams() {
        val currentState = _state.value
        if (currentState.isLoadingMore || currentState.nextCursor == null) return

        DebugLogger.logPagination("Loading more streams with cursor: ${currentState.nextCursor}, language: ${currentState.selectedLanguage}")
        
        viewModelScope.launch {
            getPopularStreamsUseCase(
                cursor = currentState.nextCursor,
                language = currentState.selectedLanguage
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        DebugLogger.logPagination("Loading more streams...")
                        _state.value = _state.value.copy(isLoadingMore = true)
                    }
                    is Resource.Success -> {
                        resource.data?.let { response ->
                            DebugLogger.logPagination("Loaded ${response.livestreams.size} more streams")
                            DebugLogger.logPagination("New cursor: ${response.pagination.nextCursor}")
                            DebugLogger.logPagination("Total streams before: ${currentState.streams.size}")
                            
                            val newTotalStreams = currentState.streams + response.livestreams
                            DebugLogger.logPagination("Total streams after: ${newTotalStreams.size}")
                            
                            _state.value = _state.value.copy(
                                isLoadingMore = false,
                                streams = newTotalStreams,
                                nextCursor = response.pagination.nextCursor
                            )
                        }
                    }
                    is Resource.Error -> {
                        DebugLogger.logPagination("Error loading more streams: ${resource.message}")
                        _state.value = _state.value.copy(
                            isLoadingMore = false,
                            error = resource.message
                        )
                    }
                }
            }
        }
    }

    fun retry() {
        val currentLanguage = _state.value.selectedLanguage
        _state.value = HomeState(selectedLanguage = currentLanguage)
        loadStreams()
    }

    fun refresh() {
        val currentLanguage = _state.value.selectedLanguage
        _state.value = _state.value.copy(
            isRefreshing = true,
            error = null
        )
        
        viewModelScope.launch {
            DebugLogger.logPagination("Refreshing streams with language: $currentLanguage")
            getPopularStreamsUseCase(language = currentLanguage).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        // Keep isRefreshing true during loading
                    }
                    is Resource.Success -> {
                        resource.data?.let { response ->
                            DebugLogger.logPagination("Refreshed ${response.livestreams.size} streams")
                            DebugLogger.logPagination("Refresh cursor: ${response.pagination.nextCursor}")
                            
                            _state.value = _state.value.copy(
                                isRefreshing = false,
                                streams = response.livestreams,
                                nextCursor = response.pagination.nextCursor,
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        DebugLogger.logPagination("Error refreshing streams: ${resource.message}")
                        _state.value = _state.value.copy(
                            isRefreshing = false,
                            error = resource.message
                        )
                    }
                }
            }
        }
    }

    fun toggleLanguageFilter() {
        _state.value = _state.value.copy(
            showLanguageFilter = !_state.value.showLanguageFilter
        )
    }

    fun selectLanguage(language: String?) {
        DebugLogger.logPagination("Language changed to: $language")
        viewModelScope.launch {
            // Save language preference
            saveSelectedLanguageUseCase(language)
            
            _state.value = _state.value.copy(
                selectedLanguage = language,
                showLanguageFilter = false,
                streams = emptyList(), // Clear current streams
                nextCursor = null,
                error = null
            )
            // Reload with new language
            loadStreams()
        }
    }

    fun clearLanguageFilter() {
        selectLanguage(null)
    }
}
