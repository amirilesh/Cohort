package com.cohort

import io.ktor.http.*
import io.ktor.server.application.*
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
    val openAlex: String,
    val openAiKey: String,
)

@Serializable
data class SearchErrorResponse(
    val success: Boolean = false,
    val reason: String,
)

fun Application.configureRouting() {
    routing {

        get("/") {
            call.respondText("Hello World!")
        }

        get("/search") {
            val query = call.request.queryParameters["q"]
            if (query.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    SearchErrorResponse(reason = "missing_query"),
                )
                return@get
            }

            val pageParam = call.request.queryParameters["page"]
            val page = pageParam?.toIntOrNull()
            if (pageParam != null && (page == null || page < 1)) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    SearchErrorResponse(reason = "invalid_page"),
                )
                return@get
            }

            val perPageParam = call.request.queryParameters["perPage"]
            val perPage = perPageParam?.toIntOrNull()
            if (perPageParam != null && (perPage == null || perPage <= 0)) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    SearchErrorResponse(reason = "invalid_per_page"),
                )
                return@get
            }

            val result = OpenAlexService.searchWorks(
                query = query,
                page = page ?: 1,
                perPage = perPage ?: 10,
            )
            call.respond(result)
        }

        get("/pdftext") {
            val url = call.request.queryParameters["url"]

            if (url.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    PdfTextResponse(
                        url = "",
                        extractedText = null,
                        textLength = 0,
                        success = false,
                        reason = "missing_url",
                    )
                )
                return@get
            }

            val result = PdfTextService.extractText(url)
            call.respond(result)
        }

        get("/studycard") {
            val doi = call.request.queryParameters["doi"]
            val url = call.request.queryParameters["url"]

            if (!doi.isNullOrBlank()) {
                val result = StudyCardService.generateFromDoi(doi)
                call.respond(result)
                return@get
            }

            if (url.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    StudyCardResponse(
                        url = "",
                        success = false,
                        reason = "missing_doi_or_url",
                    )
                )
                return@get
            }

            val result = StudyCardService.generate(url)
            call.respond(result)
        }

        get("/health") {
            val openAlexStatus = checkOpenAlex()
            val openAiKeyStatus = if (System.getenv("OPENAI_API_KEY").isNullOrBlank()) "missing" else "present"
            val overallStatus = if (openAlexStatus == "up") "ok" else "degraded"

            call.respond(
                HealthResponse(
                    status = overallStatus,
                    ktor = "up",
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