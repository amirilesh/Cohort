package com.cohort

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Serializable
data class StudyCardResponse(
    val url: String,
    val success: Boolean,
    val tldr: String = "",
    val studyDesign: String = "",
    val keyFindings: List<String> = emptyList(),
    val limitations: String = "",
)

object StudyCardService {
    private const val DEFAULT_MODEL = "gpt-4.1-mini"
    private const val MAX_INPUT_TEXT_LENGTH = 12000

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun generate(url: String): StudyCardResponse {
        val pdfText = PdfTextService.extractText(url)

        if (!pdfText.success || pdfText.extractedText.isNullOrBlank()) {
            return StudyCardResponse(
                url = url,
                success = false,
            )
        }

        return generateFromExtractedText(url, pdfText.extractedText)
    }

    private fun generateFromExtractedText(url: String, extractedText: String): StudyCardResponse {
        val apiKey = System.getenv("OPENAI_API_KEY") ?: return StudyCardResponse(
            url = url,
            success = false,
        )

        println("API KEY PRESENT: " + apiKey.take(10) + "...")

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

            println("STATUS: " + response.statusCode())
            println("BODY: " + response.body())

            if (response.statusCode() !in 200..299) {
                return StudyCardResponse(
                    url = url,
                    success = false,
                )
            }

            parseStudyCard(url, response.body())
        } catch (e: Exception) {
            println("EXCEPTION: " + e.message)
            StudyCardResponse(
                url = url,
                success = false,
            )
        }
    }

    private fun buildRequestBody(model: String, extractedText: String): String {
        val payload = OpenAiChatRequest(
            model = model,
            temperature = 0.2,
            responseFormat = ResponseFormat(type = "json_object"),
            messages = listOf(
                ChatMessage(
                    role = "system",
                    content =
                        "You create concise study cards for research papers. " +
                            "Return valid JSON with exactly these fields: " +
                            "tldr, studyDesign, keyFindings, limitations. " +
                            "keyFindings must be an array of short strings.",
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

    private fun parseStudyCard(url: String, responseBody: String): StudyCardResponse {
        return try {
            val responseJson = json.parseToJsonElement(responseBody).jsonObject
            val content = responseJson.extractMessageContent() ?: return StudyCardResponse(
                url = url,
                success = false,
            )
            val studyCardJson = json.parseToJsonElement(content).jsonObject

            StudyCardResponse(
                url = url,
                success = true,
                tldr = studyCardJson.stringValue("tldr"),
                studyDesign = studyCardJson.stringValue("studyDesign"),
                keyFindings = studyCardJson.stringList("keyFindings"),
                limitations = studyCardJson.stringValue("limitations"),
            )
        } catch (_: Exception) {
            StudyCardResponse(
                url = url,
                success = false,
            )
        }
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
