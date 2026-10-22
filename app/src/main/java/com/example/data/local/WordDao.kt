package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM vocabulary_words ORDER BY createdAt DESC")
    fun getAllWords(): Flow<List<VocabularyWord>>

    @Query("SELECT * FROM vocabulary_words WHERE masteryLevel < 2 ORDER BY reviewCount ASC, createdAt DESC")
    fun getReviewWords(): Flow<List<VocabularyWord>>

    @Query("SELECT * FROM vocabulary_words WHERE masteryLevel = 2 ORDER BY createdAt DESC")
    fun getMasteredWords(): Flow<List<VocabularyWord>>

    @Query("SELECT * FROM vocabulary_words WHERE word = :word LIMIT 1")
    suspend fun findByWord(word: String): VocabularyWord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(word: VocabularyWord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<VocabularyWord>)

    @Update
    suspend fun update(word: VocabularyWord)

    @Delete
    suspend fun delete(word: VocabularyWord)

    @Query("DELETE FROM vocabulary_words WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE vocabulary_words SET masteryLevel = :masteryLevel, reviewCount = reviewCount + 1 WHERE id = :id")
    suspend fun updateMastery(id: Long, masteryLevel: Int)
}
