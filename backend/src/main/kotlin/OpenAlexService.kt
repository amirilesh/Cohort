package com.cohort

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

object OpenAlexService {

    private val client = HttpClient.newHttpClient()
    private val json = Json {
        ignoreUnknownKeys = true
    }

    suspend fun searchWorks(query: String): List<PaperPreview> {
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8)
        val request = HttpRequest.newBuilder()
            .uri(URI("https://api.openalex.org/works?search=$encodedQuery"))
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        return parsePapers(response.body())
    }

    fun parsePapers(rawJson: String): List<PaperPreview> {
        val openAlexResponse = json.decodeFromString<OpenAlexResponse>(rawJson)

        return openAlexResponse.results
            .map { work ->
                PaperPreview(
                    title = work.title,
                    year = work.publicationYear,
                    doi = work.doi,
                    abstractText = reconstructAbstract(work.abstractInvertedIndex),
                    isOpenAccess = work.openAccess?.isOpenAccess,
                    oaStatus = work.openAccess?.oaStatus,
                    oaUrl = work.openAccess?.oaUrl
                )
            }
            .filter { paper ->
                paper.isOpenAccess == true && paper.oaUrl != null
            }
    }

    private fun reconstructAbstract(abstractInvertedIndex: Map<String, List<Int>>?): String? {
        if (abstractInvertedIndex.isNullOrEmpty()) {
            return null
        }

        val orderedWords = abstractInvertedIndex
            .flatMap { (word, positions) ->
                positions.map { position -> position to word }
            }
            .sortedBy { (position, _) -> position }
            .map { (_, word) -> word }

        if (orderedWords.isEmpty()) {
            return null
        }

        return orderedWords.joinToString(" ")
    }
}

@Serializable
data class PaperPreview(
    val title: String,
    val year: Int?,
    val doi: String?,
    val abstractText: String?,
    val isOpenAccess: Boolean?,
    val oaStatus: String?,
    val oaUrl: String?
)

@Serializable
private data class OpenAlexResponse(
    val results: List<OpenAlexWork> = emptyList()
)

@Serializable
private data class OpenAlexWork(
    @SerialName("display_name") val title: String,
    @SerialName("publication_year") val publicationYear: Int? = null,
    val doi: String? = null,
    @SerialName("abstract_inverted_index") val abstractInvertedIndex: Map<String, List<Int>>? = null,
    @SerialName("open_access") val openAccess: OpenAccessInfo? = null
)

@Serializable
private data class OpenAccessInfo(
    @SerialName("is_oa") val isOpenAccess: Boolean? = null,
    @SerialName("oa_status") val oaStatus: String? = null,
    @SerialName("pdf_url") val oaUrl: String? = null
)
