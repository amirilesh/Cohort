package com.cohort

import kotlinx.serialization.Serializable
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

object PdfInfoService {

    private val client = HttpClient.newHttpClient()

    suspend fun fetchPdfInfo(url: String): PdfInfoResponse {
        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI(url))
                .GET()
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofByteArray())
            val contentType = response.headers().firstValue("Content-Type").orElse(null)
            val contentLength = response.headers().firstValue("Content-Length").orElse(null)?.toLongOrNull()
            val body = response.body()
            val hasPdfHeader = body.size >= 4 &&
                body[0] == '%'.code.toByte() &&
                body[1] == 'P'.code.toByte() &&
                body[2] == 'D'.code.toByte() &&
                body[3] == 'F'.code.toByte()
            val isPdf = contentType?.startsWith("application/pdf") == true || hasPdfHeader

            PdfInfoResponse(
                url = url,
                contentType = contentType,
                isPdf = isPdf,
                contentLength = contentLength ?: body.size.toLong(),
                success = response.statusCode() in 200..299 && isPdf
            )
        } catch (_: Exception) {
            PdfInfoResponse(
                url = url,
                contentType = null,
                isPdf = false,
                contentLength = null,
                success = false
            )
        }
    }
}

@Serializable
data class PdfInfoResponse(
    val url: String,
    val contentType: String?,
    val isPdf: Boolean,
    val contentLength: Long?,
    val success: Boolean
)
