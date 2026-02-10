package com.panda_erkan.zvtclientdemo.ui.payment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.panda.zvt_library.model.ConnectionState
import com.panda.zvt_library.model.TransactionResult
import com.panda_erkan.zvtclientdemo.R
import com.panda_erkan.zvtclientdemo.repository.ZvtRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class PaymentViewModel(
    application: Application,
    private val repository: ZvtRepository
) : AndroidViewModel(application) {

    private val ctx get() = getApplication<Application>()

    private val _transactionResult = MutableStateFlow<TransactionResult?>(null)
    val transactionResult: StateFlow<TransactionResult?> = _transactionResult.asStateFlow()

    private val _intermediateStatus = MutableStateFlow("")
    val intermediateStatus: StateFlow<String> = _intermediateStatus.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _receiptText = MutableStateFlow("")
    val receiptText: StateFlow<String> = _receiptText.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = repository.connectionState

    private val receiptBuilder = StringBuilder()

    init {
        repository.intermediateStatus
            .onEach { status ->
                _intermediateStatus.value = status.message
            }
            .launchIn(viewModelScope)

        repository.printLines
            .onEach { line ->
                receiptBuilder.appendLine(line)
                _receiptText.value = receiptBuilder.toString()
            }
            .launchIn(viewModelScope)
    }

    fun authorize(amountText: String) {
        val cents = parseAmount(amountText) ?: run {
            _errorMessage.value = ctx.getString(R.string.invalid_amount, amountText)
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

    fun refund(amountText: String) {
        val cents = parseAmount(amountText) ?: run {
            _errorMessage.value = ctx.getString(R.string.invalid_amount, amountText)
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

    fun preAuthorize(amountText: String) {
        val cents = parseAmount(amountText) ?: run {
            _errorMessage.value = ctx.getString(R.string.invalid_amount, amountText)
            return
        }

        viewModelScope.launch {
            resetState()
            _isProcessing.value = true

            val result = repository.preAuthorize(cents)
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

    fun bookTotal(amountText: String, receiptNumber: Int, traceNumber: Int? = null, aid: String? = null) {
        val cents = parseAmount(amountText)

        viewModelScope.launch {
            resetState()
            _isProcessing.value = true

            val result = repository.bookTotal(receiptNumber, cents, traceNumber, aid)
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

    fun preAuthReversal(amountText: String, receiptNumber: Int) {
        val cents = parseAmount(amountText)

        viewModelScope.launch {
            resetState()
            _isProcessing.value = true

            val result = repository.preAuthReversal(receiptNumber, cents)
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
