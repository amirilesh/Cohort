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

@Serializable
data class SearchResult(
    val query: String,
    val page: Int,
    val perPage: Int,
    val totalCount: Int,
    val results: List<PaperPreview>,
)

object OpenAlexService {

    private const val MAX_PER_PAGE = 25

    private val client = HttpClient.newHttpClient()
    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun searchWorks(query: String, page: Int = 1, perPage: Int = 10): SearchResult {
        val clampedPerPage = perPage.coerceAtMost(MAX_PER_PAGE)
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8)
        val request = HttpRequest.newBuilder()
            .uri(URI("https://api.openalex.org/works?search=$encodedQuery&filter=is_oa:true&page=$page&per_page=$clampedPerPage"))
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        val openAlexResponse = json.decodeFromString<OpenAlexResponse>(response.body())
        val papers = openAlexResponse.results
            .map { work ->
                PaperPreview(
                    title = work.title,
                    year = work.publicationYear,
                    doi = work.doi,
                    abstractText = reconstructAbstract(work.abstractInvertedIndex),
                    isOpenAccess = work.openAccess?.isOpenAccess,
                    oaStatus = work.openAccess?.oaStatus,
                    oaUrl = work.bestOaLocation?.pdfUrl,
                )
            }
            .filter { it.isOpenAccess == true && it.oaUrl != null }

        return SearchResult(
            query = query,
            page = page,
            perPage = clampedPerPage,
            totalCount = openAlexResponse.meta?.count ?: 0,
            results = papers,
        )
    }

    fun getOaUrlForDoi(doi: String): String? {
        return getWorkByDoi(doi)?.bestOaLocation?.pdfUrl
    }

    fun hasWorkForDoi(doi: String): Boolean {
        return getWorkByDoi(doi) != null
    }

    private fun reconstructAbstract(abstractInvertedIndex: Map<String, List<Int>>?): String? {
        if (abstractInvertedIndex.isNullOrEmpty()) return null

        val orderedWords = abstractInvertedIndex
            .flatMap { (word, positions) -> positions.map { position -> position to word } }
            .sortedBy { it.first }
            .map { it.second }

        if (orderedWords.isEmpty()) return null
        return orderedWords.joinToString(" ")
    }

    private fun getWorkByDoi(doi: String): OpenAlexWork? {
        val normalizedDoi = doi
            .trim()
            .removePrefix("https://doi.org/")
            .removePrefix("http://doi.org/")
            .removePrefix("doi:")
            .trim()
            .lowercase()

        if (normalizedDoi.isBlank()) return null

        // Use the direct object endpoint: /works/doi:{doi}
        // This is OpenAlex's canonical exact-DOI lookup — returns one work or 404.
        // Do NOT use filter=doi: here — that endpoint fuzzy-matches and can return wrong results.
        val request = HttpRequest.newBuilder()
            .uri(URI("https://api.openalex.org/works/doi:$normalizedDoi"))
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) return null

        val work = json.decodeFromString<OpenAlexWork>(response.body())

        // Validate that the returned work's DOI exactly matches what we requested.
        val returnedDoi = work.doi
            ?.removePrefix("https://doi.org/")
            ?.removePrefix("http://doi.org/")
            ?.trim()
            ?.lowercase()

        if (returnedDoi != normalizedDoi) {
            println("[OpenAlexService] DOI mismatch: requested=$normalizedDoi returned=$returnedDoi")
            return null
        }

        return work
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
    val results: List<OpenAlexWork> = emptyList(),
    val meta: OpenAlexMeta? = null,
)

@Serializable
private data class OpenAlexMeta(
    val count: Int = 0,
)

@Serializable
private data class OpenAlexWork(
    @SerialName("display_name") val title: String,
    @SerialName("publication_year") val publicationYear: Int? = null,
    val doi: String? = null,
    @SerialName("abstract_inverted_index") val abstractInvertedIndex: Map<String, List<Int>>? = null,
    @SerialName("open_access") val openAccess: OpenAccessInfo? = null,
    @SerialName("best_oa_location") val bestOaLocation: BestOaLocation? = null
)

@Serializable
private data class OpenAccessInfo(
    @SerialName("is_oa") val isOpenAccess: Boolean? = null,
    @SerialName("oa_status") val oaStatus: String? = null
)

@Serializable
private data class BestOaLocation(
    @SerialName("pdf_url") val pdfUrl: String? = null,
    @SerialName("landing_page_url") val landingPageUrl: String? = null
)