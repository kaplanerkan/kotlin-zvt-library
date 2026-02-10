package com.panda.zvt_library.model

/**
 * Data models for the ZVT protocol library.
 *
 * Contains all data classes, sealed error classes, and enumerations used
 * by the ZVT client for configuration, transaction results, terminal status,
 * and error handling.
 *
 * Reference: ZVT Protocol Specification v13.13
 *
 * @author Erkan Kaplan
 * @since 2026-02-10
 */

/**
 * ZVT connection configuration.
 *
 * @property host Terminal IP address.
 * @property port Terminal TCP port (default: 20007).
 * @property connectTimeoutMs TCP connection timeout in milliseconds.
 * @property readTimeoutMs Socket read timeout in milliseconds.
 * @property password Terminal password (6-digit BCD, default: "000000").
 * @property currencyCode ISO 4217 currency code (default: 978 = EUR).
 * @property autoAck Whether to automatically send ACK responses.
 * @property keepAlive Whether to enable TCP Keep-Alive on the socket.
 * @property debugMode Whether to forward debug logs to the callback.
 */
data class ZvtConfig(
    val host: String,
    val port: Int = 20007,
    val connectTimeoutMs: Int = 10_000,
    val readTimeoutMs: Int = 90_000,
    val password: String = "000000",
    val currencyCode: Int = 978,
    val autoAck: Boolean = true,
    val keepAlive: Boolean = true,
    val debugMode: Boolean = false
)

/**
 * Result of a payment transaction or command execution.
 *
 * @property success Whether the transaction was successful.
 * @property resultCode Result code from BMP 0x27 (0x00 = success).
 * @property resultMessage Human-readable result description.
 * @property amountInCents Transaction amount in cents.
 * @property cardData Card information (PAN, type, AID, etc.), if available.
 * @property receiptNumber Receipt number from the terminal.
 * @property traceNumber Trace number for this transaction.
 * @property terminalId Terminal identifier (TID).
 * @property vuNumber VU (merchant contract) number.
 * @property date Transaction date (MMDD format).
 * @property time Transaction time (HHMMSS format).
 * @property originalTrace Original trace number (used in reversals, BMP 0x37).
 * @property turnoverNumber Turnover/batch number (BMP 0x88).
 * @property receiptLines Receipt text lines received from the terminal.
 * @property rawData Raw response data for debugging purposes.
 */
data class TransactionResult(
    val success: Boolean,
    val resultCode: Byte = 0x00,
    val resultMessage: String = "",
    val amountInCents: Long = 0,
    val cardData: CardData? = null,
    val receiptNumber: Int = 0,
    val traceNumber: Int = 0,
    val terminalId: String = "",
    val vuNumber: String = "",
    val date: String = "",
    val time: String = "",
    val originalTrace: Int = 0,
    val turnoverNumber: Int = 0,
    val receiptLines: List<String> = emptyList(),
    val rawData: ByteArray? = null
) {
    /** Formats the amount as Euro: e.g. `"12,50 EUR"`. */
    val amountFormatted: String
        get() {
            val euros = amountInCents / 100
            val cents = amountInCents % 100
            return String.format("%d,%02d \u20AC", euros, cents)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TransactionResult) return false
        return success == other.success && resultCode == other.resultCode &&
                traceNumber == other.traceNumber && receiptNumber == other.receiptNumber
    }

    override fun hashCode(): Int = 31 * success.hashCode() + traceNumber
}

/**
 * Card data extracted from BMP fields during a transaction.
 *
 * @property maskedPan Masked card number, e.g. `"****1234"`.
 * @property cardType Card type name, e.g. `"VISA"`, `"Mastercard"`.
 * @property cardName Card application label / card name.
 * @property expiryDate Expiry date in YYMM format.
 * @property sequenceNumber Card sequence number.
 * @property aid Application Identifier (AID) as hex string.
 */
data class CardData(
    val maskedPan: String = "",
    val cardType: String = "",
    val cardName: String = "",
    val expiryDate: String = "",
    val sequenceNumber: Int = 0,
    val aid: String = ""
)

