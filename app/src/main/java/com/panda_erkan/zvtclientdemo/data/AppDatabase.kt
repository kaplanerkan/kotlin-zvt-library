package com.panda_erkan.zvtclientdemo.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.panda_erkan.zvtclientdemo.data.converter.Converters
import com.panda_erkan.zvtclientdemo.data.dao.JournalDao
import com.panda_erkan.zvtclientdemo.data.entity.JournalEntry

@Database(entities = [JournalEntry::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao
}
