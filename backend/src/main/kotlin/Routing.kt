package com.cohort

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {

        get("/") {
            call.respondText("Hello World!")
        }

        get("/search") {
            val query = call.request.queryParameters["q"] ?: "unknown"
            val papers = OpenAlexService.searchWorks(query)
            call.respond(papers)
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
                        success = false
                    )
                )
                return@get
            }

            val result = PdfTextService.extractText(url)
            call.respond(result)
        }

        get("/studycard") {
            val url = call.request.queryParameters["url"]

            if (url.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    StudyCardResponse(
                        url = "",
                        success = false,
                    )
                )
                return@get
            }

            val result = StudyCardService.generate(url)
            call.respond(result)
        }
    }
}