/**
 * Terminal status information returned by Diagnosis or Status Enquiry commands.
 *
 * @property isConnected Whether the terminal is currently connected.
 * @property terminalId Terminal identifier (TID).
 * @property softwareVersion Terminal software version.
 * @property terminalModel Terminal hardware model.
 * @property statusMessage Status description text.
 * @property lastTransactionTime Timestamp of the last transaction.
 */
data class TerminalStatus(
    val isConnected: Boolean,
    val terminalId: String = "",
    val softwareVersion: String = "",
    val terminalModel: String = "",
    val statusMessage: String = "",
    val lastTransactionTime: String = ""
)

/**
 * Result of a Diagnosis command (06 70).
 *
 * @property success Whether the diagnosis was successful.
 * @property status Terminal status information.
 * @property errorMessage Error message, if any.
 */
data class DiagnosisResult(
    val success: Boolean,
    val status: TerminalStatus = TerminalStatus(false),
    val errorMessage: String = ""
)

/**
 * Result of an End of Day command (06 50).
 *
 * @property success Whether the batch close was successful.
 * @property transactionCount Total number of transactions in the batch.
 * @property totalAmountInCents Total amount processed in the batch (cents).
 * @property message Result description text.
 * @property receiptLines Receipt text lines from the terminal.
 */
data class EndOfDayResult(
    val success: Boolean,
    val transactionCount: Int = 0,
    val totalAmountInCents: Long = 0,
    val message: String = "",
    val receiptLines: List<String> = emptyList()
)

/**
 * Sealed class hierarchy for ZVT-specific errors.
 *
 * Each error type carries a descriptive message and optional additional context.
 */
sealed class ZvtError : Exception() {

    /**
     * TCP connection error (failed to connect, connection lost, etc.).
     *
     * @property message Error description.
     * @property cause Underlying exception, if any.
     */
    data class ConnectionError(
        override val message: String,
        override val cause: Throwable? = null
    ) : ZvtError()

    /**
     * Timeout error (ACK not received, response not received within time limit).
     *
     * @property message Error description.
     */
    data class TimeoutError(
        override val message: String = "Operation timed out"
    ) : ZvtError()

    /**
     * Protocol-level error (unexpected response, NACK received, etc.).
     *
     * @property message Error description.
     * @property rawData Raw packet data for debugging.
     */
    data class ProtocolError(
        override val message: String,
        val rawData: ByteArray? = null
    ) : ZvtError()

    /**
     * Terminal reported an error via result code.
     *
     * @property resultCode The result code byte from the terminal.
     * @property message Error description.
     */
    data class TerminalError(
        val resultCode: Byte,
        override val message: String
    ) : ZvtError()

    /**
     * Transaction was declined by the terminal or the authorization system.
     *
     * @property resultCode The decline reason code.
     * @property message Decline description.
     * @property cardData Card data associated with the declined transaction.
     */
    data class TransactionDeclined(
        val resultCode: Byte,
        override val message: String,
        val cardData: CardData? = null
    ) : ZvtError()
}

/**
 * ZVT client connection state machine.
 *
 * State transitions:
 * `DISCONNECTED` -> `CONNECTING` -> `CONNECTED` -> `REGISTERING` -> `REGISTERED`
 *
 * `ERROR` can be reached from any state.
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    REGISTERING,
    REGISTERED,
    ERROR
}

/**
 * Enumeration of supported ZVT transaction types.
 */
enum class TransactionType {
    AUTHORIZATION,
    REVERSAL,
    REFUND,
    PRE_AUTHORIZATION,
    END_OF_DAY,
    DIAGNOSIS,
    STATUS_ENQUIRY
}

/**
 * Intermediate status information received from the terminal during a transaction (04 FF).
 *
 * These messages inform the ECR about the current state of the payment process,
 * e.g. "Insert card", "Enter PIN", "Please wait".
 *
 * @property statusCode The intermediate status code byte.
 * @property message Human-readable status message.
 * @property timestamp Creation timestamp in milliseconds.
 */
data class IntermediateStatus(
    val statusCode: Byte,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
