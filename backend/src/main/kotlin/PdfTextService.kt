package com.cohort

import kotlinx.serialization.Serializable
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class PdfTextResponse(
    val url: String,
    val contentType: String? = null,
    val downloadedBytes: Int = 0,
    val isLikelyPdf: Boolean = false,
    val extractedText: String? = null,
    val textLength: Int = 0,
    val success: Boolean,
)

object PdfTextService {
    private const val MAX_RETURNED_TEXT_LENGTH = 5000
    private const val MAX_REDIRECTS = 5
    private const val USER_AGENT =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    fun extractText(url: String): PdfTextResponse {
        val downloadResult = downloadPdf(url)

        if (!downloadResult.isLikelyPdf || downloadResult.bytes.isEmpty()) {
            return PdfTextResponse(
                url = url,
                contentType = downloadResult.contentType,
                downloadedBytes = downloadResult.bytes.size,
                isLikelyPdf = downloadResult.isLikelyPdf,
                extractedText = null,
                textLength = 0,
                success = false,
            )
        }

        return try {
            val extractedText = extractPlainText(downloadResult.bytes)

            PdfTextResponse(
                url = url,
                contentType = downloadResult.contentType,
                downloadedBytes = downloadResult.bytes.size,
                isLikelyPdf = downloadResult.isLikelyPdf,
                extractedText = extractedText.take(MAX_RETURNED_TEXT_LENGTH),
                textLength = extractedText.length,
                success = true,
            )
        } catch (_: Exception) {
            PdfTextResponse(
                url = url,
                contentType = downloadResult.contentType,
                downloadedBytes = downloadResult.bytes.size,
                isLikelyPdf = downloadResult.isLikelyPdf,
                extractedText = null,
                textLength = 0,
                success = false,
            )
        }
    }

    private fun downloadPdf(url: String): PdfDownloadResult {
        var currentUrl = url

        repeat(MAX_REDIRECTS + 1) {
            var connection: HttpURLConnection? = null

            try {
                connection = URL(currentUrl).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15_000
                connection.readTimeout = 15_000
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("User-Agent", USER_AGENT)
                connection.setRequestProperty("Accept", "application/pdf")

                val responseCode = connection.responseCode
                val contentType = connection.contentType

                if (isRedirect(responseCode)) {
                    val location = connection.getHeaderField("Location")

                    if (!location.isNullOrBlank()) {
                        currentUrl = URL(URL(currentUrl), location).toString()
                        return@repeat
                    }
                }

                val stream = if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val bytes = stream?.use { inputStream ->
                    inputStream.readBytes()
                } ?: ByteArray(0)

                return PdfDownloadResult(
                    contentType = contentType,
                    bytes = bytes,
                    isLikelyPdf = isLikelyPdf(contentType, bytes),
                )
            } catch (_: Exception) {
                return PdfDownloadResult(
                    contentType = null,
                    bytes = ByteArray(0),
                    isLikelyPdf = false,
                )
            } finally {
                connection?.disconnect()
            }
        }

        return PdfDownloadResult(
            contentType = null,
            bytes = ByteArray(0),
            isLikelyPdf = false,
        )
    }

    private fun extractPlainText(pdfBytes: ByteArray): String {
        return PDDocument.load(pdfBytes).use { document ->
            PDFTextStripper().getText(document).trim()
        }
    }

    private fun isLikelyPdf(contentType: String?, bytes: ByteArray): Boolean {
        val looksLikePdfByHeader = contentType?.lowercase()?.contains("pdf") == true
        val looksLikePdfBySignature =
            bytes.size >= 5 &&
                    bytes[0] == '%'.code.toByte() &&
                    bytes[1] == 'P'.code.toByte() &&
                    bytes[2] == 'D'.code.toByte() &&
                    bytes[3] == 'F'.code.toByte() &&
                    bytes[4] == '-'.code.toByte()

        return looksLikePdfByHeader || looksLikePdfBySignature
    }

    private fun isRedirect(responseCode: Int): Boolean {
        return responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                responseCode == 307 ||
                responseCode == 308
    }

    private data class PdfDownloadResult(
        val contentType: String?,
        val bytes: ByteArray,
        val isLikelyPdf: Boolean,
    )
}
