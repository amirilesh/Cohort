package com.cohort.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cohort.data.api.ApiClient
import com.cohort.data.model.RecentStudyCard
import com.cohort.ui.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SavedViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<RecentStudyCard>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<RecentStudyCard>>> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val cards = ApiClient.api.getSavedStudyCards()
                _uiState.value = UiState.Success(cards)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load saved cards")
            }
        }
    }
}
