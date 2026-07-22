package com.sacompanion.core.ai

import com.sacompanion.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import com.google.gson.annotations.SerializedName
import java.util.concurrent.TimeUnit

// ── Data Models ──────────────────────────────────────────────────────────────

data class ChatMessage(
    @SerializedName("role") val role: String,       // "system" | "user" | "assistant"
    @SerializedName("content") val content: String
)

data class ChatRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<ChatMessage>,
    @SerializedName("temperature") val temperature: Double = 0.7,
    @SerializedName("max_tokens") val maxTokens: Int = 1024,
    @SerializedName("stream") val stream: Boolean = false
)

data class ChatResponse(
    @SerializedName("id") val id: String,
    @SerializedName("choices") val choices: List<Choice>,
    @SerializedName("usage") val usage: Usage?
)

data class Choice(
    @SerializedName("message") val message: ChatMessage,
    @SerializedName("finish_reason") val finishReason: String?
)

data class Usage(
    @SerializedName("prompt_tokens") val promptTokens: Int,
    @SerializedName("completion_tokens") val completionTokens: Int,
    @SerializedName("total_tokens") val totalTokens: Int
)

// ── Retrofit Interface ────────────────────────────────────────────────────────

interface GroqApiService {
    @POST("chat/completions")
    suspend fun chat(
        @Header("Authorization") authorization: String,
        @Body request: ChatRequest
    ): ChatResponse
}

// ── Groq AI Client ────────────────────────────────────────────────────────────

class GroqAIClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                else HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.GROQ_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(GroqApiService::class.java)

    private val systemPrompt = """
        Aap SA hain — ek futuristic personal AI voice assistant. 
        Aap ek smart operating system assistant ki tarah kaam karte hain, na ki ek normal chatbot.
        
        Core traits:
        - Female voice personality, soft aur natural
        - Hindi/Hinglish mein naturally baat karo jab user Hindi/Hinglish use kare
        - English mein baat karo jab user English use kare
        - Concise responses do — voice ke liye suited
        - Owner ko "Boss" ya unke naam se bulaao
        - Personal aur warm behaviour raho
        - Commands ko samjho aur execute karo
        
        Special commands handling:
        - Agar user music, battery, volume, brightness, torch, camera, screenshot, app open jaisi commands de — unhe pehchan lo aur structured response do
        - Multiple commands ek saath handle karo
        - Memory se personal information use karo
        
        Response format:
        - Voice ke liye short aur clear
        - Action commands ke liye JSON-like structure: [ACTION: battery_check], [ACTION: play_music], etc.
        - Phir natural language response
        
        IMPORTANT: Sirf saved information use karo. Kabhi bhi information invent mat karo.
    """.trimIndent()

    private val conversationHistory = mutableListOf<ChatMessage>()

    suspend fun chat(userMessage: String, apiKey: String = BuildConfig.GROQ_API_KEY): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val userMsg = ChatMessage(role = "user", content = userMessage)
                conversationHistory.add(userMsg)

                // Keep history limited to last 20 messages
                val messages = buildList {
                    add(ChatMessage(role = "system", content = systemPrompt))
                    addAll(conversationHistory.takeLast(20))
                }

                val request = ChatRequest(
                    model = BuildConfig.GROQ_MODEL,
                    messages = messages,
                    temperature = 0.7,
                    maxTokens = 512
                )

                val response = service.chat(
                    authorization = "Bearer $apiKey",
                    request = request
                )

                val assistantMessage = response.choices.firstOrNull()?.message?.content
                    ?: "Maafi chahta hoon, koi response nahi mila."

                conversationHistory.add(ChatMessage(role = "assistant", content = assistantMessage))
                Result.success(assistantMessage)

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun clearHistory() {
        conversationHistory.clear()
    }

    fun getConversationHistory(): List<ChatMessage> = conversationHistory.toList()
}

// ── Command Extractor ─────────────────────────────────────────────────────────

object CommandExtractor {
    private val actionPattern = Regex("\\[ACTION:\\s*([a-z_]+)(?::([^\\]]+))?\\]")

    data class ExtractedCommand(val action: String, val parameter: String?)

    fun extract(response: String): List<ExtractedCommand> {
        return actionPattern.findAll(response).map { match ->
            ExtractedCommand(
                action = match.groupValues[1].trim(),
                parameter = match.groupValues[2].trim().takeIf { it.isNotEmpty() }
            )
        }.toList()
    }

    fun stripActions(response: String): String {
        return actionPattern.replace(response, "").trim()
    }
}
