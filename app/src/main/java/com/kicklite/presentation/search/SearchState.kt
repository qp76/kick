package com.kicklite.presentation.search

import com.kicklite.domain.model.ChannelSearchResult
import com.kicklite.domain.model.CategorySearchResult
import com.kicklite.domain.model.TagSearchResult

data class SearchState(
    val query: String = "",
    val channels: List<ChannelSearchResult> = emptyList(),
    val categories: List<CategorySearchResult> = emptyList(),
    val tags: List<TagSearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasSearched: Boolean = false
)
