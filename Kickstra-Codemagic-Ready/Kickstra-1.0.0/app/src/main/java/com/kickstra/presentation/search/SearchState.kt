package com.kickstra.presentation.search

import com.kickstra.domain.model.ChannelSearchResult
import com.kickstra.domain.model.CategorySearchResult
import com.kickstra.domain.model.TagSearchResult

data class SearchState(
    val query: String = "",
    val channels: List<ChannelSearchResult> = emptyList(),
    val categories: List<CategorySearchResult> = emptyList(),
    val tags: List<TagSearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasSearched: Boolean = false
)
