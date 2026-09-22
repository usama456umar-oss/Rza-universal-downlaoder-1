package com.example.data.remote

import com.example.BuildConfig
import com.example.data.model.TranscriptResult
import com.example.data.model.TranscriptSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiTranscribeService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun transcribeSpeechOrCaption(
        inputContext: String,
        targetLanguage: String = "auto",
        detectSpeakers: Boolean = true,
        includeTimestamps: Boolean = true
    ): Result<TranscriptResult> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please add your key in the Secrets panel.")
            )
        }

        val prompt = buildString {
            append("You are an expert audio transcription system. Given this audio context or media caption: \"$inputContext\".\n")
            if (targetLanguage != "auto") {
                append("The language is: $targetLanguage.\n")
            } else {
                append("Detect the spoken language automatically.\n")
            }
            if (detectSpeakers) {
                append("Identify distinct speakers (e.g. Speaker 1, Speaker 2).\n")
            }
            append("Format your response STRICTLY as a JSON object with this structure:\n")
            append("{\n")
            append("  \"language\": \"English\",\n")
            append("  \"cleanScript\": \"The clean transcript without timestamps or speaker names.\",\n")
            append("  \"speakers\": [\"Speaker 1\"],\n")
            append("  \"segments\": [\n")
            append("    {\"start\": \"00:00\", \"end\": \"00:05\", \"speaker\": \"Speaker 1\", \"text\": \"First phrase.\"}\n")
            append("  ]\n")
            append("}")
        }

        val requestJson = JSONObject().apply {
            val contentsArray = org.json.JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = org.json.JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)

            val configObj = JSONObject().apply {
                put("responseMimeType", "application/json")
            }
            put("generationConfig", configObj)
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val body = requestJson.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder().url(url).post(body).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errStr = response.body?.string().orEmpty()
                    return@withContext Result.failure(Exception("Gemini API error (HTTP ${response.code}): $errStr"))
                }

                val resBody = response.body?.string().orEmpty()
                val jsonRoot = JSONObject(resBody)
                val candidates = jsonRoot.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
                    val textOutput = parts?.optJSONObject(0)?.optString("text").orEmpty()

                    val parsed = JSONObject(textOutput)
                    val lang = parsed.optString("language", "Auto-detected")
                    val cleanScript = parsed.optString("cleanScript", "")

                    val speakersList = mutableListOf<String>()
                    val spkArray = parsed.optJSONArray("speakers")
                    if (spkArray != null) {
                        for (i in 0 until spkArray.length()) {
                            speakersList.add(spkArray.optString(i))
                        }
                    }

                    val segmentsList = mutableListOf<TranscriptSegment>()
                    val segArray = parsed.optJSONArray("segments")
                    if (segArray != null) {
                        for (i in 0 until segArray.length()) {
                            val segObj = segArray.getJSONObject(i)
                            segmentsList.add(
                                TranscriptSegment(
                                    id = i + 1,
                                    start = segObj.optString("start", "00:00"),
                                    end = segObj.optString("end", "00:05"),
                                    speaker = segObj.optString("speaker", "Speaker"),
                                    text = segObj.optString("text", "")
                                )
                            )
                        }
                    }

                    val fullTranscript = if (segmentsList.isNotEmpty()) {
                        segmentsList.joinToString("\n") { s ->
                            "[${s.start} - ${s.end}] ${if (s.speaker.isNotBlank()) "${s.speaker}: " else ""}${s.text}"
                        }
                    } else {
                        cleanScript
                    }

                    return@withContext Result.success(
                        TranscriptResult(
                            fullTranscript = fullTranscript,
                            cleanScript = cleanScript,
                            language = lang,
                            speakers = if (speakersList.isNotEmpty()) speakersList else listOf("Speaker 1"),
                            segments = segmentsList,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }

        Result.failure(Exception("Failed to generate transcript from AI engine."))
    }
}
