package com.cohort.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cohort.data.model.RecentStudyCard
import com.cohort.ui.UiState
import android.content.Intent
import android.net.Uri

// ── Source type accent colors (mirrors StudyCardScreen palette) ──────────────
private val Purple    = Color(0xFFA78BFA)
private val PurpleDim = Color(0xFF1E1635)
private val Blue      = Color(0xFF60A5FA)
private val BlueDim   = Color(0xFF131C30)
private val Orange    = Color(0xFFFBBF24)
private val OrangeDim = Color(0xFF1F1A0E)

private val CardBg = Color(0xFF1A1E2E)

private data class SourceStyle(
    val accent: Color,
    val dimBg: Color,
    val label: String,
    val generationSource: String,
)

private fun sourceStyle(generationSource: String): SourceStyle = when (generationSource.lowercase()) {
    "llm"          -> SourceStyle(Purple, PurpleDim, "Full text",     generationSource)
    "llm_abstract" -> SourceStyle(Blue,   BlueDim,   "Abstract only", generationSource)
    "fallback"     -> SourceStyle(Orange, OrangeDim, "Metadata only", generationSource)
    else           -> SourceStyle(Purple, PurpleDim, "Full text",     generationSource)
}

// ────────────────────────────────────────────────────────────────────────────
// History Screen
// ────────────────────────────────────────────────────────────────────────────

@Composable
fun HistoryScreen(
    onCardClick: (RecentStudyCard) -> Unit = {},
    viewModel: HistoryViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    StudyCardCollectionScreen(
        title = "History",
        subtitle = "Your recently generated study cards",
        emptyTitle = "No cards yet",
        emptySubtitle = "Generate study cards from the Search tab",
        emptyIcon = Icons.Filled.History,
        uiState = uiState,
        onRetry = viewModel::load,
        onCardClick = onCardClick,
        errorTitle = "Could not load history",
    )
}

// ────────────────────────────────────────────────────────────────────────────
// Shared collection screen (used by History + Saved)
// ────────────────────────────────────────────────────────────────────────────

@Composable
internal fun StudyCardCollectionScreen(
    title: String,
    subtitle: String,
    emptyTitle: String,
    emptySubtitle: String,
    emptyIcon: androidx.compose.ui.graphics.vector.ImageVector,
    uiState: UiState<List<RecentStudyCard>>,
    onRetry: () -> Unit,
    onCardClick: (RecentStudyCard) -> Unit,
    errorTitle: String,
) {
    Column(modifier = Modifier.fillMaxSize()) {

        // ── Header ──────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ── Body ─────────────────────────────────────────────────────────────
        when (val state = uiState) {
            is UiState.Idle, is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = Purple,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = errorTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        FilledTonalButton(
                            onClick = onRetry,
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("Retry", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            is UiState.Success -> {
                val cards = state.data
                if (cards.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(48.dp)
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(14.dp),
                                    ),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = emptyIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = emptyTitle,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = emptySubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(cards) { card ->
                            StudyCardListItem(
                                card = card,
                                onClick = { onCardClick(card) },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Card item
// ────────────────────────────────────────────────────────────────────────────

@Composable
internal fun StudyCardListItem(
    card: RecentStudyCard,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val style = sourceStyle(card.generationSource)

    // ── Item 1: type-appropriate source badge icon ────────────────────────
    val sourceBadgeIcon = when (card.generationSource.lowercase()) {
        "llm", "llm_abstract" -> Icons.Filled.AutoAwesome
        else                  -> Icons.Filled.TextSnippet
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            // ── Item 5: reduced border opacity 0.18f → 0.12f ─────────────
            .border(
                width = 1.dp,
                color = style.accent.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp),
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {

            // ── Item 6: accent bar width 4dp → 3dp ───────────────────────
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(style.accent),
            )

            // ── Card body ────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {

                // ── Top row: source badge + date ─────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Source badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = style.dimBg,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            // ── Item 1: type-appropriate icon ─────────────
                            Icon(
                                imageVector = sourceBadgeIcon,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = style.accent,
                            )
                            // ── Item 2: sentence-case label ───────────────
                            Text(
                                text = style.label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = style.accent,
                                letterSpacing = 0.5.sp,
                            )
                        }
                    }

                    // Date
                    Text(
                        text = formatDate(card.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ── Title ────────────────────────────────────────────────
                Text(
                    text = card.title ?: card.sourceUrl,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 24.sp,
                )

                // ── TL;DR preview ────────────────────────────────────────
                if (card.tldr.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = card.tldr,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp,
                    )
                }

                Spacer(Modifier.height(14.dp))

                // ── Bottom row: saved badge + open paper ─────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Saved badge
                    if (card.isSaved) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bookmark,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Purple,
                            )
                            Text(
                                text = "Saved",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = Purple,
                            )
                        }
                    } else {
                        // Spacer to push "Open paper" to the right even when not saved
                        Spacer(Modifier.width(1.dp))
                    }

                    // ── Item 3: "Open paper" button — neutral surfaceVariant ──
                    if (card.sourceUrl.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(card.sourceUrl))
                                    )
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.OpenInBrowser,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "Open paper",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Date formatting — "2026-05-01T..." → "May 1, 2026"
// ────────────────────────────────────────────────────────────────────────────

internal fun formatDate(isoString: String): String {
    return try {
        val date = isoString.take(10)          // "2026-05-01"
        val parts = date.split("-")
        if (parts.size != 3) return date
        val year = parts[0]
        val month = when (parts[1]) {
            "01" -> "Jan"; "02" -> "Feb"; "03" -> "Mar"; "04" -> "Apr"
            "05" -> "May"; "06" -> "Jun"; "07" -> "Jul"; "08" -> "Aug"
            "09" -> "Sep"; "10" -> "Oct"; "11" -> "Nov"; "12" -> "Dec"
            else -> parts[1]
        }
        val day = parts[2].trimStart('0')
        "$month $day, $year"
    } catch (_: Exception) {
        if (isoString.length >= 10) isoString.substring(0, 10) else isoString
    }
}
