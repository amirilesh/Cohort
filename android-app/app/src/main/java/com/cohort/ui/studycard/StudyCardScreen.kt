package com.cohort.ui.studycard

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cohort.data.model.RecentStudyCard
import com.cohort.data.model.StudyCardResponse
import com.cohort.ui.UiState

// ── Section accent colors ───────────────────────────────────────────────
private val Purple    = Color(0xFFA78BFA)
private val PurpleDim = Color(0xFF1E1635)
private val Blue      = Color(0xFF60A5FA)
private val BlueDim   = Color(0xFF131C30)
private val Green     = Color(0xFF4ADE80)
private val GreenDim  = Color(0xFF122119)
private val Orange    = Color(0xFFFBBF24)
private val OrangeDim = Color(0xFF1F1A0E)
private val Cyan      = Color(0xFF67E8F9)
private val CyanDim   = Color(0xFF0E1F25)

private val CardBg = Color(0xFF1A1E2E)

// ────────────────────────────────────────────────────────────────────────
// Study Card Screen (from DOI)
// ────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyCardScreen(
    doi: String,
    onBack: () -> Unit,
    viewModel: StudyCardViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(doi) { viewModel.load(doi) }

    Scaffold(
        topBar = { StudyCardTopBar(onBack = onBack) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val state = uiState) {
                is UiState.Idle, is UiState.Loading -> LoadingContent()
                is UiState.Success -> StudyCardContent(card = state.data)
                is UiState.Error -> ErrorContent(message = state.message)
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// Study Card Detail Screen (from History)
// ────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyCardDetailScreen(
    card: RecentStudyCard,
    onBack: () -> Unit,
    viewModel: StudyCardViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(card.doi, card.sourceUrl) {
        if (!card.doi.isNullOrBlank()) viewModel.load(card.doi)
        else if (card.sourceUrl.isNotBlank()) viewModel.loadByUrl(card.sourceUrl)
    }

    val showFullCard = uiState is UiState.Success

    Scaffold(
        topBar = { StudyCardTopBar(onBack = onBack) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                showFullCard -> {
                    StudyCardContent(card = (uiState as UiState.Success).data)
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

// ────────────────────────────────────────────────────────────────────────
// Top bar (shared)
// ────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudyCardTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "Study Card",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = { /* bookmark – no-op for now */ }) {
                Icon(
                    Icons.Filled.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

// ────────────────────────────────────────────────────────────────────────
// Full study card content
// ────────────────────────────────────────────────────────────────────────

@Composable
private fun StudyCardContent(card: StudyCardResponse) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ── Summarized Study ────────────────────────────────────────────
        item {
            SectionCard(
                accent = Purple,
                dimBg = PurpleDim,
                icon = Icons.Filled.AutoAwesome,
                title = "Summarized Study",
                chip = "AI Summary",
                subtitle = "AI-generated summary of key findings\nand insights from this research.",
                showChevron = true,
            ) {
                Text(
                    text = card.tldr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp,
                )
            }
        }

        // ── Study Design ────────────────────────────────────────────────
        item {
            SectionCard(
                accent = Blue,
                dimBg = BlueDim,
                icon = Icons.Filled.Science,
                title = "Study Design",
            ) {
                Text(
                    text = card.studyDesign,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp,
                )
            }
        }

        // ── Key Findings ────────────────────────────────────────────────
        if (card.keyFindings.isNotEmpty()) {
            item {
                SectionCard(
                    accent = Green,
                    dimBg = GreenDim,
                    icon = Icons.Outlined.FormatListNumbered,
                    title = "Key Findings",
                ) {
                    Column {
                        card.keyFindings.forEachIndexed { index, finding ->
                            val isLast = index == card.keyFindings.lastIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                            ) {
                                // Number badge + vertical connecting line
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(32.dp)
                                        .fillMaxHeight(),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .border(1.5.dp, Green, CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Green,
                                        )
                                    }
                                    if (!isLast) {
                                        Box(
                                            modifier = Modifier
                                                .width(1.5.dp)
                                                .weight(1f)
                                                .background(Green.copy(alpha = 0.25f)),
                                        )
                                    }
                                }
                                Text(
                                    text = finding,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 22.sp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(
                                            start = 12.dp,
                                            top = 2.dp,
                                            bottom = if (isLast) 0.dp else 16.dp,
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Limitations ─────────────────────────────────────────────────
        item {
            SectionCard(
                accent = Orange,
                dimBg = OrangeDim,
                icon = Icons.Filled.Warning,
                title = "Limitations",
            ) {
                Text(
                    text = card.limitations,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp,
                )
            }
        }

        // ── Open Full Paper ─────────────────────────────────────────────
        if (card.url.isNotBlank()) {
            item { OpenPaperCard(url = card.url) }
        }

        // ── Source badge ────────────────────────────────────────────────
        item { SourceBadge(source = card.source) }

        // Bottom spacing
        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ────────────────────────────────────────────────────────────────────────
// History detail content
// ────────────────────────────────────────────────────────────────────────

@Composable
private fun HistoryDetailContent(card: RecentStudyCard) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Title
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = card.title ?: card.sourceUrl,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(6.dp))
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
                SectionCard(
                    accent = Purple,
                    dimBg = PurpleDim,
                    icon = Icons.Filled.AutoAwesome,
                    title = "Summarized Study",
                    chip = "AI Summary",
                    subtitle = "AI-generated summary from this research.",
                ) {
                    Text(
                        text = card.tldr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp,
                    )
                }
            }
        }

        // Study Design
        if (card.studyDesign.isNotBlank()) {
            item {
                SectionCard(
                    accent = Blue,
                    dimBg = BlueDim,
                    icon = Icons.Filled.Science,
                    title = "Study Design",
                ) {
                    Text(
                        text = card.studyDesign,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp,
                    )
                }
            }
        }

        // Source badge
        if (card.generationSource.isNotBlank()) {
            item { SourceBadge(source = card.generationSource) }
        }

        // Open paper
        if (card.sourceUrl.isNotBlank()) {
            item { OpenPaperCard(url = card.sourceUrl) }
        }

        // Info notice
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
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Showing summary from history",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// SectionCard — color-coded card with icon, title, optional chip & chevron
// ────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionCard(
    accent: Color,
    dimBg: Color,
    icon: ImageVector,
    title: String,
    chip: String? = null,
    subtitle: String? = null,
    showChevron: Boolean = false,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp),
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // ── Header row ──────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Colored circle icon
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(dimBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = accent,
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Title + chip + subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = accent,
                            fontSize = 15.sp,
                        )
                        // "AI Summary" chip
                        if (chip != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = accent.copy(alpha = 0.15f),
                            ) {
                                Text(
                                    text = chip,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = accent,
                                    modifier = Modifier.padding(
                                        horizontal = 6.dp,
                                        vertical = 2.dp,
                                    ),
                                )
                            }
                        }
                    }
                    if (subtitle != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp,
                        )
                    }
                }

                // Chevron (for expandable sections)
                if (showChevron) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = accent,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Body content ────────────────────────────────────────────
            content()
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// Open Full Paper card
// ────────────────────────────────────────────────────────────────────────

@Composable
private fun OpenPaperCard(url: String) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Cyan.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(CyanDim),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Cyan,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Open Full Paper",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Cyan,
                    fontSize = 15.sp,
                )
                Text(
                    text = "View original research",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = Cyan,
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// Source badge
// ────────────────────────────────────────────────────────────────────────

@Composable
private fun SourceBadge(source: String) {
    val isAi = source.lowercase() in listOf("llm", "llm_abstract")
    val label = when (source.lowercase()) {
        "llm", "llm_abstract" -> "Generated by Cohort AI"
        "fallback"            -> "Text extraction"
        else                  -> source
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isAi) Purple.copy(alpha = 0.1f)
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
                    tint = if (isAi) Purple else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isAi) Purple else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// Loading content
// ────────────────────────────────────────────────────────────────────────

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
            color = Purple,
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
            shape = RoundedCornerShape(12.dp),
            color = CardBg,
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Purple,
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

// ────────────────────────────────────────────────────────────────────────
// Error content
// ────────────────────────────────────────────────────────────────────────

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
