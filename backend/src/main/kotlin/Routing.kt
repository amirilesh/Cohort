package com.cohort

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import kotlinx.serialization.Serializable

@Serializable
data class PaperPreview(
    val title: String,
    val year: Int,
    val doi: String
)

fun Application.configureRouting() {
    routing {

        get("/") {
            call.respondText("Hello World!")
        }

        get("/search") {
            val query = call.request.queryParameters["q"] ?: "unknown"

            val results = listOf(
                PaperPreview(
                    title = "Test paper about $query",
                    year = 2024,
                    doi = "10.0000/test-doi"
                )
            )

            call.respond(results)
        }
    }
}