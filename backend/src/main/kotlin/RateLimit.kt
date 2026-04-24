package com.cohort

import io.ktor.server.application.*
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.*
import kotlin.time.Duration.Companion.minutes

val SearchRateLimit = RateLimitName("search")
val StudyCardRateLimit = RateLimitName("studycard")

fun Application.configureRateLimit() {
    install(RateLimit) {
        register(SearchRateLimit) {
            rateLimiter(limit = 30, refillPeriod = 1.minutes)
            requestKey { call -> call.request.local.remoteAddress }        }
        register(StudyCardRateLimit) {
            rateLimiter(limit = 10, refillPeriod = 1.minutes)
            requestKey { call -> call.request.local.remoteAddress }        }
    }
}
