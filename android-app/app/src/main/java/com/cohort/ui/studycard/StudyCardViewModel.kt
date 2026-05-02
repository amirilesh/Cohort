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

data class SaveUiState(
    val isSaved: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

class StudyCardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<StudyCardResponse>>(UiState.Idle)
    val uiState: StateFlow<UiState<StudyCardResponse>> = _uiState.asStateFlow()

    private val _saveUiState = MutableStateFlow(SaveUiState())
    val saveUiState: StateFlow<SaveUiState> = _saveUiState.asStateFlow()

    fun load(doi: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _saveUiState.value = _saveUiState.value.copy(isSaving = false, errorMessage = null)
            _uiState.value = try {
                val card = ApiClient.api.studyCardByDoi(doi)
                if (card.success) {
                    syncSavedState(card.isSaved)
                    UiState.Success(card)
                } else if (card.sourceType == "METADATA_ONLY") {
                    UiState.Success(card)
                } else {
                    UiState.Error(card.reason ?: "Failed to generate study card")
                }
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Request failed")
            }
        }
    }

    fun loadByUrl(url: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _saveUiState.value = _saveUiState.value.copy(isSaving = false, errorMessage = null)
            _uiState.value = try {
                val card = ApiClient.api.studyCardByUrl(url)
                if (card.success) {
                    syncSavedState(card.isSaved)
                    UiState.Success(card)
                } else {
                    UiState.Error(card.reason ?: "Failed to load study card")
                }
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Request failed")
            }
        }
    }

    fun setInitialSavedState(isSaved: Boolean) {
        syncSavedState(isSaved)
    }

    fun toggleSaved(url: String) {
        val currentState = _saveUiState.value
        if (url.isBlank() || currentState.isSaving) return
        if (currentState.isSaved) {
            removeSaved(url)
            return
        }
        save(url)
    }

    fun clearSaveError() {
        _saveUiState.value = _saveUiState.value.copy(errorMessage = null)
    }

    private fun save(url: String) {
        viewModelScope.launch {
            _saveUiState.value = _saveUiState.value.copy(isSaving = true, errorMessage = null)
            try {
                val response = ApiClient.api.saveStudyCard(url)
                if (response.isSuccessful) {
                    syncSavedState(true)
                    return@launch
                }
                _saveUiState.value = _saveUiState.value.copy(
                    isSaving = false,
                    errorMessage = "Failed to save card",
                )
            } catch (e: Exception) {
                _saveUiState.value = _saveUiState.value.copy(
                    isSaving = false,
                    errorMessage = e.message ?: "Failed to save card",
                )
            }
        }
    }

    private fun removeSaved(url: String) {
        viewModelScope.launch {
            _saveUiState.value = _saveUiState.value.copy(isSaving = true, errorMessage = null)
            try {
                val response = ApiClient.api.deleteSavedStudyCard(url)
                if (response.isSuccessful) {
                    syncSavedState(false)
                    return@launch
                }
                _saveUiState.value = _saveUiState.value.copy(
                    isSaving = false,
                    errorMessage = "Failed to remove saved card",
                )
            } catch (e: Exception) {
                _saveUiState.value = _saveUiState.value.copy(
                    isSaving = false,
                    errorMessage = e.message ?: "Failed to remove saved card",
                )
            }
        }
    }

    private fun syncSavedState(isSaved: Boolean) {
        _saveUiState.value = SaveUiState(isSaved = isSaved)
        val currentCard = (_uiState.value as? UiState.Success)?.data ?: return
        _uiState.value = UiState.Success(currentCard.copy(isSaved = isSaved))
    }
}
