package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vocabulary_words")
data class VocabularyWord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val word: String,
    val koreanMeaning: String,
    val englishDefinition: String,
    val exampleSentence: String = "",
    val masteryLevel: Int = 0, // 0: Learning, 1: Familiar, 2: Mastered
    val reviewCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
