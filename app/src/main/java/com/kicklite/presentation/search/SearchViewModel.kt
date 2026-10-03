package com.kicklite.presentation.search

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.viewModelScope
import com.kicklite.domain.common.Resource
import com.kicklite.domain.usecase.SearchUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchUseCase: SearchUseCase,
    @ApplicationContext context: Context
) : ViewModel() {

    private val recentPrefs = context.getSharedPreferences("search_recent", Context.MODE_PRIVATE)

    private fun loadRecent(): List<String> =
        recentPrefs.getString("items", "")?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()

    private fun saveRecent(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        val list = (listOf(q) + loadRecent().filter { !it.equals(q, ignoreCase = true) }).take(8)
        recentPrefs.edit().putString("items", list.joinToString("\n")).apply()
        _state.value = _state.value.copy(recent = list)
    }

    fun clearRecent() {
        recentPrefs.edit().remove("items").apply()
        _state.value = _state.value.copy(recent = emptyList())
    }

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    
    init {
        _state.value = _state.value.copy(recent = loadRecent())
        // Setup debounced search
        viewModelScope.launch {
            _searchQuery
                .debounce(300) // Wait 300ms after user stops typing
                .distinctUntilChanged()
                .filter { it.isNotBlank() && it.length >= 3 } // Only search if query is at least 3 characters
                .collect { query ->
                    performSearch(query)
                }
        }
    }

    fun updateQuery(query: String) {
        _state.value = _state.value.copy(query = query)
        _searchQuery.value = query
        
        if (query.isBlank()) {
            clearResults()
        } else if (query.length < 3) {
            _state.value = _state.value.copy(
                channels = emptyList(),
                categories = emptyList(),
                tags = emptyList(),
                isLoading = false,
                error = null,
                hasSearched = false
            )
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            searchUseCase(query).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _state.value = _state.value.copy(
                            isLoading = true,
                            error = null
                        )
                    }
                    is Resource.Success -> {
                        saveRecent(query)
                        resource.data?.let { searchResponse ->
                            _state.value = _state.value.copy(
                                isLoading = false,
                                channels = searchResponse.channels,
                                categories = searchResponse.categories,
                                tags = searchResponse.tags,
                                error = null,
                                hasSearched = true
                            )
                        }
                    }
                    is Resource.Error -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = resource.message,
                            hasSearched = true
                        )
                    }
                }
            }
        }
    }

    private fun clearResults() {
        _state.value = _state.value.copy(
            channels = emptyList(),
            categories = emptyList(),
            tags = emptyList(),
            error = null,
            hasSearched = false
        )
    }

    fun retry() {
        val currentQuery = _state.value.query
        if (currentQuery.isNotBlank()) {
            performSearch(currentQuery)
        }
    }
}
