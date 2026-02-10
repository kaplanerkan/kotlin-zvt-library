package com.panda_erkan.zvtclientdemo.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.panda.zvt.simulator.SimulatorServer
import com.panda.zvt_library.model.ConnectionState
import com.panda_erkan.zvtclientdemo.R
import com.panda_erkan.zvtclientdemo.repository.LogEntry
import com.panda_erkan.zvtclientdemo.repository.ZvtRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(
    application: Application,
    private val repository: ZvtRepository
) : AndroidViewModel(application) {

    private val ctx get() = getApplication<Application>()

    private val _connectionState = MutableLiveData(ConnectionState.DISCONNECTED)
    val connectionState: LiveData<ConnectionState> = _connectionState

    private val _statusMessage = MutableLiveData("")
    val statusMessage: LiveData<String> = _statusMessage

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _logEntries = MutableLiveData<List<LogEntry>>(emptyList())
    val logEntries: LiveData<List<LogEntry>> = _logEntries

    private val logBuffer = mutableListOf<LogEntry>()

    // Embedded simulator
    private var simulatorServer: SimulatorServer? = null

    private val _simulatorRunning = MutableLiveData(false)
    val simulatorRunning: LiveData<Boolean> = _simulatorRunning

    private val _simulatorStarting = MutableLiveData(false)
    val simulatorStarting: LiveData<Boolean> = _simulatorStarting

    init {
        repository.connectionState
            .onEach { state ->
                _connectionState.postValue(state)
            }
            .launchIn(viewModelScope)

        repository.logMessages
            .onEach { entry ->
                logBuffer.add(entry)
                if (logBuffer.size > 500) logBuffer.removeAt(0)
                _logEntries.postValue(logBuffer.toList())
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
