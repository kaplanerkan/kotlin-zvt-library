package com.panda_erkan.zvtclientdemo.ui.journals

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.panda_erkan.zvtclientdemo.data.entity.JournalEntry
import com.panda_erkan.zvtclientdemo.data.model.OperationType
import com.panda_erkan.zvtclientdemo.repository.JournalRepository
import kotlinx.coroutines.launch

class JournalsViewModel(
    application: Application,
    private val journalRepository: JournalRepository
) : AndroidViewModel(application) {

    private val _selectedFilter = MutableLiveData<OperationType?>(null)
    val selectedFilter: LiveData<OperationType?> = _selectedFilter

    val entries: LiveData<List<JournalEntry>> = _selectedFilter.switchMap { filter ->
        if (filter == null) {
            journalRepository.getAllEntries().asLiveData()
        } else {
            journalRepository.getEntriesByType(filter).asLiveData()
        }
    }

    private val _selectedEntry = MutableLiveData<JournalEntry?>()
    val selectedEntry: LiveData<JournalEntry?> = _selectedEntry

    fun setFilter(type: OperationType?) {
        _selectedFilter.value = type
    }

    fun selectEntry(entry: JournalEntry) {
        _selectedEntry.value = entry
    }

    fun clearSelectedEntry() {
        _selectedEntry.value = null
    }

    fun clearAllEntries() {
        viewModelScope.launch {
            journalRepository.clearAll()
        }
    }
}
