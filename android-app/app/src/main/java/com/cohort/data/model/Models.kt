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
)

@Serializable
data class StudyCardResponse(
    val url: String,
    val success: Boolean,
    val source: String = "",
    val tldr: String = "",
    val studyDesign: String = "",
    val keyFindings: List<String> = emptyList(),
    val limitations: String = "",
    val reason: String? = null,
)

@Serializable
data class RecentStudyCard(
    val createdAt: String,
    val generationSource: String,
    val sourceUrl: String,
    val tldr: String,
    val studyDesign: String,
    val doi: String? = null,
    val title: String? = null,
)
