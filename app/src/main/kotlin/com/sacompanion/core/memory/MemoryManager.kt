package com.sacompanion.core.memory

import android.content.Context
import com.sacompanion.database.SADatabase
import com.sacompanion.database.entities.ConversationEntity
import com.sacompanion.database.entities.FamilyProfileEntity
import com.sacompanion.database.entities.MemoryEntity
import com.sacompanion.database.entities.UserPreferenceEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MemoryManager(
    private val context: Context,
    private val database: SADatabase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Conversation History ──────────────────────────────────────────────────

    fun saveConversation(userMessage: String, assistantResponse: String) {
        scope.launch {
            database.conversationDao().insert(
                ConversationEntity(
                    userMessage = userMessage,
                    assistantResponse = assistantResponse,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun getRecentConversations(limit: Int = 20): List<ConversationEntity> {
        return database.conversationDao().getRecent(limit)
    }

    fun getConversationsFlow(limit: Int = 50): Flow<List<ConversationEntity>> {
        return database.conversationDao().getRecentFlow(limit)
    }

    suspend fun clearConversations() {
        database.conversationDao().deleteAll()
    }

    // ── Memory Entries (Facts / Knowledge) ───────────────────────────────────

    fun saveMemory(key: String, value: String, category: String = "general") {
        scope.launch {
            database.memoryDao().upsert(
                MemoryEntity(
                    key = key,
                    value = value,
                    category = category,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun getMemory(key: String): String? {
        return database.memoryDao().getByKey(key)?.value
    }

    suspend fun getMemoriesByCategory(category: String): List<MemoryEntity> {
        return database.memoryDao().getByCategory(category)
    }

    fun getAllMemoriesFlow(): Flow<List<MemoryEntity>> {
        return database.memoryDao().getAllFlow()
    }

    suspend fun deleteMemory(key: String) {
        database.memoryDao().deleteByKey(key)
    }

    // ── User Preferences ─────────────────────────────────────────────────────

    fun savePreference(key: String, value: String) {
        scope.launch {
            database.preferenceDao().upsert(
                UserPreferenceEntity(key = key, value = value)
            )
        }
    }

    suspend fun getPreference(key: String, default: String = ""): String {
        return database.preferenceDao().get(key)?.value ?: default
    }

    // ── Family Profiles ───────────────────────────────────────────────────────

    fun saveFamilyProfile(profile: FamilyProfileEntity) {
        scope.launch {
            database.familyProfileDao().upsert(profile)
        }
    }

    suspend fun getFamilyProfile(name: String): FamilyProfileEntity? {
        return database.familyProfileDao().getByName(name)
    }

    fun getAllFamilyProfilesFlow(): Flow<List<FamilyProfileEntity>> {
        return database.familyProfileDao().getAllFlow()
    }

    suspend fun getFamilyProfiles(): List<FamilyProfileEntity> {
        return database.familyProfileDao().getAll()
    }

    suspend fun deleteFamilyProfile(id: Long) {
        database.familyProfileDao().deleteById(id)
    }

    // ── Context Builder for AI ────────────────────────────────────────────────

    suspend fun buildContextString(): String {
        val sb = StringBuilder()
        val preferences = database.preferenceDao().getAll()
        val memories = database.memoryDao().getAll()
        val familyProfiles = database.familyProfileDao().getAll()

        if (preferences.isNotEmpty()) {
            sb.appendLine("=== User Preferences ===")
            preferences.forEach { sb.appendLine("${it.key}: ${it.value}") }
        }

        if (memories.isNotEmpty()) {
            sb.appendLine("=== Known Facts ===")
            memories.forEach { sb.appendLine("${it.key}: ${it.value}") }
        }

        if (familyProfiles.isNotEmpty()) {
            sb.appendLine("=== Family Members ===")
            familyProfiles.forEach { profile ->
                sb.appendLine("${profile.name} (${profile.relation}) - Access: ${profile.accessLevel}")
                if (profile.notes.isNotEmpty()) sb.appendLine("  Notes: ${profile.notes}")
            }
        }

        return sb.toString()
    }

    // ── Security Log ──────────────────────────────────────────────────────────

    fun logSecurityEvent(event: String, severity: String = "INFO") {
        scope.launch {
            database.memoryDao().upsert(
                MemoryEntity(
                    key = "security_log_${System.currentTimeMillis()}",
                    value = "[$severity] $event",
                    category = "security",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
}
