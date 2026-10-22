package com.example.data.remote

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.local.TypingTestQuestion
import com.example.data.local.VocabularyWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models"

    private fun getApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }

    /**
     * General multi-turn conversational chat using gemini-3.8-flash
     */
    suspend fun sendChatMessage(
        history: List<Pair<String, String>>, // role ("user" or "model") to text
        message: String,
        personaInstruction: String = "You are Topia AI, a warm, encouraging, and expert English tutor. You help learners practice natural English conversation, correct their grammar gently, explain idiomatic phrases, and converse dynamically. Keep responses engaging and under 4 sentences unless asked for an in-depth lesson."
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            val url = "$baseUrl/gemini-3.8-flash:generateContent?key=$apiKey"

            val contentsArray = JSONArray()

            // History
            for ((role, text) in history) {
                val contentObj = JSONObject()
                contentObj.put("role", if (role == "user") "user" else "model")
                val partsArray = JSONArray()
                val partObj = JSONObject()
                partObj.put("text", text)
                partsArray.put(partObj)
                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
            }

            // Current message
            val currentObj = JSONObject()
            currentObj.put("role", "user")
            val currentParts = JSONArray()
            val currentPart = JSONObject()
            currentPart.put("text", message)
            currentParts.put(currentPart)
            currentObj.put("parts", currentParts)
            contentsArray.put(currentObj)

            val rootJson = JSONObject()
            rootJson.put("contents", contentsArray)

            // System instruction
            val sysInstructionObj = JSONObject()
            val sysParts = JSONArray()
            val sysPart = JSONObject()
            sysPart.put("text", personaInstruction)
            sysParts.put(sysPart)
            sysInstructionObj.put("parts", sysParts)
            rootJson.put("systemInstruction", sysInstructionObj)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = rootJson.toString().toRequestBody(mediaType)
            val request = Request.Builder().url(url).post(body).build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Gemini API Error: ${response.code} $responseBody"))
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text")

            if (text != null && text.isNotEmpty()) {
                Result.success(text)
            } else {
                Result.failure(Exception("No response received from Gemini"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * High Thinking Deep Grammar & Sentence Analysis using gemini-4-argon with thinkingLevel: HIGH
     */
    suspend fun analyzeWithHighThinking(
        sentenceOrQuery: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            val url = "$baseUrl/gemini-4-argon:generateContent?key=$apiKey"

            val prompt = """
                Perform an in-depth linguistic, grammatical, and stylistic breakdown of the following English input:
                \"$sentenceOrQuery\"
                
                Provide:
                1. Accurate Korean translation (자연스러운 한국어 번역)
                2. Key grammatical structure breakdown (문법 핵심 분석: tense, clauses, modifiers)
                3. Essential vocabulary & collocation nuances (핵심 어휘 및 뉘앙스)
                4. Two alternative natural native expressions (원어민 추천 표현 2가지)
            """.trimIndent()

            val rootJson = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            rootJson.put("contents", contentsArray)

            // Thinking config
            val genConfig = JSONObject()
            val thinkingConfig = JSONObject()
            thinkingConfig.put("thinkingLevel", "HIGH")
            genConfig.put("thinkingConfig", thinkingConfig)
            rootJson.put("generationConfig", genConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = rootJson.toString().toRequestBody(mediaType)
            val request = Request.Builder().url(url).post(body).build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                // Fallback to gemini-3.8-flash if model preview is unavailable
                return@withContext sendSinglePrompt("gemini-3.8-flash", prompt)
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            var text = ""
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val p = parts.optJSONObject(i)
                    if (p != null && p.has("text")) {
                        text += p.optString("text")
                    }
                }
            }

            if (text.isNotEmpty()) {
                Result.success(text)
            } else {
                Result.failure(Exception("No analysis generated"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate English typing test from camera photo / textbook image
     * Creates fill-in-the-blank questions where target words are removed and NO HINTS are given.
     */
    suspend fun generateTestFromImage(
        bitmap: Bitmap
    ): Result<List<TypingTestQuestion>> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            val url = "$baseUrl/gemini-3.8-flash:generateContent?key=$apiKey"

            // Convert bitmap to base64 jpeg
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            val prompt = """
                You are an English test generator for Topia AI.
                Analyze the English text in this image (e.g. workbook, textbook page, article).
                Extract 4 to 6 important vocabulary words and create a strict TYPING test without hints.
                For each item, produce:
                - sentenceWithBlank: The sentence where the target word is replaced exactly with "____" (blank). Do NOT give first letters or hints.
                - targetWord: The exact lowercase English word that fits into the blank.
                - koreanMeaning: Natural Korean meaning of the target word.
                - englishDefinition: Clear English definition of the target word.
                - explanation: Brief explanation of why this word fits in context.

                Return strictly JSON formatted as a JSON array:
                [
                  {
                    "id": 1,
                    "sentenceWithBlank": "The scientist conducted a ____ investigation to ensure accurate results.",
                    "targetWord": "meticulous",
                    "koreanMeaning": "꼼꼼한, 세심한",
                    "englishDefinition": "showing great attention to detail; very careful and precise",
                    "explanation": "Meticulous is used here to describe the thoroughness of the investigation."
                  }
                ]
            """.trimIndent()

            val rootJson = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()

            // Text part
            val textPart = JSONObject()
            textPart.put("text", prompt)
            partsArray.put(textPart)

            // Image part
            val imagePart = JSONObject()
            val inlineData = JSONObject()
            inlineData.put("mimeType", "image/jpeg")
            inlineData.put("data", base64Image)
            imagePart.put("inlineData", inlineData)
            partsArray.put(imagePart)

            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            rootJson.put("contents", contentsArray)

            // Generation config JSON
            val genConfig = JSONObject()
            genConfig.put("responseMimeType", "application/json")
            rootJson.put("generationConfig", genConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = rootJson.toString().toRequestBody(mediaType)
            val request = Request.Builder().url(url).post(body).build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Image Test API Error: ${response.code} $responseBody"))
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val rawJson = parts?.optJSONObject(0)?.optString("text") ?: "[]"

            val questions = parseQuestionsFromJson(rawJson)
            Result.success(questions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate test questions from topic or passage text
     */
    suspend fun generateTestFromText(
        passageText: String
    ): Result<List<TypingTestQuestion>> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            val url = "$baseUrl/gemini-3.8-flash:generateContent?key=$apiKey"

            val prompt = """
                You are Topia AI's English test creator.
                Based on this topic or text:
                "$passageText"

                Generate 4 to 6 English typing test questions where the learner must type the target word.
                DO NOT show hints or partial words. The sentence must contain "____".
                
                Return strictly JSON array:
                [
                  {
                    "id": 1,
                    "sentenceWithBlank": "A sentence with ____ for the target word.",
                    "targetWord": "target",
                    "koreanMeaning": "목표, 대상",
                    "englishDefinition": "a person, object, or place selected as the aim of an attack or action",
                    "explanation": "Context explanation"
                  }
                ]
            """.trimIndent()

            val rootJson = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val textPart = JSONObject()
            textPart.put("text", prompt)
            partsArray.put(textPart)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            rootJson.put("contents", contentsArray)

            val genConfig = JSONObject()
            genConfig.put("responseMimeType", "application/json")
            rootJson.put("generationConfig", genConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = rootJson.toString().toRequestBody(mediaType)
            val request = Request.Builder().url(url).post(body).build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Test generation error: ${response.code}"))
            }

            val jsonResponse = JSONObject(responseBody)
            val rawJson = jsonResponse.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: "[]"

            val questions = parseQuestionsFromJson(rawJson)
            Result.success(questions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * AI Vocabulary word definition generator (fetches Korean meaning, English definition, and example sentence)
     */
    suspend fun lookupWordDetails(
        word: String
    ): Result<VocabularyWord> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            val url = "$baseUrl/gemini-3.1-flash-lite-preview:generateContent?key=$apiKey"

            val prompt = """
                Define the English word "$word" for an English learner's vocabulary notebook.
                Return strictly JSON:
                {
                  "word": "$word",
                  "koreanMeaning": "한국어 대표 뜻 (1~2개)",
                  "englishDefinition": "Concise English definition in simple learner-friendly terms",
                  "exampleSentence": "A practical, modern example sentence demonstrating the word."
                }
            """.trimIndent()

            val rootJson = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val textPart = JSONObject()
            textPart.put("text", prompt)
            partsArray.put(textPart)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            rootJson.put("contents", contentsArray)

            val genConfig = JSONObject()
            genConfig.put("responseMimeType", "application/json")
            rootJson.put("generationConfig", genConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = rootJson.toString().toRequestBody(mediaType)
            val request = Request.Builder().url(url).post(body).build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Word lookup failed: ${response.code}"))
            }

            val jsonResponse = JSONObject(responseBody)
            val rawJson = jsonResponse.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: "{}"

            val obj = JSONObject(rawJson)
            val vocaWord = VocabularyWord(
                word = obj.optString("word", word).trim(),
                koreanMeaning = obj.optString("koreanMeaning", "").trim(),
                englishDefinition = obj.optString("englishDefinition", "").trim(),
                exampleSentence = obj.optString("exampleSentence", "").trim(),
                masteryLevel = 0,
                reviewCount = 0
            )
            Result.success(vocaWord)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun sendSinglePrompt(model: String, prompt: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            val url = "$baseUrl/$model:generateContent?key=$apiKey"

            val rootJson = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val textPart = JSONObject()
            textPart.put("text", prompt)
            partsArray.put(textPart)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            rootJson.put("contents", contentsArray)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = rootJson.toString().toRequestBody(mediaType)
            val request = Request.Builder().url(url).post(body).build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("API Error: ${response.code}"))
            }

            val jsonResponse = JSONObject(responseBody)
            val text = jsonResponse.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: ""

            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseQuestionsFromJson(rawJson: String): List<TypingTestQuestion> {
        val list = mutableListOf<TypingTestQuestion>()
        try {
            val trimmed = rawJson.trim()
            val jsonArray = if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                val start = trimmed.indexOf('[')
                val end = trimmed.lastIndexOf(']')
                if (start != -1 && end != -1) {
                    JSONArray(trimmed.substring(start, end + 1))
                } else {
                    JSONArray()
                }
            }

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                val question = TypingTestQuestion(
                    id = obj.optInt("id", i + 1),
                    sentenceWithBlank = obj.optString("sentenceWithBlank", ""),
                    targetWord = obj.optString("targetWord", "").trim(),
                    koreanMeaning = obj.optString("koreanMeaning", "").trim(),
                    englishDefinition = obj.optString("englishDefinition", "").trim(),
                    explanation = obj.optString("explanation", "")
                )
                if (question.sentenceWithBlank.isNotEmpty() && question.targetWord.isNotEmpty()) {
                    list.add(question)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
