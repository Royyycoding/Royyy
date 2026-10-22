package com.example.data.repository

import android.graphics.Bitmap
import com.example.data.local.TestDao
import com.example.data.local.TestRecord
import com.example.data.local.TypingTestQuestion
import com.example.data.local.VocabularyWord
import com.example.data.local.WordDao
import com.example.data.remote.GeminiService
import kotlinx.coroutines.flow.Flow

class TopiaRepository(
    private val wordDao: WordDao,
    private val testDao: TestDao,
    private val geminiService: GeminiService
) {
    // Voca Book operations
    val allWords: Flow<List<VocabularyWord>> = wordDao.getAllWords()
    val reviewWords: Flow<List<VocabularyWord>> = wordDao.getReviewWords()
    val masteredWords: Flow<List<VocabularyWord>> = wordDao.getMasteredWords()

    suspend fun saveWord(word: VocabularyWord): Long = wordDao.insert(word)

    suspend fun saveWords(words: List<VocabularyWord>) = wordDao.insertAll(words)

    suspend fun updateWord(word: VocabularyWord) = wordDao.update(word)

    suspend fun deleteWord(word: VocabularyWord) = wordDao.delete(word)

    suspend fun deleteWordById(id: Long) = wordDao.deleteById(id)

    suspend fun updateMastery(id: Long, level: Int) = wordDao.updateMastery(id, level)

    // Test operations
    val allTestRecords: Flow<List<TestRecord>> = testDao.getAllTestRecords()

    suspend fun saveTestRecord(record: TestRecord): Long = testDao.insertTestRecord(record)

    suspend fun deleteTestRecord(id: Long) = testDao.deleteTestRecord(id)

    // Gemini Remote operations
    suspend fun sendChatMessage(
        history: List<Pair<String, String>>,
        message: String,
        persona: String
    ): Result<String> = geminiService.sendChatMessage(history, message, persona)

    suspend fun analyzeWithHighThinking(input: String): Result<String> =
        geminiService.analyzeWithHighThinking(input)

    suspend fun generateTestFromImage(bitmap: Bitmap): Result<List<TypingTestQuestion>> =
        geminiService.generateTestFromImage(bitmap)

    suspend fun generateTestFromText(text: String): Result<List<TypingTestQuestion>> =
        geminiService.generateTestFromText(text)

    suspend fun lookupWord(word: String): Result<VocabularyWord> =
        geminiService.lookupWordDetails(word)
}
