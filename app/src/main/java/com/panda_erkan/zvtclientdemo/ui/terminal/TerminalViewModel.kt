package com.panda_erkan.zvtclientdemo.ui.terminal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.panda.zvt_library.model.ConnectionState
import com.panda.zvt_library.model.DiagnosisResult
import com.panda.zvt_library.model.EndOfDayResult
import com.panda.zvt_library.model.TerminalStatus
import com.panda.zvt_library.model.TransactionResult
import com.panda_erkan.zvtclientdemo.R
import com.panda_erkan.zvtclientdemo.repository.ZvtRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TerminalViewModel(
    application: Application,
    private val repository: ZvtRepository
) : AndroidViewModel(application) {

    private val ctx get() = getApplication<Application>()

    private val _diagnosisResult = MutableStateFlow<DiagnosisResult?>(null)
    val diagnosisResult: StateFlow<DiagnosisResult?> = _diagnosisResult.asStateFlow()

    private val _endOfDayResult = MutableStateFlow<EndOfDayResult?>(null)
    val endOfDayResult: StateFlow<EndOfDayResult?> = _endOfDayResult.asStateFlow()

    private val _terminalStatus = MutableStateFlow<TerminalStatus?>(null)
    val terminalStatus: StateFlow<TerminalStatus?> = _terminalStatus.asStateFlow()

    private val _repeatReceiptResult = MutableStateFlow<TransactionResult?>(null)
    val repeatReceiptResult: StateFlow<TransactionResult?> = _repeatReceiptResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = repository.connectionState

    fun diagnosis() {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = ctx.getString(R.string.running_diagnosis)

            val result = repository.diagnosis()
            result.fold(
                onSuccess = { diag ->
                    _diagnosisResult.value = diag
                    _statusMessage.value = if (diag.success) ctx.getString(R.string.diagnosis_successful) else diag.errorMessage
                },
                onFailure = { error ->
                    _statusMessage.value = ctx.getString(R.string.status_error, error.message)
                }
            )
            _isLoading.value = false
        }
    }

    fun endOfDay() {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = ctx.getString(R.string.running_end_of_day)

            val result = repository.endOfDay()
            result.fold(
                onSuccess = { eod ->
                    if (eod.success) {
                        _statusMessage.value = ctx.getString(R.string.running_repeat_receipt)
                        val receiptResult = repository.repeatReceipt()
                        receiptResult.fold(
                            onSuccess = { txResult ->
                                _endOfDayResult.value = eod.copy(
                                    receiptLines = txResult.receiptLines.ifEmpty { eod.receiptLines }
                                )
                            },
                            onFailure = {
                                _endOfDayResult.value = eod
                            }
                        )
                    } else {
                        _endOfDayResult.value = eod
                    }
                    _statusMessage.value = if (eod.success) ctx.getString(R.string.end_of_day_successful) else eod.message
                },
                onFailure = { error ->
                    _statusMessage.value = ctx.getString(R.string.status_error, error.message)
                }
            )
            _isLoading.value = false
        }
    }

    fun statusEnquiry() {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = ctx.getString(R.string.querying_status)

            val result = repository.statusEnquiry()
            result.fold(
                onSuccess = { status ->
                    _terminalStatus.value = status
                    _statusMessage.value = ctx.getString(R.string.terminal_status_msg, status.statusMessage)
                },
                onFailure = { error ->
                    _statusMessage.value = ctx.getString(R.string.status_error, error.message)
                }
            )
            _isLoading.value = false
        }
    }

    fun repeatReceipt() {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = ctx.getString(R.string.running_repeat_receipt)

            val result = repository.repeatReceipt()
            result.fold(
                onSuccess = { txResult ->
                    _repeatReceiptResult.value = txResult
                    _statusMessage.value = if (txResult.success) {
                        ctx.getString(R.string.repeat_receipt_result)
                    } else {
                        txResult.resultMessage
                    }
                },
                onFailure = { error ->
                    _statusMessage.value = ctx.getString(R.string.status_error, error.message)
                }
            )
            _isLoading.value = false
        }
    }

    fun logOff() {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = ctx.getString(R.string.running_log_off)

            val result = repository.logOff()
            result.fold(
                onSuccess = { success ->
                    _statusMessage.value = if (success) {
                        ctx.getString(R.string.log_off_successful)
                    } else {
                        ctx.getString(R.string.log_off_failed)
                    }
                },
                onFailure = { error ->
                    _statusMessage.value = ctx.getString(R.string.status_error, error.message)
                }
            )
            _isLoading.value = false
        }
    }
}
