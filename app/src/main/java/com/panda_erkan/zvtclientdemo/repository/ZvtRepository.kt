package com.panda_erkan.zvtclientdemo.repository

import com.panda.zvt_library.ZvtCallback
import com.panda.zvt_library.ZvtClient
import com.panda.zvt_library.model.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber

/**
 * ZVT Repository
 *
 * ZvtClient'ı sararak ViewModel katmanına temiz bir API sunar.
 * Hataları yakalar ve Result tipine dönüştürür.
 * Log mesajlarını SharedFlow ile dışarı verir.
 */
class ZvtRepository(
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
                addLog(LogLevel.INFO, "Bağlantı: $state")
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
                addLog(LogLevel.ERROR, "Hata: ${error.message}")
            }
        })
    }

    // =====================================================
    // Bağlantı
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
    // İşlemler
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

    // =====================================================
    // Yardımcı
    // =====================================================

    private suspend fun <T> runSafe(operation: String, block: suspend () -> T): Result<T> {
        return try {
            addLog(LogLevel.INFO, "▶ $operation başlatılıyor...")
            val result = block()
            addLog(LogLevel.INFO, "✓ $operation tamamlandı")
            Result.success(result)
        } catch (e: ZvtError) {
            addLog(LogLevel.ERROR, "✗ $operation hatası: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            addLog(LogLevel.ERROR, "✗ $operation beklenmeyen hata: ${e.message}")
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
// Log Modelleri
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
