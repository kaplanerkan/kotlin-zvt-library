package com.erkan.zvtclient.ui.terminal

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erkan.zvt.model.DiagnosisResult
import com.erkan.zvt.model.EndOfDayResult
import com.erkan.zvt.model.TerminalStatus
import com.erkan.zvtclient.repository.ZvtRepository
import kotlinx.coroutines.launch

class TerminalViewModel(
    private val repository: ZvtRepository
) : ViewModel() {

    private val _diagnosisResult = MutableLiveData<DiagnosisResult?>()
    val diagnosisResult: LiveData<DiagnosisResult?> = _diagnosisResult

    private val _endOfDayResult = MutableLiveData<EndOfDayResult?>()
    val endOfDayResult: LiveData<EndOfDayResult?> = _endOfDayResult

    private val _terminalStatus = MutableLiveData<TerminalStatus?>()
    val terminalStatus: LiveData<TerminalStatus?> = _terminalStatus

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _statusMessage = MutableLiveData("")
    val statusMessage: LiveData<String> = _statusMessage

    fun diagnosis() {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Tanılama yapılıyor..."

            val result = repository.diagnosis()
            result.fold(
                onSuccess = { diag ->
                    _diagnosisResult.value = diag
                    _statusMessage.value = if (diag.success) "Tanılama başarılı" else diag.errorMessage
                },
                onFailure = { error ->
                    _statusMessage.value = "Hata: ${error.message}"
                }
            )
            _isLoading.value = false
        }
    }

    fun endOfDay() {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Gün sonu yapılıyor..."

            val result = repository.endOfDay()
            result.fold(
                onSuccess = { eod ->
                    _endOfDayResult.value = eod
                    _statusMessage.value = if (eod.success) "Gün sonu başarılı" else eod.message
                },
                onFailure = { error ->
                    _statusMessage.value = "Hata: ${error.message}"
                }
            )
            _isLoading.value = false
        }
    }

    fun statusEnquiry() {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Durum sorgulanıyor..."

            val result = repository.statusEnquiry()
            result.fold(
                onSuccess = { status ->
                    _terminalStatus.value = status
                    _statusMessage.value = "Terminal: ${status.statusMessage}"
                },
                onFailure = { error ->
                    _statusMessage.value = "Hata: ${error.message}"
                }
            )
            _isLoading.value = false
        }
    }
}
