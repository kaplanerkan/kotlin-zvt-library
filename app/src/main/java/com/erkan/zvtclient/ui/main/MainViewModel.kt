package com.erkan.zvtclient.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erkan.zvt.model.ConnectionState
import com.erkan.zvtclient.repository.LogEntry
import com.erkan.zvtclient.repository.ZvtRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: ZvtRepository
) : ViewModel() {

    // Bağlantı durumu
    private val _connectionState = MutableLiveData(ConnectionState.DISCONNECTED)
    val connectionState: LiveData<ConnectionState> = _connectionState

    // Durum mesajı
    private val _statusMessage = MutableLiveData("")
    val statusMessage: LiveData<String> = _statusMessage

    // Loading
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Log mesajları
    private val _logEntries = MutableLiveData<List<LogEntry>>(emptyList())
    val logEntries: LiveData<List<LogEntry>> = _logEntries

    private val logBuffer = mutableListOf<LogEntry>()

    init {
        // Bağlantı durumunu dinle
        repository.connectionState
            .onEach { state ->
                _connectionState.postValue(state)
            }
            .launchIn(viewModelScope)

        // Log mesajlarını dinle
        repository.logMessages
            .onEach { entry ->
                logBuffer.add(entry)
                if (logBuffer.size > 500) logBuffer.removeAt(0)
                _logEntries.postValue(logBuffer.toList())
            }
            .launchIn(viewModelScope)
    }

    fun connectAndRegister(host: String, port: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Bağlanıyor: $host:$port..."

            // Config'i güncelle (basit yaklaşım)
            val result = repository.connectAndRegister()

            result.fold(
                onSuccess = { registered ->
                    _statusMessage.value = if (registered) {
                        "Terminal kayıtlı ve hazır ✓"
                    } else {
                        "Bağlandı ama kayıt başarısız"
                    }
                },
                onFailure = { error ->
                    _statusMessage.value = "Hata: ${error.message}"
                }
            )
            _isLoading.value = false
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            repository.disconnect()
            _statusMessage.value = "Bağlantı kesildi"
        }
    }

    fun clearLogs() {
        logBuffer.clear()
        _logEntries.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        repository.destroy()
    }
}
