package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [VocabularyWord::class, TestRecord::class],
    version = 1,
    exportSchema = false
)
abstract class TopiaDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun testDao(): TestDao

    companion object {
        @Volatile
        private var INSTANCE: TopiaDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): TopiaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TopiaDatabase::class.java,
                    "topia_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialWords(database.wordDao())
                    }
                }
            }

            private suspend fun populateInitialWords(wordDao: WordDao) {
                val starterWords = listOf(
                    VocabularyWord(
                        word = "resilient",
                        koreanMeaning = "회복력 있는, 탄력 있는",
                        englishDefinition = "able to withstand or recover quickly from difficult conditions",
                        exampleSentence = "She is resilient and always bounces back after a setback.",
                        masteryLevel = 1,
                        reviewCount = 1
                    ),
                    VocabularyWord(
                        word = "meticulous",
                        koreanMeaning = "꼼꼼한, 세심한",
                        englishDefinition = "showing great attention to detail; very careful and precise",
                        exampleSentence = "He kept meticulous records of all experimental transactions.",
                        masteryLevel = 0,
                        reviewCount = 0
                    ),
                    VocabularyWord(
                        word = "pragmatic",
                        koreanMeaning = "실용적인, 실무적인",
                        englishDefinition = "dealing with things sensibly and realistically in a practical way",
                        exampleSentence = "We need a pragmatic approach to solving the issue.",
                        masteryLevel = 0,
                        reviewCount = 0
                    ),
                    VocabularyWord(
                        word = "ephemeral",
                        koreanMeaning = "덧없는, 순식간의",
                        englishDefinition = "lasting for a very short time; fleeting",
                        exampleSentence = "Fame in the digital age can be quite ephemeral.",
                        masteryLevel = 0,
                        reviewCount = 0
                    ),
                    VocabularyWord(
                        word = "ubiquitous",
                        koreanMeaning = "어디에나 존재하는, 도처에 있는",
                        englishDefinition = "present, appearing, or found everywhere simultaneously",
                        exampleSentence = "Smartphones have become ubiquitous across all age groups.",
                        masteryLevel = 2,
                        reviewCount = 2
                    ),
                    VocabularyWord(
                        word = "articulate",
                        koreanMeaning = "명확하게 표현하다, 조리 있는",
                        englishDefinition = "having or showing the ability to speak fluently and coherently",
                        exampleSentence = "She gave a very articulate presentation to the international committee.",
                        masteryLevel = 1,
                        reviewCount = 1
                    )
                )
                wordDao.insertAll(starterWords)
            }
        }
    }
}
