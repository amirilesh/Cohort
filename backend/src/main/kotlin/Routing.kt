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

@Serializable
data class StudyCardRequest(
    val doi: String? = null,
    val url: String? = null,
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
            post("/studycard") {
                val queryDoi = call.request.queryParameters["doi"].asPresentValue()
                val queryUrl = call.request.queryParameters["url"].asPresentValue()
                val body = if (queryDoi == null && queryUrl == null) {
                    call.receiveStudyCardRequestOrNull()
                } else {
                    null
                }
                val bodyDoi = body?.doi.asPresentValue()
                val bodyUrl = body?.url.asPresentValue()

                call.enqueueStudyCardJob(
                    doi = queryDoi ?: bodyDoi,
                    url = queryUrl ?: bodyUrl,
                )
            }

            get("/studycard") {
                call.enqueueStudyCardJob(
                    doi = call.request.queryParameters["doi"].asPresentValue(),
                    url = call.request.queryParameters["url"].asPresentValue(),
                )
            }
        }

        get("/studycard/{jobId}") {
            val jobId = call.parameters["jobId"].asPresentValue()
            if (jobId == null) {
                call.respond(HttpStatusCode.BadRequest, ApiErrorResponse(reason = "missing_job_id"))
                return@get
            }

            val job = InMemoryJobStore.getJob(jobId)
            if (job == null) {
                call.respond(HttpStatusCode.NotFound, ApiErrorResponse(reason = "job_not_found"))
                return@get
            }

            call.respondStudyCardJob(job)
        }

        get("/studycards/recent") {
            call.respond(HistoryQueries.getRecentStudyCards())
        }

        post("/studycards/save") {
            val url = call.request.queryParameters["url"]
            if (url.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiErrorResponse(reason = "missing_url"))
                return@post
            }
            if (url.length > 2000) {
                call.respond(HttpStatusCode.BadRequest, ApiErrorResponse(reason = "url_too_long"))
                return@post
            }

            if (!StudyCardPersistence.markSaved(url)) {
                call.respond(HttpStatusCode.NotFound, ApiErrorResponse(reason = "study_card_not_found"))
                return@post
            }

            call.respond(HttpStatusCode.OK)
        }

        delete("/studycards/save") {
            val url = call.request.queryParameters["url"]
            if (url.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiErrorResponse(reason = "missing_url"))
                return@delete
            }
            if (url.length > 2000) {
                call.respond(HttpStatusCode.BadRequest, ApiErrorResponse(reason = "url_too_long"))
                return@delete
            }

            if (!StudyCardPersistence.unmarkSaved(url)) {
                call.respond(HttpStatusCode.NotFound, ApiErrorResponse(reason = "study_card_not_found"))
                return@delete
            }

            call.respond(HttpStatusCode.OK)
        }

        get("/studycards/saved") {
            call.respond(HistoryQueries.getSavedStudyCards())
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

private fun String?.asPresentValue(): String? {
    return this?.trim()?.takeIf { it.isNotBlank() }
}

private suspend fun ApplicationCall.receiveStudyCardRequestOrNull(): StudyCardRequest? {
    return try {
        receiveNullable<StudyCardRequest>()
    } catch (_: Exception) {
        null
    }
}

private suspend fun ApplicationCall.enqueueStudyCardJob(doi: String?, url: String?) {
    if (doi != null) {
        if (doi.length > 200) {
            respond(HttpStatusCode.BadRequest, ApiErrorResponse(reason = "doi_too_long"))
            return
        }

        respond(HttpStatusCode.Accepted, StudyCardJobService.start(doi = doi, url = null))
        return
    }

    if (url == null) {
        respond(HttpStatusCode.BadRequest, ApiErrorResponse(reason = "missing_doi_or_url"))
        return
    }
    if (url.length > 2000) {
        respond(HttpStatusCode.BadRequest, ApiErrorResponse(reason = "url_too_long"))
        return
    }

    respond(HttpStatusCode.Accepted, StudyCardJobService.start(doi = null, url = url))
}

private suspend fun ApplicationCall.respondStudyCardJob(job: AsyncJobResult) {
    when (job.status) {
        JobStatus.PROCESSING -> respond(ProcessingJobResult(status = job.status))
        JobStatus.COMPLETED -> {
            val result = job.result
            if (result == null) {
                respond(FailedJobResult(status = JobStatus.FAILED, error = "study_card_result_missing"))
            } else {
                respond(CompletedJobResult(status = job.status, result = result))
            }
        }
        JobStatus.FAILED -> respond(
            FailedJobResult(
                status = job.status,
                error = job.error ?: "study_card_generation_failed",
            )
        )
    }
}
