package com.sacompanion.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sacompanion.SAApplication
import com.sacompanion.core.ai.GroqAIClient
import com.sacompanion.core.memory.MemoryManager
import com.sacompanion.core.tts.TTSManager
import com.sacompanion.core.voice.VoiceManager
import com.sacompanion.core.voice.VoiceState
import com.sacompanion.database.entities.ConversationEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChatMessage(val text: String, val isUser: Boolean, val timestamp: Long = System.currentTimeMillis())
data class SAStatus(val label: String, val isOnline: Boolean = true)

data class MainUiState(
    val isServiceRunning: Boolean = false,
    val voiceState: VoiceState = VoiceState.IDLE,
    val saStatus: SAStatus = SAStatus("SA CORE ONLINE", true),
    val lastResponse: String = "",
    val chatMessages: List<ChatMessage> = emptyList(),
    val apiKey: String = "",
    val isTorchOn: Boolean = false,
    val isFloatingWindowShown: Boolean = false,
    val currentServiceState: String = "IDLE"
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SAApplication
    private val memoryManager: MemoryManager = app.memoryManager
    private val ttsManager: TTSManager = app.ttsManager
    private val groqAIClient: GroqAIClient = app.groqAIClient

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _conversationHistory = MutableStateFlow<List<ConversationEntity>>(emptyList())
    val conversationHistory: StateFlow<List<ConversationEntity>> = _conversationHistory.asStateFlow()

    init {
        loadConversationHistory()
        loadApiKey()
    }

    private fun loadConversationHistory() {
        viewModelScope.launch {
            memoryManager.getConversationsFlow(50).collect { conversations ->
                _conversationHistory.value = conversations

                val chatMessages = conversations.reversed().flatMap { conv ->
                    listOf(
                        ChatMessage(conv.userMessage, true, conv.timestamp),
                        ChatMessage(conv.assistantResponse, false, conv.timestamp + 1)
                    )
                }
                _uiState.update { it.copy(chatMessages = chatMessages) }
            }
        }
    }

    private fun loadApiKey() {
        viewModelScope.launch {
            val key = memoryManager.getPreference("groq_api_key", "")
            _uiState.update { it.copy(apiKey = key) }
        }
    }

    fun onServiceUpdate(state: String, response: String) {
        _uiState.update { it.copy(currentServiceState = state, lastResponse = response) }

        if (state == "RESPONSE" && response.isNotEmpty()) {
            val newMsg = ChatMessage(response, false)
            _uiState.update { current ->
                current.copy(chatMessages = current.chatMessages + newMsg)
            }
        }
    }

    fun sendTextMessage(message: String) {
        if (message.isBlank()) return

        val userMsg = ChatMessage(message, true)
        _uiState.update { current ->
            current.copy(chatMessages = current.chatMessages + userMsg)
        }

        viewModelScope.launch {
            _uiState.update { it.copy(currentServiceState = "PROCESSING") }
            val apiKey = _uiState.value.apiKey.ifEmpty {
                com.sacompanion.BuildConfig.GROQ_API_KEY
            }
            val context = memoryManager.buildContextString()
            val enriched = if (context.isNotEmpty()) "$message\n\n[Context]\n$context" else message
            val result = groqAIClient.chat(enriched, apiKey)

            result.fold(
                onSuccess = { response ->
                    memoryManager.saveConversation(message, response)
                    val saMsg = ChatMessage(response, false)
                    _uiState.update { current ->
                        current.copy(
                            chatMessages = current.chatMessages + saMsg,
                            currentServiceState = "IDLE"
                        )
                    }
                    ttsManager.speak(response)
                },
                onFailure = { error ->
                    val errMsg = "Sorry, AI se connect nahi ho pa raha: ${error.message}"
                    val saMsg = ChatMessage(errMsg, false)
                    _uiState.update { current ->
                        current.copy(
                            chatMessages = current.chatMessages + saMsg,
                            currentServiceState = "ERROR"
                        )
                    }
                }
            )
        }
    }

    fun saveApiKey(key: String) {
        memoryManager.savePreference("groq_api_key", key)
        _uiState.update { it.copy(apiKey = key) }
    }

    fun clearConversation() {
        viewModelScope.launch {
            memoryManager.clearConversations()
            groqAIClient.clearHistory()
            _uiState.update { it.copy(chatMessages = emptyList()) }
        }
    }

    fun saveMemory(key: String, value: String) {
        memoryManager.saveMemory(key, value)
    }

    fun setServiceRunning(running: Boolean) {
        _uiState.update { it.copy(isServiceRunning = running) }
    }

    fun speakText(text: String) {
        ttsManager.speak(text)
    }

    fun stopSpeaking() {
        ttsManager.stopSpeaking()
    }
}
