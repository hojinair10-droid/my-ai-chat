package com.example.zetachat.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CharacterDao {
    @Insert
    suspend fun insert(character: CharacterEntity): Long

    @Query("SELECT * FROM characters")
    suspend fun getAll(): List<CharacterEntity>

    @Query("SELECT * FROM characters WHERE id = :id")
    suspend fun getById(id: Long): CharacterEntity?

    @Query("UPDATE characters SET nativeIndex = :nativeIndex WHERE id = :id")
    suspend fun updateNativeIndex(id: Long, nativeIndex: Int)
}