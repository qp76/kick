package com.kicklite.presentation.home

import com.kicklite.domain.model.LiveStream

data class HomeState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val streams: List<LiveStream> = emptyList(),
    val error: String? = null,
    val nextCursor: String? = null,
    val isLoadingMore: Boolean = false,
    val selectedLanguage: String? = null,
    val showLanguageFilter: Boolean = false
)
