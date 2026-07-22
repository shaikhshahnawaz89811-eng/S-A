package com.sacompanion.database.dao

import androidx.room.*
import com.sacompanion.database.entities.MemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MemoryEntity)

    @Query("SELECT * FROM memories WHERE `key` = :key LIMIT 1")
    suspend fun getByKey(key: String): MemoryEntity?

    @Query("SELECT * FROM memories WHERE category = :category ORDER BY timestamp DESC")
    suspend fun getByCategory(category: String): List<MemoryEntity>

    @Query("SELECT * FROM memories ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories ORDER BY timestamp DESC")
    suspend fun getAll(): List<MemoryEntity>

    @Query("DELETE FROM memories WHERE `key` = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM memories")
    suspend fun deleteAll()
}
