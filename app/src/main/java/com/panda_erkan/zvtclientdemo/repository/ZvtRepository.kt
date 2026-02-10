package com.panda_erkan.zvtclientdemo.repository

import android.content.Context
import com.panda.zvt_library.ZvtCallback
import com.panda.zvt_library.ZvtClient
import com.panda.zvt_library.model.*
import com.panda_erkan.zvtclientdemo.R
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
 */
class ZvtRepository(
    private val context: Context,
    private val client: ZvtClient
) {
    companion object {
        private const val TAG = "ZvtRepository"
    }

    // =====================================================
    // Observable Streams
    // =====================================================

    private val _logMessages = MutableSharedFlow<LogEntry>(replay = 100)
    val logMessages: SharedFlow<LogEntry> = _logMessages.asSharedFlow()

    private val _intermediateStatus = MutableSharedFlow<IntermediateStatus>(replay = 1)
    val intermediateStatus: SharedFlow<IntermediateStatus> = _intermediateStatus.asSharedFlow()

    private val _printLines = MutableSharedFlow<String>(replay = 50)
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

    suspend fun register(): Result<Boolean> = runSafe("Register") {
        client.register()
    }

    suspend fun connectAndRegister(host: String, port: Int): Result<Boolean> = runSafe("Connect & Register") {
        client.updateConnectionParams(host, port)
        client.connect()
        client.register()
    }

    // =====================================================
    // Operations
    // =====================================================

    suspend fun authorize(amountInCents: Long): Result<TransactionResult> =
        runSafe("Authorization") {
            client.authorize(amountInCents)
        }

    suspend fun authorizeEuro(amountEuro: Double): Result<TransactionResult> =
        runSafe("Authorization") {
            client.authorizeEuro(amountEuro)
        }

    suspend fun reversal(receiptNumber: Int? = null): Result<TransactionResult> =
        runSafe("Reversal") {
            client.reversal(receiptNumber)
        }

    suspend fun refund(amountInCents: Long): Result<TransactionResult> =
        runSafe("Refund") {
            client.refund(amountInCents)
        }

    suspend fun endOfDay(): Result<EndOfDayResult> =
        runSafe("End of Day") {
            client.endOfDay()
        }

    suspend fun diagnosis(): Result<DiagnosisResult> =
        runSafe("Diagnosis") {
            client.diagnosis()
        }

    suspend fun statusEnquiry(): Result<TerminalStatus> =
        runSafe("Status Enquiry") {
            client.statusEnquiry()
        }

    suspend fun abort(): Result<Boolean> =
        runSafe("Abort") {
            client.abort()
        }

    suspend fun preAuthorize(amountInCents: Long): Result<TransactionResult> =
        runSafe("Pre-Authorization") {
            client.preAuthorize(amountInCents)
        }

    suspend fun bookTotal(amountInCents: Long, receiptNumber: Int): Result<TransactionResult> =
        runSafe("Book Total") {
            client.bookTotal(amountInCents, receiptNumber)
        }

    suspend fun partialReversal(amountInCents: Long, receiptNumber: Int): Result<TransactionResult> =
        runSafe("Partial Reversal") {
            client.partialReversal(amountInCents, receiptNumber)
        }

    suspend fun repeatReceipt(): Result<TransactionResult> =
        runSafe("Repeat Receipt") {
            client.repeatReceipt()
        }

    suspend fun logOff(): Result<Boolean> =
        runSafe("Log Off") {
            client.logOff()
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
