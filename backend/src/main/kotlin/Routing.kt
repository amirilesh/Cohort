package com.cohort

import io.ktor.server.application.*
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

            call.respond(result)
        }
    }
}
