package com.laxiang.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    @Query("SELECT * FROM records WHERE deleted = 0 ORDER BY occurredAt DESC")
    fun observeVisibleRecords(): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records ORDER BY updatedAt DESC")
    suspend fun getAllRecords(): List<RecordEntity>

    @Query("SELECT * FROM records WHERE id = :id LIMIT 1")
    suspend fun getRecord(id: String): RecordEntity?

    @Query("DELETE FROM records")
    suspend fun deleteAllRecords()

    @Upsert
    suspend fun upsertRecord(record: RecordEntity)

    @Upsert
    suspend fun upsertRecords(records: List<RecordEntity>)
}
