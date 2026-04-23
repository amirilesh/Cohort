package com.cohort

import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    Database.initSchema()
    configureHTTP()
    configureMonitoring()
    configureSerialization()
    configureRateLimit()
    configureRouting()
}
