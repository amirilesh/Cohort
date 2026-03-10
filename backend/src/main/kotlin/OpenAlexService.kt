package com.cohort

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

object OpenAlexService {

    private val client = HttpClient(CIO)

    suspend fun searchWorks(query: String): String {

        val url = "https://api.openalex.org/works?search=$query"

        val response = client.get(url)

        return response.bodyAsText()
    }
}