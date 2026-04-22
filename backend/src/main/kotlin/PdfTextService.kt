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
    val reason: String? = null,
)

object PdfTextService {
    private const val MAX_RETURNED_TEXT_LENGTH = 5000
    private const val MAX_REDIRECTS = 8
    private const val MAX_PDF_CANDIDATES = 5
    private const val MAX_RETRIES = 2
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
                reason = downloadResult.reason,
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
                reason = "pdf_text_extraction_failed",
            )
        }
    }

    private fun downloadPdf(url: String): PdfDownloadResult {
        val host = try { URL(url).host } catch (_: Exception) { url }
        println("[PdfTextService] host=$host url=$url")

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
            println("[PdfTextService] host=$host result=FAILED reason=request_failed")
            return PdfDownloadResult(contentType = null, bytes = ByteArray(0), isLikelyPdf = false, reason = "pdf_download_failed")
        }

        if (isLikelyPdf(firstResponse.contentType, firstResponse.bytes)) {
            println("[PdfTextService] host=$host result=PDF_FOUND via=direct")
            return PdfDownloadResult(
                contentType = firstResponse.contentType,
                bytes = firstResponse.bytes,
                isLikelyPdf = true,
            )
        }

        if (!isHtml(firstResponse.contentType, firstResponse.bytes)) {
            println("[PdfTextService] host=$host result=FAILED reason=not_html_not_pdf")
            return PdfDownloadResult(
                contentType = firstResponse.contentType,
                bytes = firstResponse.bytes,
                isLikelyPdf = false,
                reason = "pdf_download_failed",
            )
        }

        val html = firstResponse.bytes.toString(Charsets.UTF_8)
        val candidateUrls = findPdfCandidateUrls(firstResponse.finalUrl, html)

        if (candidateUrls.isEmpty()) {
            // No candidates from HTML — try simple URL transformations
            val urlVariants = buildUrlVariants(firstResponse.finalUrl)
            println("[PdfTextService] host=$host no_html_candidates trying ${urlVariants.size} url_variants")

            for (variant in urlVariants) {
                val variantResponse = fetchWithRedirects(
                    client = client,
                    url = variant,
                    acceptHeader = PDF_ACCEPT,
                    referer = firstResponse.finalUrl,
                ) ?: continue

                if (isLikelyPdf(variantResponse.contentType, variantResponse.bytes)) {
                    println("[PdfTextService] host=$host result=PDF_FOUND via=url_variant variant=$variant")
                    return PdfDownloadResult(
                        contentType = variantResponse.contentType,
                        bytes = variantResponse.bytes,
                        isLikelyPdf = true,
                    )
                }
            }

            println("[PdfTextService] host=$host result=FAILED reason=no_pdf_candidate_found")
            return PdfDownloadResult(
                contentType = firstResponse.contentType,
                bytes = firstResponse.bytes,
                isLikelyPdf = false,
                reason = "pdf_download_failed",
            )
        }

        println("[PdfTextService] host=$host html_candidates=${candidateUrls.size}")

        for (candidateUrl in candidateUrls.take(MAX_PDF_CANDIDATES)) {
            val candidateResponse = fetchWithRedirects(
                client = client,
                url = candidateUrl,
                acceptHeader = PDF_ACCEPT,
                referer = firstResponse.finalUrl,
            ) ?: continue

            if (isLikelyPdf(candidateResponse.contentType, candidateResponse.bytes)) {
                println("[PdfTextService] host=$host result=PDF_FOUND via=html_candidate candidate=$candidateUrl")
                return PdfDownloadResult(
                    contentType = candidateResponse.contentType,
                    bytes = candidateResponse.bytes,
                    isLikelyPdf = true,
                )
            }
        }

        println("[PdfTextService] host=$host result=FAILED reason=html_only_no_valid_pdf")
        return PdfDownloadResult(
            contentType = firstResponse.contentType,
            bytes = firstResponse.bytes,
            isLikelyPdf = false,
            reason = "pdf_download_failed",
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
            var lastException: Exception? = null

            // Retry up to MAX_RETRIES times on network failure
            repeat(MAX_RETRIES + 1) retryLoop@{
                try {
                    val requestBuilder = HttpRequest.newBuilder()
                        .uri(URL(currentUrl).toURI())
                        .timeout(Duration.ofSeconds(20))
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", acceptHeader)
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .header("Upgrade-Insecure-Requests", "1")

                    if (!currentReferer.isNullOrBlank()) {
                        requestBuilder.header("Referer", currentReferer)
                    }

                    val request = requestBuilder.GET().build()
                    val response = client.send(request, HttpResponse.BodyHandlers.ofByteArray())
                    val contentType = response.headers().firstValue("Content-Type").orElse(null)

                    if (isRedirect(response.statusCode())) {
                        val location = response.headers().firstValue("Location").orElse(null)

                        if (!location.isNullOrBlank()) {
                            val previousUrl = currentUrl
                            currentUrl = URL(URL(currentUrl), location).toString()
                            currentReferer = previousUrl
                            return@repeat  // follow the redirect on the next outer hop
                        }
                    }

                    return HttpFetchResult(
                        finalUrl = currentUrl,
                        contentType = contentType,
                        bytes = response.body(),
                    )
                } catch (e: Exception) {
                    lastException = e
                    // will retry
                }
            }

            // All retries exhausted for this hop
            if (lastException != null) return null
        }

        return null
    }

    private fun findPdfCandidateUrls(baseUrl: String, html: String): List<String> {
        val pdfMatches = linkedSetOf<String>()    // direct PDF signals — preferred
        val otherMatches = linkedSetOf<String>()  // download / fulltext / article links

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

        // citation_pdf_url meta tag — highest priority
        metaRegex.findAll(html).forEach { match ->
            pdfMatches += resolveUrl(baseUrl, match.groupValues[1])
        }

        hrefRegex.findAll(html).forEach { match ->
            val candidate = match.groupValues[1]
            val normalized = candidate.lowercase()

            when {
                // Direct PDF signals — preferred
                normalized.contains(".pdf") ||
                        normalized.contains("/pdf") ||
                        normalized.contains("pdf=") ||
                        normalized.contains("downloadpdf") -> {
                    pdfMatches += resolveUrl(baseUrl, candidate)
                }

                // Download / fulltext / article links — secondary
                normalized.contains("download") ||
                        normalized.contains("fulltext") ||
                        normalized.contains("full-text") ||
                        normalized.contains("article") -> {
                    otherMatches += resolveUrl(baseUrl, candidate)
                }
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
                pdfMatches += resolveUrl(baseUrl, candidate)
            }
        }

        // PDF-looking links first, then other download candidates
        return (pdfMatches + otherMatches).filter { it.isNotBlank() }
    }

    // Simple URL transformations to try when HTML parsing finds no candidates
    private fun buildUrlVariants(url: String): List<String> {
        val variants = mutableListOf<String>()

        if (url.contains("/full")) {
            variants += url.replace("/full", "/pdf")
        }
        if (url.contains("/article")) {
            variants += url.replace("/article", "/pdf")
        }
        // Append .pdf only if the path has no extension and no query string
        try {
            val parsed = URL(url)
            val path = parsed.path
            if (!path.contains(".") && parsed.query == null) {
                variants += url.trimEnd('/') + ".pdf"
            }
        } catch (_: Exception) { /* ignore malformed URLs */ }

        return variants
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
        val reason: String? = null,
    )

    private data class HttpFetchResult(
        val finalUrl: String,
        val contentType: String?,
        val bytes: ByteArray,
    )
}