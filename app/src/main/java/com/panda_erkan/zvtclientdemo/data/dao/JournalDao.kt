package com.panda_erkan.zvtclientdemo.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.panda_erkan.zvtclientdemo.data.entity.JournalEntry
import com.panda_erkan.zvtclientdemo.data.model.OperationType
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {

    @Insert
    suspend fun insert(entry: JournalEntry): Long

    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE operationType = :type ORDER BY timestamp DESC")
    fun getEntriesByType(type: OperationType): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): JournalEntry?

    @Query("DELETE FROM journal_entries")
    suspend fun deleteAll()
}
