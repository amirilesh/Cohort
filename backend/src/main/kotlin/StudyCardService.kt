package com.cohort

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Serializable
data class StudyCardResponse(
    val url: String,
    val success: Boolean,
    val source: String = "",
    val sourceType: String = "FULL_TEXT",  // FULL_TEXT | ABSTRACT_ONLY | METADATA_ONLY
    val tldr: String = "",
    val studyDesign: String = "",
    val keyFindings: List<String> = emptyList(),
    val limitations: String = "",
    val reason: String? = null,
)

object StudyCardService {
    private val log = LoggerFactory.getLogger(StudyCardService::class.java)
    private const val DEFAULT_MODEL = "gpt-4.1-mini"
    private const val MAX_INPUT_TEXT_LENGTH = 12000
    private const val FALLBACK_LIMITATIONS =
        "LLM unavailable; fallback summary generated from extracted text."

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun generate(url: String): StudyCardResponse {
        val pdfText = PdfTextService.extractText(url)

        if (!pdfText.success || pdfText.extractedText.isNullOrBlank()) {
            return StudyCardResponse(
                url = url,
                success = false,
                sourceType = "METADATA_ONLY",
                reason = pdfText.reason ?: "pdf_download_failed",
            )
        }

        val llmResult = generateFromExtractedText(url, pdfText.extractedText)

        // llmResult is null only when no API key is set — fall back silently
        // llmResult.success == false means the LLM call failed — still fall back but preserve reason
        if (llmResult != null && !llmResult.success) {
            return generateFallbackStudyCard(url, pdfText.extractedText)
        }

        return llmResult ?: generateFallbackStudyCard(url, pdfText.extractedText)
    }

    fun generateFromDoi(doi: String): StudyCardResponse {
        val candidates = OpenAlexService.getPdfCandidatesForDoi(doi)
            ?: return StudyCardResponse(url = "", success = false, sourceType = "METADATA_ONLY", reason = "openalex_not_found")

        if (candidates.isNotEmpty()) {
            log.info("doi={} pdf_candidates={}", doi, candidates.size)

            for (candidate in candidates) {
                val result = generate(candidate)
                if (result.success) {
                    log.info("doi={} pdf_success url={}", doi, candidate)
                    return result
                }
                log.info("doi={} pdf_failed url={} reason={}", doi, candidate, result.reason)
            }

            log.warn("doi={} all_candidates_failed count={}", doi, candidates.size)
        }

        val paper = OpenAlexService.getPaperByDoi(doi)
        return generateFromAbstractOrMetadata(doi, paper)
    }

