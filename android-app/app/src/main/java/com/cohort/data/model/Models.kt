package com.cohort.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchResult(
    val query: String,
    val page: Int,
    val perPage: Int,
    val totalCount: Int,
    val results: List<PaperPreview>,
)

@Serializable
data class PaperPreview(
    val title: String,
    val year: Int? = null,
    val doi: String? = null,
    @SerialName("abstractText") val abstractText: String? = null,
    val oaUrl: String? = null,
    val authors: List<String> = emptyList(),
    val citedByCount: Int? = null,
)

@Serializable
data class StudyCardResponse(
    val url: String,
    val success: Boolean,
    val source: String = "",
    val sourceType: String = "FULL_TEXT",
    val tldr: String = "",
    val studyDesign: String = "",
    val keyFindings: List<String> = emptyList(),
    val limitations: String = "",
    val isSaved: Boolean = false,
    val reason: String? = null,
    val title: String? = null,
    val year: Int? = null,
    val doi: String? = null,
)

@Serializable
data class RecentStudyCard(
    val createdAt: String,
    val generationSource: String,
    val sourceUrl: String,
    val tldr: String,
    val studyDesign: String,
    val isSaved: Boolean = false,
    val doi: String? = null,
    val title: String? = null,
)