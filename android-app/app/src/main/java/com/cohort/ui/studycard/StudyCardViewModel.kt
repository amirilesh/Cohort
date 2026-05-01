package com.cohort.ui.studycard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cohort.data.api.ApiClient
import com.cohort.data.model.StudyCardResponse
import com.cohort.ui.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StudyCardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<StudyCardResponse>>(UiState.Idle)
    val uiState: StateFlow<UiState<StudyCardResponse>> = _uiState.asStateFlow()

    fun load(doi: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _uiState.value = try {
                val card = ApiClient.api.studyCardByDoi(doi)
                if (card.success) UiState.Success(card)
                else UiState.Error(card.reason ?: "Failed to generate study card")
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Request failed")
            }
        }
    }

    fun loadByUrl(url: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _uiState.value = try {
                val card = ApiClient.api.studyCardByUrl(url)
                if (card.success) UiState.Success(card)
                else UiState.Error(card.reason ?: "Failed to load study card")
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Request failed")
            }
        }
    }
}
