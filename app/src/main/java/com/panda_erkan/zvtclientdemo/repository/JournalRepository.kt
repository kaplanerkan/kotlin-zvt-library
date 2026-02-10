package com.panda_erkan.zvtclientdemo.repository

import com.panda_erkan.zvtclientdemo.data.dao.JournalDao
import com.panda_erkan.zvtclientdemo.data.entity.JournalEntry
import com.panda_erkan.zvtclientdemo.data.model.OperationType
import kotlinx.coroutines.flow.Flow

class JournalRepository(private val journalDao: JournalDao) {

    suspend fun saveEntry(entry: JournalEntry): Long =
        journalDao.insert(entry)

    fun getAllEntries(): Flow<List<JournalEntry>> =
        journalDao.getAllEntries()

    fun getEntriesByType(type: OperationType): Flow<List<JournalEntry>> =
        journalDao.getEntriesByType(type)

    suspend fun getEntryById(id: Long): JournalEntry? =
        journalDao.getEntryById(id)

    suspend fun clearAll() =
        journalDao.deleteAll()
}
