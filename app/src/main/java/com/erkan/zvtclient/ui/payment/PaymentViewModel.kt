package com.erkan.zvtclient.ui.payment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erkan.zvt.model.IntermediateStatus
import com.erkan.zvt.model.TransactionResult
import com.erkan.zvtclient.repository.ZvtRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class PaymentViewModel(
    private val repository: ZvtRepository
) : ViewModel() {

    private val _transactionResult = MutableLiveData<TransactionResult?>()
    val transactionResult: LiveData<TransactionResult?> = _transactionResult

    private val _intermediateStatus = MutableLiveData<String>("")
    val intermediateStatus: LiveData<String> = _intermediateStatus

    private val _isProcessing = MutableLiveData(false)
    val isProcessing: LiveData<Boolean> = _isProcessing

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _receiptText = MutableLiveData("")
    val receiptText: LiveData<String> = _receiptText

    private val receiptBuilder = StringBuilder()

    init {
        repository.intermediateStatus
            .onEach { status ->
                _intermediateStatus.postValue(status.message)
            }
            .launchIn(viewModelScope)

        repository.printLines
            .onEach { line ->
                receiptBuilder.appendLine(line)
                _receiptText.postValue(receiptBuilder.toString())
            }
            .launchIn(viewModelScope)
    }

    /**
     * Ödeme işlemi başlat
     * @param amountText Tutar metni (ör: "12.50" veya "1250")
     */
    fun authorize(amountText: String) {
        val cents = parseAmount(amountText) ?: run {
            _errorMessage.value = "Geçersiz tutar: $amountText"
            return
        }

        viewModelScope.launch {
            resetState()
            _isProcessing.value = true

            val result = repository.authorize(cents)
            result.fold(
                onSuccess = { txResult ->
                    _transactionResult.value = txResult
                    if (!txResult.success) {
                        _errorMessage.value = txResult.resultMessage
                    }
                },
                onFailure = { error ->
                    _errorMessage.value = error.message
                }
            )
            _isProcessing.value = false
        }
    }

    /**
     * İptal (Reversal)
     */
    fun reversal(receiptNumber: Int? = null) {
        viewModelScope.launch {
            resetState()
            _isProcessing.value = true

            val result = repository.reversal(receiptNumber)
            result.fold(
                onSuccess = { txResult -> _transactionResult.value = txResult },
                onFailure = { error -> _errorMessage.value = error.message }
            )
            _isProcessing.value = false
        }
    }

    /**
     * İade (Refund)
     */
    fun refund(amountText: String) {
        val cents = parseAmount(amountText) ?: run {
            _errorMessage.value = "Geçersiz tutar: $amountText"
            return
        }

        viewModelScope.launch {
            resetState()
            _isProcessing.value = true

            val result = repository.refund(cents)
            result.fold(
                onSuccess = { txResult -> _transactionResult.value = txResult },
                onFailure = { error -> _errorMessage.value = error.message }
            )
            _isProcessing.value = false
        }
    }

    /**
     * İşlemi iptal et
     */
    fun abort() {
        viewModelScope.launch {
            repository.abort()
        }
    }

    private fun resetState() {
        _transactionResult.value = null
        _errorMessage.value = null
        _intermediateStatus.value = ""
        receiptBuilder.clear()
        _receiptText.value = ""
    }

    /**
     * Tutar metnini cent'e çevirir
     * "12.50" → 1250, "12,50" → 1250, "1250" → 1250
     */
    private fun parseAmount(text: String): Long? {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return null

        return try {
            if (cleaned.contains('.') || cleaned.contains(',')) {
                val normalized = cleaned.replace(',', '.')
                val euros = normalized.toDouble()
                (euros * 100).toLong()
            } else {
                cleaned.toLong()
            }
        } catch (e: NumberFormatException) {
            null
        }
    }
}
