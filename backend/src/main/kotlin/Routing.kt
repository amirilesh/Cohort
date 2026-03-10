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

            val result = OpenAlexService.searchWorks(query)

            call.respondText(
                text = result,
                contentType = ContentType.Application.Json
            )
        }
    }
}