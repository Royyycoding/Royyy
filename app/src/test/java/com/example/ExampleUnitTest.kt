package com.example

import com.example.data.local.TypingTestQuestion
import com.example.data.local.VocabularyWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testTypingQuestionEvaluation() {
        val question = TypingTestQuestion(
            id = 1,
            sentenceWithBlank = "The scientist conducted a ____ investigation.",
            targetWord = "meticulous",
            koreanMeaning = "꼼꼼한",
            englishDefinition = "very careful and precise"
        )

        // Strict typing evaluation without hints
        val correctAttempt = "Meticulous ".trim().lowercase()
        val incorrectAttempt = "careful"

        assertTrue(correctAttempt == question.targetWord.lowercase())
        assertFalse(incorrectAttempt == question.targetWord.lowercase())
    }

    @Test
    fun testVocabularyWordCreation() {
        val word = VocabularyWord(
            word = "resilient",
            koreanMeaning = "회복력 있는",
            englishDefinition = "able to recover quickly"
        )
        assertEquals("resilient", word.word)
        assertEquals(0, word.masteryLevel)
    }
}
