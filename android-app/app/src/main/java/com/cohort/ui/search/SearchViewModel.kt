package com.cohort.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cohort.data.api.ApiClient
import com.cohort.data.model.PaperPreview
import com.cohort.data.model.SearchResult
import com.cohort.ui.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<SearchResult>>(UiState.Idle)
    val uiState: StateFlow<UiState<SearchResult>> = _uiState.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private var currentQuery = ""
    private var currentPage = 0
    private var totalCount = 0
    private val accumulated = mutableListOf<PaperPreview>()

    // Single boolean guards against concurrent requests (both initial and paginated).
    private var loading = false

    private val hasMore: Boolean
        get() = accumulated.size < totalCount

    fun search(query: String) {
        if (query.isBlank()) return
        currentQuery = query.trim()
        currentPage = 0
        totalCount = 0
        accumulated.clear()
        loading = false
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            fetchPage()
        }
    }

    fun loadNextPage() {
        if (loading || !hasMore) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            fetchPage()
            _isLoadingMore.value = false
        }
    }

    private suspend fun fetchPage() {
        loading = true
        val nextPage = currentPage + 1
        try {
            val result = ApiClient.api.search(currentQuery, page = nextPage, perPage = 10)
            currentPage = nextPage
            totalCount = result.totalCount
            accumulated.addAll(result.results)
            _uiState.value = UiState.Success(
                result.copy(results = accumulated.toList())
            )
        } catch (e: Exception) {
            if (currentPage == 0) {
                _uiState.value = UiState.Error(e.message ?: "Search failed")
            }
            // silent failure on pagination — existing results stay visible
        } finally {
            loading = false
        }
    }
}
