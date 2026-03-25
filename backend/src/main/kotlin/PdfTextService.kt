package com.cohort

import kotlinx.serialization.Serializable
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.net.CookieManager
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

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
    private const val MAX_PDF_CANDIDATES = 5
    private const val USER_AGENT =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    private const val HTML_ACCEPT =
        "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
    private const val PDF_ACCEPT = "application/pdf"

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
        val client = HttpClient.newBuilder()
            .cookieHandler(CookieManager())
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build()

        val firstResponse = fetchWithRedirects(
            client = client,
            url = url,
            acceptHeader = HTML_ACCEPT,
            referer = null,
        )

        if (firstResponse == null) {
            return PdfDownloadResult(
                contentType = null,
                bytes = ByteArray(0),
                isLikelyPdf = false,
            )
        }

        if (isLikelyPdf(firstResponse.contentType, firstResponse.bytes)) {
            return PdfDownloadResult(
                contentType = firstResponse.contentType,
                bytes = firstResponse.bytes,
                isLikelyPdf = true,
            )
        }

        if (!isHtml(firstResponse.contentType, firstResponse.bytes)) {
            return PdfDownloadResult(
                contentType = firstResponse.contentType,
                bytes = firstResponse.bytes,
                isLikelyPdf = false,
            )
        }

        val html = firstResponse.bytes.toString(Charsets.UTF_8)
        val candidateUrls = findPdfCandidateUrls(firstResponse.finalUrl, html)

        for (candidateUrl in candidateUrls.take(MAX_PDF_CANDIDATES)) {
            val candidateResponse = fetchWithRedirects(
                client = client,
                url = candidateUrl,
                acceptHeader = PDF_ACCEPT,
                referer = firstResponse.finalUrl,
            ) ?: continue
            val candidateIsPdf = isLikelyPdf(candidateResponse.contentType, candidateResponse.bytes)

            if (candidateIsPdf) {
                return PdfDownloadResult(
                    contentType = candidateResponse.contentType,
                    bytes = candidateResponse.bytes,
                    isLikelyPdf = true,
                )
            }
        }

        return PdfDownloadResult(
            contentType = firstResponse.contentType,
            bytes = firstResponse.bytes,
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

    private fun isHtml(contentType: String?, bytes: ByteArray): Boolean {
        val looksLikeHtmlByHeader = contentType?.lowercase()?.contains("text/html") == true
        val bodyStart = bytes
            .take(200)
            .toByteArray()
            .toString(Charsets.UTF_8)
            .trimStart()
            .lowercase()

        val looksLikeHtmlByBody =
            bodyStart.startsWith("<!doctype html") || bodyStart.startsWith("<html")

        return looksLikeHtmlByHeader || looksLikeHtmlByBody
    }

    private fun fetchWithRedirects(
        client: HttpClient,
        url: String,
        acceptHeader: String,
        referer: String?,
    ): HttpFetchResult? {
        var currentUrl = url
        var currentReferer = referer

        repeat(MAX_REDIRECTS + 1) {
            try {
                val requestBuilder = HttpRequest.newBuilder()
                    .uri(URL(currentUrl).toURI())
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", acceptHeader)

                if (!currentReferer.isNullOrBlank()) {
                    requestBuilder.header("Referer", currentReferer)
                }

                val request = requestBuilder
                    .GET()
                    .build()

                val response = client.send(request, HttpResponse.BodyHandlers.ofByteArray())
                val contentType = response.headers().firstValue("Content-Type").orElse(null)

                if (isRedirect(response.statusCode())) {
                    val location = response.headers().firstValue("Location").orElse(null)

                    if (!location.isNullOrBlank()) {
                        val previousUrl = currentUrl
                        currentUrl = URL(URL(currentUrl), location).toString()
                        currentReferer = previousUrl
                        return@repeat
                    }
                }

                return HttpFetchResult(
                    finalUrl = currentUrl,
                    contentType = contentType,
                    bytes = response.body(),
                )
            } catch (_: Exception) {
                return null
            }
        }

        return null
    }

    private fun findPdfCandidateUrls(baseUrl: String, html: String): List<String> {
        val matches = linkedSetOf<String>()

        val metaRegex = Regex(
            """<meta[^>]+name=["']citation_pdf_url["'][^>]+content=["']([^"']+)["']""",
            RegexOption.IGNORE_CASE,
        )
        val hrefRegex = Regex(
            """href=["']([^"']+)["']""",
            RegexOption.IGNORE_CASE,
        )
        val srcRegex = Regex(
            """src=["']([^"']+)["']""",
            RegexOption.IGNORE_CASE,
        )

        metaRegex.findAll(html).forEach { match ->
            matches += resolveUrl(baseUrl, match.groupValues[1])
        }

        hrefRegex.findAll(html).forEach { match ->
            val candidate = match.groupValues[1]
            val normalized = candidate.lowercase()

            if (
                normalized.contains(".pdf") ||
                normalized.contains("/pdf") ||
                normalized.contains("pdf=") ||
                normalized.contains("downloadpdf")
            ) {
                matches += resolveUrl(baseUrl, candidate)
            }
        }

        srcRegex.findAll(html).forEach { match ->
            val candidate = match.groupValues[1]
            val normalized = candidate.lowercase()

            if (
                normalized.contains(".pdf") ||
                normalized.contains("/pdf") ||
                normalized.contains("pdf=") ||
                normalized.contains("downloadpdf")
            ) {
                matches += resolveUrl(baseUrl, candidate)
            }
        }

        return matches.filter { it.isNotBlank() }
    }

    private fun resolveUrl(baseUrl: String, value: String): String {
        return try {
            URL(URL(baseUrl), value).toString()
        } catch (_: Exception) {
            ""
        }
    }

    private fun isRedirect(statusCode: Int): Boolean {
        return statusCode == 301 ||
            statusCode == 302 ||
            statusCode == 303 ||
            statusCode == 307 ||
            statusCode == 308
    }

    private data class PdfDownloadResult(
        val contentType: String?,
        val bytes: ByteArray,
        val isLikelyPdf: Boolean,
    )

    private data class HttpFetchResult(
        val finalUrl: String,
        val contentType: String?,
        val bytes: ByteArray,
    )
}
