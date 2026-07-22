package com.sacompanion.database.dao

import androidx.room.*
import com.sacompanion.database.entities.UserPreferenceEntity

@Dao
interface PreferenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserPreferenceEntity)

    @Query("SELECT * FROM user_preferences WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): UserPreferenceEntity?

    @Query("SELECT * FROM user_preferences")
    suspend fun getAll(): List<UserPreferenceEntity>

    @Query("DELETE FROM user_preferences WHERE `key` = :key")
    suspend fun deleteByKey(key: String)
}
