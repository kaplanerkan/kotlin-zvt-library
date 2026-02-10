package com.panda_erkan.zvtclientdemo.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.panda.zvt.simulator.SimulatorServer
import com.panda.zvt_library.model.ConnectionState
import com.panda_erkan.zvtclientdemo.R
import com.panda_erkan.zvtclientdemo.repository.LogEntry
import com.panda_erkan.zvtclientdemo.repository.ZvtRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(
    application: Application,
    private val repository: ZvtRepository
) : AndroidViewModel(application) {

    private val ctx get() = getApplication<Application>()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _logEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    val logEntries: StateFlow<List<LogEntry>> = _logEntries.asStateFlow()

    private val logBuffer = mutableListOf<LogEntry>()

    // Embedded simulator
    private var simulatorServer: SimulatorServer? = null

    private val _simulatorRunning = MutableStateFlow(false)
    val simulatorRunning: StateFlow<Boolean> = _simulatorRunning.asStateFlow()

    private val _simulatorStarting = MutableStateFlow(false)
    val simulatorStarting: StateFlow<Boolean> = _simulatorStarting.asStateFlow()

    init {
        repository.connectionState
            .onEach { state ->
                _connectionState.value = state
            }
            .launchIn(viewModelScope)

        repository.logMessages
            .onEach { entry ->
                logBuffer.add(entry)
                if (logBuffer.size > 500) logBuffer.removeAt(0)
                _logEntries.value = logBuffer.toList()
            }
            .launchIn(viewModelScope)
    }

    fun connectAndRegister(host: String, port: Int, configByte: Byte = 0x08, tlvEnabled: Boolean = false, keepAlive: Boolean = true) {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = ctx.getString(R.string.status_connecting, host, port)

            val result = repository.connectAndRegister(host, port, configByte, tlvEnabled, keepAlive)

            result.fold(
                onSuccess = { registered ->
                    _statusMessage.value = if (registered) {
                        ctx.getString(R.string.status_registered)
                    } else {
                        ctx.getString(R.string.status_connect_failed)
                    }
                },
                onFailure = { error ->
                    _statusMessage.value = ctx.getString(R.string.status_error, error.message)
                }
            )
            _isLoading.value = false
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            repository.disconnect()
            _statusMessage.value = ctx.getString(R.string.status_disconnected)
        }
    }

    fun clearLogs() {
        logBuffer.clear()
        _logEntries.value = emptyList()
    }

    fun startSimulator() {
        viewModelScope.launch {
            _simulatorStarting.value = true
            try {
                withContext(Dispatchers.IO) {
                    val server = SimulatorServer()
                    server.start()
                    simulatorServer = server
                }
                _simulatorRunning.value = true
            } catch (e: Exception) {
                _statusMessage.value = ctx.getString(R.string.status_error, e.message)
            }
            _simulatorStarting.value = false
        }
    }

    fun stopSimulator() {
        viewModelScope.launch {
            _simulatorStarting.value = true
            try {
                withContext(Dispatchers.IO) {
                    simulatorServer?.stop()
                    simulatorServer = null
                }
                _simulatorRunning.value = false
            } catch (e: Exception) {
                _statusMessage.value = ctx.getString(R.string.status_error, e.message)
            }
            _simulatorStarting.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        simulatorServer?.stop()
        simulatorServer = null
    }
}
