package com.example.zetachat.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val appearance: String,
    val personality: String,
    val speakingStyle: String,
    val worldview: String,
    val scenarioPrompt: String,
    val isPublic: Boolean = false,
    val nativeIndex: Int = -1
)