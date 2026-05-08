package com.cohort.ui.studycard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cohort.data.api.ApiClient
import com.cohort.data.model.StudyCardJobResponse
import com.cohort.data.model.StudyCardResponse
import com.cohort.ui.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private companion object {
        const val POLL_DELAY_MS = 1_500L
        const val MAX_POLL_ATTEMPTS = 80
    }

    private var loadJob: Job? = null

    private val _uiState = MutableStateFlow<UiState<StudyCardResponse>>(UiState.Idle)
    val uiState: StateFlow<UiState<StudyCardResponse>> = _uiState.asStateFlow()

    private val _saveUiState = MutableStateFlow(SaveUiState())
    val saveUiState: StateFlow<SaveUiState> = _saveUiState.asStateFlow()

    fun load(doi: String) {
        loadStudyCard(
            createJob = { ApiClient.api.createStudyCardByDoi(doi) },
            allowMetadataOnly = true,
            defaultError = "Failed to generate study card",
        )
    }

    fun loadByUrl(url: String) {
        loadStudyCard(
            createJob = { ApiClient.api.createStudyCardByUrl(url) },
            allowMetadataOnly = false,
            defaultError = "Failed to load study card",
        )
    }

    private fun loadStudyCard(
        createJob: suspend () -> StudyCardJobResponse,
        allowMetadataOnly: Boolean,
        defaultError: String,
    ) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = UiState.Loading
            _saveUiState.value = _saveUiState.value.copy(isSaving = false, errorMessage = null)
            _uiState.value = try {
                val job = createJob()
                val card = pollForStudyCard(job.jobId)
                card.toUiState(
                    allowMetadataOnly = allowMetadataOnly,
                    defaultError = defaultError,
                )
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

    private suspend fun pollForStudyCard(jobId: String): StudyCardResponse {
        repeat(MAX_POLL_ATTEMPTS) { attempt ->
            val job = ApiClient.api.getStudyCardJob(jobId)
            when (job.status.lowercase()) {
                "processing" -> {
                    if (attempt == MAX_POLL_ATTEMPTS - 1) {
                        throw IllegalStateException("Study card generation timed out")
                    }
                    delay(POLL_DELAY_MS)
                }
                "completed" -> {
                    return job.result ?: throw IllegalStateException("Study card result missing")
                }
                "failed" -> {
                    throw IllegalStateException(job.error ?: "Failed to generate study card")
                }
                else -> {
                    throw IllegalStateException("Unknown study card job status")
                }
            }
        }

        throw IllegalStateException("Study card generation timed out")
    }

    private fun StudyCardResponse.toUiState(
        allowMetadataOnly: Boolean,
        defaultError: String,
    ): UiState<StudyCardResponse> {
        return when {
            success -> {
                syncSavedState(isSaved)
                UiState.Success(this)
            }
            allowMetadataOnly && sourceType == "METADATA_ONLY" -> UiState.Success(this)
            else -> UiState.Error(reason ?: defaultError)
        }
    }

    private fun syncSavedState(isSaved: Boolean) {
        _saveUiState.value = SaveUiState(isSaved = isSaved)
        val currentCard = (_uiState.value as? UiState.Success)?.data ?: return
        _uiState.value = UiState.Success(currentCard.copy(isSaved = isSaved))
    }

    override fun onCleared() {
        loadJob?.cancel()
        super.onCleared()
    }
}