    private fun generateFromExtractedText(url: String, extractedText: String): StudyCardResponse? {
        val apiKey = System.getenv("OPENAI_API_KEY")
        if (apiKey.isNullOrBlank()) return null

        val model = System.getenv("OPENAI_MODEL") ?: DEFAULT_MODEL
        val requestBody = buildRequestBody(model, extractedText.take(MAX_INPUT_TEXT_LENGTH))

        return try {
            val client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build()

            val request = HttpRequest.newBuilder()
                .uri(URI("https://api.openai.com/v1/chat/completions"))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() !in 200..299) {
                return StudyCardResponse(url = url, success = false, reason = "llm_generation_failed")
            }

            parseStudyCard(url, response.body())
                ?: StudyCardResponse(url = url, success = false, reason = "llm_generation_failed")
        } catch (_: Exception) {
            StudyCardResponse(url = url, success = false, reason = "llm_generation_failed")
        }
    }

    private fun buildRequestBody(model: String, extractedText: String): String {
        val payload = OpenAiChatRequest(
            model = model,
            temperature = 0.0,
            responseFormat = ResponseFormat(type = "json_object"),
            messages = listOf(
                ChatMessage(
                    role = "system",
                    content =
                        "You create concise study cards for research papers. " +
                                "Explain everything in very simple language, like to a beginner. " +
                                "Avoid scientific jargon and complex terms. " +
                                "Use short sentences. " +
                                "Explain like to a 16-year-old with no medical background. " +
                                "Use only the information from the provided paper text. " +
                                "Do not invent medicines, patients, placebo groups, outcomes, or results that are not clearly present in the text. " +
                                "If the text is unclear or incomplete, say that clearly instead of guessing. " +
                                "Return valid JSON with exactly these fields: " +
                                "tldr, studyDesign, keyFindings, limitations. " +
                                "keyFindings must be an array of short, simple bullet points.",
                ),
                ChatMessage(
                    role = "user",
                    content =
                        "Create a study card from the paper text below.\n\n" +
                                extractedText,
                ),
            ),
        )

        return json.encodeToString(OpenAiChatRequest.serializer(), payload)
    }

    private fun parseStudyCard(url: String, responseBody: String): StudyCardResponse? {
        return try {
            val responseJson = json.parseToJsonElement(responseBody).jsonObject
            val content = responseJson.extractMessageContent() ?: return null
            val studyCardJson = json.parseToJsonElement(content).jsonObject

            StudyCardResponse(
                url = url,
                success = true,
                source = "llm",
                sourceType = "FULL_TEXT",
                tldr = studyCardJson.stringValue("tldr"),
                studyDesign = studyCardJson.stringValue("studyDesign"),
                keyFindings = studyCardJson.stringList("keyFindings"),
                limitations = studyCardJson.stringValue("limitations"),
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun generateFallbackStudyCard(url: String, extractedText: String): StudyCardResponse {
        val sections = extractUsefulSections(extractedText)
        val fallbackText = cleanText(extractedText)

        val tldr = firstMeaningfulParagraph(
            sections["abstract"]
                ?: sections["conclusion"]
                ?: sections["summary"]
                ?: fallbackText,
        )
        val findingText = listOfNotNull(
            sections["results"],
            sections["discussion"],
            sections["conclusion"],
        ).joinToString(" ")

        return StudyCardResponse(
            url = url,
            success = true,
            source = "fallback",
            sourceType = "FULL_TEXT",
            tldr = tldr,
            studyDesign = detectStudyDesign(fallbackText),
            keyFindings = extractFindings(findingText.ifBlank { fallbackText }),
            limitations = FALLBACK_LIMITATIONS,
        )
    }

    private fun generateFromAbstractOrMetadata(doi: String, paper: PaperPreview?): StudyCardResponse {
        val abstract = paper?.abstractText
        if (abstract.isNullOrBlank()) {
            log.warn("doi={} no_abstract_available", doi)
            return StudyCardResponse(
                url = paper?.oaUrl ?: "",
                success = false,
                sourceType = "METADATA_ONLY",
                reason = "no_pdf_or_abstract",
            )
        }

        log.info("doi={} generating_from_abstract", doi)
        val url = paper.oaUrl ?: ""
        val llmResult = generateFromAbstract(url, abstract)

        if (llmResult != null && llmResult.success) {
            return llmResult
        }

        log.warn("doi={} abstract_llm_failed", doi)
        return StudyCardResponse(
            url = url,
            success = false,
            sourceType = "METADATA_ONLY",
            reason = "no_pdf_or_abstract",
        )
    }

    private fun generateFromAbstract(url: String, abstract: String): StudyCardResponse? {
        val apiKey = System.getenv("OPENAI_API_KEY")
        if (apiKey.isNullOrBlank()) return null

        val model = System.getenv("OPENAI_MODEL") ?: DEFAULT_MODEL
        val requestBody = buildAbstractRequestBody(model, abstract)

        return try {
            val client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build()

            val request = HttpRequest.newBuilder()
                .uri(URI("https://api.openai.com/v1/chat/completions"))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() !in 200..299) {
                return StudyCardResponse(url = url, success = false, sourceType = "ABSTRACT_ONLY", reason = "llm_generation_failed")
            }

            parseAbstractStudyCard(url, response.body())
                ?: StudyCardResponse(url = url, success = false, sourceType = "ABSTRACT_ONLY", reason = "llm_generation_failed")
        } catch (_: Exception) {
            StudyCardResponse(url = url, success = false, sourceType = "ABSTRACT_ONLY", reason = "llm_generation_failed")
        }
    }

    private fun buildAbstractRequestBody(model: String, abstract: String): String {
        val payload = OpenAiChatRequest(
            model = model,
            temperature = 0.0,
            responseFormat = ResponseFormat(type = "json_object"),
            messages = listOf(
                ChatMessage(
                    role = "system",
                    content =
                        "You create concise study cards from research paper abstracts. " +
                                "Explain everything in very simple language, like to a beginner. " +
                                "Avoid scientific jargon. Use short sentences. " +
                                "Only use information present in the abstract. " +
                                "Return valid JSON with exactly these fields: " +
                                "tldr, studyDesign, keyFindings, limitations. " +
                                "keyFindings must be an array of short, simple bullet points. " +
                                "For limitations, note that this summary is based on the abstract only.",
                ),
                ChatMessage(
                    role = "user",
                    content = "Create a study card from this paper abstract:\n\n$abstract",
                ),
            ),
        )
        return json.encodeToString(OpenAiChatRequest.serializer(), payload)
    }

    private fun parseAbstractStudyCard(url: String, responseBody: String): StudyCardResponse? {
        return try {
            val responseJson = json.parseToJsonElement(responseBody).jsonObject
            val content = responseJson.extractMessageContent() ?: return null
            val studyCardJson = json.parseToJsonElement(content).jsonObject

            StudyCardResponse(
                url = url,
                success = true,
                source = "llm_abstract",
                sourceType = "ABSTRACT_ONLY",
                tldr = studyCardJson.stringValue("tldr"),
                studyDesign = studyCardJson.stringValue("studyDesign"),
                keyFindings = studyCardJson.stringList("keyFindings"),
                limitations = studyCardJson.stringValue("limitations"),
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun extractUsefulSections(text: String): Map<String, String> {
        val sectionNames = setOf("abstract", "results", "discussion", "conclusion", "summary")
        val sections = mutableMapOf<String, StringBuilder>()
        var currentSection: String? = null

        text.lines().forEach { line ->
            val header = sectionHeader(line, sectionNames)

            if (header != null) {
                currentSection = header
                sections.putIfAbsent(header, StringBuilder())
            } else if (currentSection != null) {
                sections[currentSection]?.appendLine(line)
            }
        }

        return sections.mapValues { cleanText(it.value.toString()) }
            .filterValues { it.isNotBlank() }
    }

    private fun sectionHeader(line: String, sectionNames: Set<String>): String? {
        val cleanedLine = line
            .trim()
            .lowercase()
            .removePrefix("section")
            .trim()
            .trim('.', ':')
            .replace(Regex("""^\d+(\.\d+)*\.?\s+"""), "")

        return sectionNames.firstOrNull { section ->
            cleanedLine == section || cleanedLine == "${section}s"
        }
    }

    private fun detectStudyDesign(text: String): String {
        val lowerText = text.lowercase()
        val designs = listOf(
            "meta-analysis" to "Meta-analysis",
            "randomized" to "Randomized trial",
            "cross-sectional" to "Cross-sectional study",
            "cohort" to "Cohort study",
            "survey" to "Survey",
            "trial" to "Trial",
            "review" to "Review",
        )

        return designs.firstOrNull { (keyword, _) ->
            lowerText.contains(keyword)
        }?.second ?: "Unknown"
    }

    private fun extractFindings(text: String): List<String> {
        return text
            .split(Regex("""(?<=[.!?])\s+"""))
            .map { it.trim() }
            .filter { isUsefulSentence(it) }
            .map { shortenSentence(it) }
            .distinct()
            .take(3)
    }

    private fun isUsefulSentence(sentence: String): Boolean {
        if (sentence.length < 40 || sentence.length > 300) {
            return false
        }

        val lowerSentence = sentence.lowercase()
        val usefulWords = listOf(
            "found",
            "showed",
            "increased",
            "decreased",
            "associated",
            "significant",
            "improved",
            "reduced",
            "higher",
            "lower",
            "result",
            "conclusion",
        )

        return usefulWords.any { lowerSentence.contains(it) }
    }

    private fun firstMeaningfulParagraph(text: String): String {
        return cleanText(text)
            .split(Regex("""\n\s*\n"""))
            .map { it.trim() }
            .firstOrNull { it.length >= 40 }
            ?.let { shortenSentence(it) }
            ?: ""
    }

    private fun shortenSentence(text: String): String {
        val cleanedText = cleanText(text)

        return if (cleanedText.length <= 260) {
            cleanedText
        } else {
            cleanedText.take(257).trimEnd() + "..."
        }
    }

    private fun cleanText(text: String): String {
        return text
            .replace("\r\n", "\n")
            .replace(Regex("""[ \t]+"""), " ")
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()
    }

    private fun JsonObject.extractMessageContent(): String? {
        val choices = this["choices"]?.jsonArray ?: return null
        val firstChoice = choices.firstOrNull()?.jsonObject ?: return null
        val message = firstChoice["message"]?.jsonObject ?: return null
        return message["content"]?.jsonPrimitive?.contentOrNull
    }

    private fun JsonObject.stringValue(key: String): String {
        return this[key]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
    }

    private fun JsonObject.stringList(key: String): List<String> {
        val values = this[key] as? JsonArray ?: return emptyList()

        return values.mapNotNull { element ->
            (element as? JsonPrimitive)?.contentOrNull?.trim()
        }.filter { it.isNotBlank() }
    }
}

@Serializable
private data class OpenAiChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double,
    @SerialName("response_format")
    val responseFormat: ResponseFormat,
)

@Serializable
private data class ChatMessage(
    val role: String,
    val content: String,
)

@Serializable
private data class ResponseFormat(
    val type: String,
)
