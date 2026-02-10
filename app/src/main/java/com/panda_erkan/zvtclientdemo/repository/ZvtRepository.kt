package com.panda_erkan.zvtclientdemo.repository

import android.content.Context
import com.panda.zvt_library.ZvtCallback
import com.panda.zvt_library.ZvtClient
import com.panda.zvt_library.model.*
import com.panda_erkan.zvtclientdemo.R
import com.panda_erkan.zvtclientdemo.data.entity.JournalEntry
import com.panda_erkan.zvtclientdemo.data.model.OperationType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber

/**
 * ZVT Repository
 *
 * Wraps ZvtClient and provides a clean API to the ViewModel layer.
 * Catches errors and converts them to Result type.
 * Emits log messages via SharedFlow.
 * Saves operation results to journal database.
 */
class ZvtRepository(
    private val context: Context,
    private val client: ZvtClient,
    private val journalRepository: JournalRepository
) {
    companion object {
        private const val TAG = "ZvtRepository"
        private const val RECEIPT_DELIMITER = "|||"
    }

    // =====================================================
    // Observable Streams
    // =====================================================

    private val _logMessages = MutableSharedFlow<LogEntry>(replay = 100)
    val logMessages: SharedFlow<LogEntry> = _logMessages.asSharedFlow()

    private val _intermediateStatus = MutableSharedFlow<IntermediateStatus>(extraBufferCapacity = 1)
    val intermediateStatus: SharedFlow<IntermediateStatus> = _intermediateStatus.asSharedFlow()

    private val _printLines = MutableSharedFlow<String>(extraBufferCapacity = 50)
    val printLines: SharedFlow<String> = _printLines.asSharedFlow()

    val connectionState = client.connectionState

    init {
        client.setCallback(object : ZvtCallback {
            override fun onConnectionStateChanged(state: ConnectionState) {
                addLog(LogLevel.INFO, context.getString(R.string.log_connection, state.name))
            }

            override fun onIntermediateStatus(status: IntermediateStatus) {
                _intermediateStatus.tryEmit(status)
                addLog(LogLevel.INFO, "Terminal: ${status.message}")
            }

            override fun onPrintLine(line: String) {
                _printLines.tryEmit(line)
                addLog(LogLevel.DEBUG, "Print: $line")
            }

            override fun onDebugLog(tag: String, message: String) {
                addLog(LogLevel.DEBUG, "[$tag] $message")
                Timber.tag(tag).d(message)
            }

            override fun onError(error: ZvtError) {
                addLog(LogLevel.ERROR, context.getString(R.string.log_error, error.message))
            }
        })
    }

    // =====================================================
    // Connection
    // =====================================================

    suspend fun connect(): Result<Boolean> = runSafe("Connect") {
        client.connect()
        true
    }

    suspend fun disconnect(): Result<Boolean> = runSafe("Disconnect") {
        client.disconnect()
        true
    }

    suspend fun register(configByte: Byte = 0x08, tlvEnabled: Boolean = false): Result<Boolean> = runSafe("Register") {
        client.register(configByte, tlvEnabled)
    }

    suspend fun connectAndRegister(
        host: String,
        port: Int,
        configByte: Byte = 0x08,
        tlvEnabled: Boolean = false,
        keepAlive: Boolean = true
    ): Result<Boolean> = runSafe("Connect & Register") {
        client.updateConnectionParams(host, port, keepAlive)
        client.connect()
        client.register(configByte, tlvEnabled)
    }

    // =====================================================
    // Operations
    // =====================================================

    suspend fun authorize(amountInCents: Long): Result<TransactionResult> {
        val result = runSafe("Authorization") { client.authorize(amountInCents) }
        saveTransactionJournal(OperationType.PAYMENT, result)
        return result
    }

    suspend fun authorizeEuro(amountEuro: Double): Result<TransactionResult> {
        val result = runSafe("Authorization") { client.authorizeEuro(amountEuro) }
        saveTransactionJournal(OperationType.PAYMENT, result)
        return result
    }

    suspend fun reversal(receiptNumber: Int? = null): Result<TransactionResult> {
        val result = runSafe("Reversal") { client.reversal(receiptNumber) }
        saveTransactionJournal(OperationType.REVERSAL, result)
        return result
    }

    suspend fun refund(amountInCents: Long): Result<TransactionResult> {
        val result = runSafe("Refund") { client.refund(amountInCents) }
        saveTransactionJournal(OperationType.REFUND, result)
        return result
    }

    suspend fun endOfDay(): Result<EndOfDayResult> {
        val result = runSafe("End of Day") { client.endOfDay() }
        saveEndOfDayJournal(result)
        return result
    }

    suspend fun diagnosis(): Result<DiagnosisResult> {
        val result = runSafe("Diagnosis") { client.diagnosis() }
        saveDiagnosisJournal(result)
        return result
    }

    suspend fun statusEnquiry(): Result<TerminalStatus> {
        val result = runSafe("Status Enquiry") { client.statusEnquiry() }
        saveStatusJournal(result)
        return result
    }

    suspend fun abort(): Result<Boolean> =
        runSafe("Abort") {
            client.abort()
        }

    suspend fun preAuthorize(amountInCents: Long): Result<TransactionResult> {
        val result = runSafe("Pre-Authorization") { client.preAuthorize(amountInCents) }
        saveTransactionJournal(OperationType.PRE_AUTHORIZATION, result)
        return result
    }

    suspend fun bookTotal(
        receiptNumber: Int,
        amountInCents: Long? = null,
        traceNumber: Int? = null,
        aid: String? = null
    ): Result<TransactionResult> {
        val result = runSafe("Book Total") { client.bookTotal(receiptNumber, amountInCents, traceNumber, aid) }
        saveTransactionJournal(OperationType.BOOK_TOTAL, result)
        return result
    }

    suspend fun preAuthReversal(receiptNumber: Int, amountInCents: Long? = null): Result<TransactionResult> {
        val result = runSafe("Pre-Auth Reversal") { client.preAuthReversal(receiptNumber, amountInCents) }
        saveTransactionJournal(OperationType.PARTIAL_REVERSAL, result)
        return result
    }

    suspend fun repeatReceipt(): Result<TransactionResult> {
        val result = runSafe("Repeat Receipt") { client.repeatReceipt() }
        saveTransactionJournal(OperationType.REPEAT_RECEIPT, result)
        return result
    }

    suspend fun logOff(): Result<Boolean> {
        val result = runSafe("Log Off") { client.logOff() }
        saveSimpleJournal(OperationType.LOG_OFF, result)
        return result
    }

    // =====================================================
    // Journal Save Helpers
    // =====================================================

    private suspend fun saveTransactionJournal(type: OperationType, result: Result<TransactionResult>) {
        val entry = result.fold(
            onSuccess = { tx ->
                JournalEntry(
                    operationType = type,
                    success = tx.success,
                    resultCode = tx.resultCode.toInt() and 0xFF,
                    resultMessage = tx.resultMessage,
                    amountInCents = tx.amountInCents,
                    receiptNumber = tx.receiptNumber,
                    traceNumber = tx.traceNumber,
                    terminalId = tx.terminalId,
                    vuNumber = tx.vuNumber,
                    transactionDate = tx.date,
                    transactionTime = tx.time,
                    originalTrace = tx.originalTrace,
                    turnoverNumber = tx.turnoverNumber,
                    maskedPan = tx.cardData?.maskedPan?.takeIf { it.isNotEmpty() },
                    cardType = tx.cardData?.cardType?.takeIf { it.isNotEmpty() },
                    cardName = tx.cardData?.cardName?.takeIf { it.isNotEmpty() },
                    expiryDate = tx.cardData?.expiryDate?.takeIf { it.isNotEmpty() },
                    cardSequenceNumber = tx.cardData?.sequenceNumber?.takeIf { it > 0 },
                    aid = tx.cardData?.aid?.takeIf { it.isNotEmpty() },
                    receiptLines = tx.receiptLines.takeIf { it.isNotEmpty() }?.joinToString(RECEIPT_DELIMITER)
                )
            },
            onFailure = { error ->
                JournalEntry(
                    operationType = type,
                    success = false,
                    resultMessage = error.message ?: "Unknown error"
                )
            }
        )
        saveEntrySafe(entry)
    }

    private suspend fun saveEndOfDayJournal(result: Result<EndOfDayResult>) {
        val entry = result.fold(
            onSuccess = { eod ->
                JournalEntry(
                    operationType = OperationType.END_OF_DAY,
                    success = eod.success,
                    resultMessage = eod.message,
                    transactionCount = eod.transactionCount,
                    totalAmountInCents = eod.totalAmountInCents,
                    receiptLines = eod.receiptLines.takeIf { it.isNotEmpty() }?.joinToString(RECEIPT_DELIMITER)
                )
            },
            onFailure = { error ->
                JournalEntry(
                    operationType = OperationType.END_OF_DAY,
                    success = false,
                    resultMessage = error.message ?: "Unknown error"
                )
            }
        )
        saveEntrySafe(entry)
    }

    private suspend fun saveDiagnosisJournal(result: Result<DiagnosisResult>) {
        val entry = result.fold(
            onSuccess = { diag ->
                JournalEntry(
                    operationType = OperationType.DIAGNOSIS,
                    success = diag.success,
                    resultMessage = diag.errorMessage.ifEmpty { diag.status.statusMessage },
                    terminalId = diag.status.terminalId.takeIf { it.isNotEmpty() },
                    statusMessage = diag.status.statusMessage.takeIf { it.isNotEmpty() }
                )
            },
            onFailure = { error ->
                JournalEntry(
                    operationType = OperationType.DIAGNOSIS,
                    success = false,
                    resultMessage = error.message ?: "Unknown error"
                )
            }
        )
        saveEntrySafe(entry)
    }

    private suspend fun saveStatusJournal(result: Result<TerminalStatus>) {
        val entry = result.fold(
            onSuccess = { status ->
                JournalEntry(
                    operationType = OperationType.STATUS_ENQUIRY,
                    success = true,
                    resultMessage = status.statusMessage,
                    terminalId = status.terminalId.takeIf { it.isNotEmpty() },
                    statusMessage = status.statusMessage.takeIf { it.isNotEmpty() }
                )
            },
            onFailure = { error ->
                JournalEntry(
                    operationType = OperationType.STATUS_ENQUIRY,
                    success = false,
                    resultMessage = error.message ?: "Unknown error"
                )
            }
        )
        saveEntrySafe(entry)
    }

    private suspend fun saveSimpleJournal(type: OperationType, result: Result<Boolean>) {
        val entry = result.fold(
            onSuccess = { success ->
                JournalEntry(
                    operationType = type,
                    success = success,
                    resultMessage = if (success) "OK" else "Failed"
                )
            },
            onFailure = { error ->
                JournalEntry(
                    operationType = type,
                    success = false,
                    resultMessage = error.message ?: "Unknown error"
                )
            }
        )
        saveEntrySafe(entry)
    }

    private suspend fun saveEntrySafe(entry: JournalEntry) {
        try {
            journalRepository.saveEntry(entry)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save journal entry")
        }
    }

    // =====================================================
    // Helper
    // =====================================================

    private suspend fun <T> runSafe(operation: String, block: suspend () -> T): Result<T> {
        return try {
            addLog(LogLevel.INFO, context.getString(R.string.log_operation_starting, operation))
            val result = block()
            addLog(LogLevel.INFO, context.getString(R.string.log_operation_completed, operation))
            Result.success(result)
        } catch (e: ZvtError) {
            addLog(LogLevel.ERROR, context.getString(R.string.log_operation_error, operation, e.message))
            Result.failure(e)
        } catch (e: Exception) {
            addLog(LogLevel.ERROR, context.getString(R.string.log_operation_unexpected, operation, e.message))
            Result.failure(e)
        }
    }

    private fun addLog(level: LogLevel, message: String) {
        val entry = LogEntry(
            level = level,
            message = message,
            timestamp = System.currentTimeMillis()
        )
        _logMessages.tryEmit(entry)
    }

    fun destroy() {
        client.destroy()
    }
}

// =====================================================
// Log Models
// =====================================================

data class LogEntry(
    val level: LogLevel,
    val message: String,
    val timestamp: Long
) {
    val timeFormatted: String
        get() {
            val sdf = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(timestamp))
        }
}

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}
