package com.cohort

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object OpenAlexService {

    private val client = HttpClient(CIO)
    private val json = Json {
        ignoreUnknownKeys = true
    }

    suspend fun searchWorks(query: String): List<PaperPreview> {
        val response = client.get("https://api.openalex.org/works") {
            parameter("search", query)
        }

        return parsePapers(response.bodyAsText())
    }

    fun parsePapers(rawJson: String): List<PaperPreview> {
        val openAlexResponse = json.decodeFromString<OpenAlexResponse>(rawJson)

        return openAlexResponse.results.map { work ->
            PaperPreview(
                title = work.title,
                year = work.publicationYear,
                doi = work.doi
            )
        }
    }
}

@Serializable
data class PaperPreview(
    val title: String,
    val year: Int?,
    val doi: String?
)

@Serializable
private data class OpenAlexResponse(
    val results: List<OpenAlexWork> = emptyList()
)

@Serializable
private data class OpenAlexWork(
    @SerialName("display_name") val title: String,
    @SerialName("publication_year") val publicationYear: Int? = null,
    val doi: String? = null
)