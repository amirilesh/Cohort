package com.cohort.ui.history

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SavedScreen(
    onCardClick: (com.cohort.data.model.RecentStudyCard) -> Unit = {},
    viewModel: SavedViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    StudyCardCollectionScreen(
        title = "Saved",
        subtitle = "Study cards you saved",
        emptyTitle = "No saved cards yet",
        emptySubtitle = "Save study cards to find them here",
        emptyIcon = Icons.Filled.Bookmark,
        uiState = uiState,
        onRetry = viewModel::load,
        onCardClick = onCardClick,
        errorTitle = "Could not load saved cards",
    )
}
