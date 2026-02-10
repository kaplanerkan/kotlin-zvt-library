package com.panda_erkan.zvtclientdemo.ui.journals

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.panda_erkan.zvtclientdemo.data.entity.JournalEntry
import com.panda_erkan.zvtclientdemo.data.model.OperationType
import com.panda_erkan.zvtclientdemo.repository.JournalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class JournalsViewModel(
    application: Application,
    private val journalRepository: JournalRepository
) : AndroidViewModel(application) {

    private val _selectedFilter = MutableStateFlow<OperationType?>(null)
    val selectedFilter: StateFlow<OperationType?> = _selectedFilter.asStateFlow()

    val entries: StateFlow<List<JournalEntry>> = _selectedFilter
        .flatMapLatest { filter ->
            if (filter == null) {
                journalRepository.getAllEntries()
            } else {
                journalRepository.getEntriesByType(filter)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedEntry = MutableStateFlow<JournalEntry?>(null)
    val selectedEntry: StateFlow<JournalEntry?> = _selectedEntry.asStateFlow()

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
