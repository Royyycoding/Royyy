package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "test_records")
data class TestRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val totalQuestions: Int,
    val correctCount: Int,
    val scorePercentage: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class TypingTestQuestion(
    val id: Int,
    val sentenceWithBlank: String,
    val targetWord: String,
    val koreanMeaning: String,
    val englishDefinition: String,
    val explanation: String = "",
    var userAnswer: String = "",
    var isSubmitted: Boolean = false,
    var isCorrect: Boolean = false
)
