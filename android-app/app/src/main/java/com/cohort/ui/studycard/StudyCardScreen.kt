package com.cohort.ui.studycard

import androidx.compose.foundation.border
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cohort.data.model.RecentStudyCard
import com.cohort.data.model.StudyCardResponse
import com.cohort.ui.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyCardScreen(
    doi: String,
    onBack: () -> Unit,
    viewModel: StudyCardViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(doi) {
        viewModel.load(doi)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Study Card",
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val state = uiState) {
                is UiState.Idle,
                is UiState.Loading -> LoadingContent()
                is UiState.Success -> StudyCardContent(card = state.data)
                is UiState.Error   -> ErrorContent(message = state.message)
            }
        }
    }
}

/**
 * Read-only detail screen for History items.
 * If the card has a DOI, attempts to load the full cached card from the API.
 * Otherwise displays the partial data from the History item directly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyCardDetailScreen(
    card: RecentStudyCard,
    onBack: () -> Unit,
    viewModel: StudyCardViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Try to load full card if DOI or URL is available
    LaunchedEffect(card.doi, card.sourceUrl) {
        if (!card.doi.isNullOrBlank()) {
            viewModel.load(card.doi)
        } else if (card.sourceUrl.isNotBlank()) {
            viewModel.loadByUrl(card.sourceUrl)
        }
    }

    val showFullCard = uiState is UiState.Success

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Study Card",
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                showFullCard -> {
                    val fullCard = (uiState as UiState.Success).data
                    StudyCardContent(card = fullCard)
                }
                uiState is UiState.Loading || uiState is UiState.Idle -> {
                    if (card.tldr.isNotBlank() || card.studyDesign.isNotBlank()) {
                        HistoryDetailContent(card = card)
                    } else {
                        LoadingContent()
                    }
                }
                uiState is UiState.Error -> {
                    HistoryDetailContent(card = card)
                }
            }
        }
    }
}

// ── Full study card content ─────────────────────────────────────────────

@Composable
private fun StudyCardContent(card: StudyCardResponse) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // TL;DR — hero card with subtle primary border
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(14.dp),
                    ),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "TL;DR",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = card.tldr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        // Study Design
        item {
            SectionCard(icon = Icons.Filled.Science, title = "Study Design") {
                Text(
                    text = card.studyDesign,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // Key Findings
        if (card.keyFindings.isNotEmpty()) {
            item {
                SectionCard(icon = Icons.Filled.FormatListNumbered, title = "Key Findings") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        card.keyFindings.forEachIndexed { index, finding ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    modifier = Modifier.size(22.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                                Text(
                                    text = finding,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Limitations
        item {
            SectionCard(icon = Icons.Filled.Warning, title = "Limitations") {
                Text(
                    text = card.limitations,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // Source badge
        item { SourceBadge(source = card.source) }

        // Open paper button
        if (card.url.isNotBlank()) {
            item { OpenPaperButton(url = card.url) }
        }
    }
}

// ── History detail content (partial data from RecentStudyCard) ───────────

@Composable
private fun HistoryDetailContent(card: RecentStudyCard) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Title card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(14.dp),
                    ),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = card.title ?: card.sourceUrl,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = formatHistoryDate(card.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // TL;DR
        if (card.tldr.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(14.dp),
                        ),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "TL;DR",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = card.tldr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }

        // Study Design
        if (card.studyDesign.isNotBlank()) {
            item {
                SectionCard(icon = Icons.Filled.Science, title = "Study Design") {
                    Text(
                        text = card.studyDesign,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // Source badge
        if (card.generationSource.isNotBlank()) {
            item { SourceBadge(source = card.generationSource) }
        }

        // Open paper button
        if (card.sourceUrl.isNotBlank()) {
            item { OpenPaperButton(url = card.sourceUrl) }
        }

        // Limited info notice
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Showing summary from history",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
    }
}

// ── Shared composables ──────────────────────────────────────────────────

@Composable
private fun OpenPaperButton(url: String) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        FilledTonalButton(
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                )
            },
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.OpenInBrowser,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Open paper",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(40.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Generating your study card…",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "This may take up to 60 seconds",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Cohort AI is reading the paper and creating a concise study card.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(14.dp),
            ),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline,
            )
            content()
        }
    }
}

@Composable
private fun SourceBadge(source: String) {
    val isAi = source.lowercase() in listOf("llm", "llm_abstract")
    val label = when (source.lowercase()) {
        "llm"          -> "Generated by Cohort AI"
        "llm_abstract" -> "Generated by Cohort AI"
        "fallback"     -> "Text extraction"
        else           -> source
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isAi) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    else MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = if (isAi) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isAi) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.size(56.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Error,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Could not generate study card",
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun formatHistoryDate(isoString: String): String =
    if (isoString.length >= 10) isoString.substring(0, 10) else isoString
