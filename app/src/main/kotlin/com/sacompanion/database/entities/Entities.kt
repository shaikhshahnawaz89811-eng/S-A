package com.sacompanion.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userMessage: String,
    val assistantResponse: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sessionId: String = ""
)

@Entity(
    tableName = "memories",
    indices = [Index(value = ["key"], unique = true)]
)
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val value: String,
    val category: String = "general",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "user_preferences",
    indices = [Index(value = ["key"], unique = true)]
)
data class UserPreferenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val value: String
)

@Entity(tableName = "family_profiles")
data class FamilyProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val relation: String,       // "owner", "family", "guest"
    val accessLevel: String,    // "full", "limited", "basic"
    val voicePrint: String = "", // reserved for future speaker ID
    val notes: String = "",
    val isOwner: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "command_history")
data class CommandHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val command: String,
    val result: String,
    val success: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
