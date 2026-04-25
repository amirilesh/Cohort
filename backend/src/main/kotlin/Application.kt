package com.cohort

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

@Serializable
data class ApiErrorResponse(val success: Boolean = false, val reason: String)

fun Application.module() {
    Database.initSchema()
    configureHTTP()
    configureMonitoring()
    configureSerialization()
    configureStatusPages()
    configureRateLimit()
    configureRouting()
}

fun Application.configureStatusPages() {
    install(StatusPages) {
        status(HttpStatusCode.TooManyRequests) { call, _ ->
            call.respond(
                HttpStatusCode.TooManyRequests,
                ApiErrorResponse(reason = "rate_limit_exceeded"),
            )
        }
    }
}
