package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TestDao {
    @Query("SELECT * FROM test_records ORDER BY timestamp DESC")
    fun getAllTestRecords(): Flow<List<TestRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTestRecord(record: TestRecord): Long

    @Query("DELETE FROM test_records WHERE id = :id")
    suspend fun deleteTestRecord(id: Long)
}
