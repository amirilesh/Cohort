package com.cohort

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Serializable
data class HealthResponse(
    val status: String,
    val ktor: String,
    val database: String,
    val openAlex: String,
    val openAiKey: String,
)

fun Application.configureRouting() {
    routing {

        get("/") {
            call.respondText("Hello World!")
        }

        rateLimit(SearchRateLimit) {
            get("/search") {
                val query = call.request.queryParameters["q"]
                if (query.isNullOrBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiErrorResponse(reason = "missing_query"),
                    )
                    return@get
                }
                if (query.length > 200) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiErrorResponse(reason = "query_too_long"),
                    )
                    return@get
                }

                val pageParam = call.request.queryParameters["page"]
                val page = pageParam?.toIntOrNull()
                if (pageParam != null && (page == null || page < 1)) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiErrorResponse(reason = "invalid_page"),
                    )
                    return@get
                }

                val perPageParam = call.request.queryParameters["perPage"]
                val perPage = perPageParam?.toIntOrNull()
                if (perPageParam != null && (perPage == null || perPage <= 0)) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiErrorResponse(reason = "invalid_per_page"),
                    )
                    return@get
                }

                val result = OpenAlexService.searchWorks(
                    query = query,
                    page = page ?: 1,
                    perPage = perPage ?: 10,
                )
                SearchPersistence.save(result)
                call.respond(result)
            }
        }

        get("/pdftext") {
            val url = call.request.queryParameters["url"]

            if (url.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiErrorResponse(reason = "missing_url"))
                return@get
            }
            if (url.length > 2000) {
                call.respond(HttpStatusCode.BadRequest, ApiErrorResponse(reason = "url_too_long"))
                return@get
            }

            val result = PdfTextService.extractText(url)
            call.respond(result)
        }

        rateLimit(StudyCardRateLimit) {
            get("/studycard") {
                val doi = call.request.queryParameters["doi"]
                val url = call.request.queryParameters["url"]

                if (!doi.isNullOrBlank()) {
                    if (doi.length > 200) {
                        call.respond(HttpStatusCode.BadRequest, ApiErrorResponse(reason = "doi_too_long"))
                        return@get
                    }
                    val cached = StudyCardPersistence.findByDoi(doi)
                    if (cached != null) {
                        call.respond(cached)
                        return@get
                    }
                    val result = StudyCardService.generateFromDoi(doi)
                    if (result.success) StudyCardPersistence.save(result, doi)
                    call.respond(result)
                    return@get
                }

                if (url.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ApiErrorResponse(reason = "missing_doi_or_url"))
                    return@get
                }
                if (url.length > 2000) {
                    call.respond(HttpStatusCode.BadRequest, ApiErrorResponse(reason = "url_too_long"))
                    return@get
                }

                val cached = StudyCardPersistence.findByUrl(url)
                if (cached != null) {
                    call.respond(cached)
                    return@get
                }
                val result = StudyCardService.generate(url)
                if (result.success) StudyCardPersistence.save(result, doi = null)
                call.respond(result)
            }
        }

        get("/studycards/recent") {
            call.respond(HistoryQueries.getRecentStudyCards())
        }

        get("/search/history") {
            call.respond(HistoryQueries.getSearchHistory())
        }

        get("/analytics/top-searches") {
            call.respond(AnalyticsQueries.getTopSearches())
        }

        get("/analytics/popular-papers") {
            call.respond(AnalyticsQueries.getPopularPapers())
        }

        get("/health") {
            val dbStatus = Database.checkConnection()
            val openAlexStatus = checkOpenAlex()
            val openAiKeyStatus = if (System.getenv("OPENAI_API_KEY").isNullOrBlank()) "missing" else "present"
            val overallStatus = if (dbStatus == "up" && openAlexStatus == "up") "ok" else "degraded"

            call.respond(
                HealthResponse(
                    status = overallStatus,
                    ktor = "up",
                    database = dbStatus,
                    openAlex = openAlexStatus,
                    openAiKey = openAiKeyStatus,
                )
            )
        }
    }
}

private fun checkOpenAlex(): String {
    return try {
        val client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build()
        val request = HttpRequest.newBuilder()
            .uri(URI("https://api.openalex.org/works?search=test&per_page=1"))
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.discarding())
        if (response.statusCode() in 200..299) "up" else "down"
    } catch (_: Exception) {
        "down"
    }
}