package com.sacompanion.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sacompanion.database.dao.*
import com.sacompanion.database.entities.*

@Database(
    entities = [
        ConversationEntity::class,
        MemoryEntity::class,
        UserPreferenceEntity::class,
        FamilyProfileEntity::class,
        CommandHistoryEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class SADatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao
    abstract fun memoryDao(): MemoryDao
    abstract fun preferenceDao(): PreferenceDao
    abstract fun familyProfileDao(): FamilyProfileDao
    abstract fun commandHistoryDao(): CommandHistoryDao

    companion object {
        @Volatile
        private var instance: SADatabase? = null

        fun getInstance(context: Context): SADatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SADatabase::class.java,
                    "sa_companion.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
