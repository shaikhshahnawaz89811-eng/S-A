package com.sacompanion.database.dao

import androidx.room.*
import com.sacompanion.database.entities.FamilyProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FamilyProfileEntity)

    @Query("SELECT * FROM family_profiles WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): FamilyProfileEntity?

    @Query("SELECT * FROM family_profiles WHERE isOwner = 1 LIMIT 1")
    suspend fun getOwner(): FamilyProfileEntity?

    @Query("SELECT * FROM family_profiles ORDER BY isOwner DESC, name ASC")
    fun getAllFlow(): Flow<List<FamilyProfileEntity>>

    @Query("SELECT * FROM family_profiles ORDER BY isOwner DESC, name ASC")
    suspend fun getAll(): List<FamilyProfileEntity>

    @Query("DELETE FROM family_profiles WHERE id = :id")
    suspend fun deleteById(id: Long)
}
