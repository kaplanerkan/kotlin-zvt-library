package com.panda.zvt_library

import com.panda.zvt_library.model.*
import com.panda.zvt_library.protocol.*
import com.panda.zvt_library.util.toHexString
import com.panda.zvt_library.util.toLogString
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

/**
 * ZVT TCP/IP Client.
 *
 * Provides full ZVT protocol communication with a payment terminal over TCP/IP.
 * Uses Kotlin coroutines for asynchronous, thread-safe operation.
 *
 * All ECR <-> PT communication is comprehensively logged via Timber, including
 * every byte sent (TX) and received (RX), ACK/NACK packets, intermediate statuses,
 * print lines, status info, and completion/abort responses.
 *
 * Basic usage:
 * ```kotlin
 * val config = ZvtConfig(host = "192.168.1.100", port = 20007)
 * val client = ZvtClient(config)
 *
 * // Connect and register
 * client.connect()
 * client.register()
 *
 * // Make a payment
 * val result = client.authorize(1250) // 12.50 EUR
 *
 * // Disconnect
 * client.disconnect()
 * ```
 *
 * Transaction flow:
 * 1. ECR sends command -> Terminal returns ACK
 * 2. Terminal sends intermediate status messages (04 FF) -> ECR returns ACK
 * 3. Terminal sends status information (04 0F) -> ECR returns ACK
 * 4. Terminal sends completion (06 0F) -> Transaction ends
 *
 * @param config Connection and protocol configuration.
 *
 * @author Erkan Kaplan
 * @since 2026-02-10
 */
class ZvtClient(
    private var config: ZvtConfig
) {
    companion object {
        private const val TAG = "ZVT"
    }

    /**
     * Updates the connection parameters (host and port) before connecting.
     *
     * @param host Terminal IP address.
     * @param port Terminal TCP port (default 20007).
     */
    fun updateConnectionParams(host: String, port: Int) {
        config = config.copy(host = host, port = port)
        Timber.tag(TAG).d("Connection params updated: host=$host, port=$port")
    }

    // =====================================================
    // State
    // =====================================================

    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)

    /** Observable connection state. */
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _intermediateStatus = MutableStateFlow<IntermediateStatus?>(null)

    /** Observable intermediate status (null when no transaction is in progress). */
    val intermediateStatus: StateFlow<IntermediateStatus?> = _intermediateStatus.asStateFlow()

    private var callback: ZvtCallback? = null
    private val receiptLines = mutableListOf<String>()
    private val readBuffer = ByteArray(ZvtConstants.READ_BUFFER_SIZE)

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sendLock = Any()

    // =====================================================
    // Configuration
    // =====================================================

    /**
     * Sets the event listener for receiving callbacks.
     *
     * @param callback The [ZvtCallback] implementation, or `null` to remove.
     */
    fun setCallback(callback: ZvtCallback?) {
        this.callback = callback
    }

    // =====================================================
    // Connection Management
    // =====================================================

    /**
     * Opens a TCP connection to the payment terminal.
     *
     * @throws ZvtError.ConnectionError if the connection fails.
     * @throws ZvtError.TimeoutError if the connection times out.
     */
    suspend fun connect() = withContext(Dispatchers.IO) {
        if (_connectionState.value == ConnectionState.CONNECTED ||
            _connectionState.value == ConnectionState.REGISTERED) {
            log("Already connected, skipping connect()")
            return@withContext
        }

        try {
            updateState(ConnectionState.CONNECTING)
            log("Connecting to ${config.host}:${config.port} (connectTimeout=${config.connectTimeoutMs}ms, readTimeout=${config.readTimeoutMs}ms)")

            socket = Socket().apply {
                soTimeout = config.readTimeoutMs
                connect(
                    InetSocketAddress(config.host, config.port),
                    config.connectTimeoutMs
                )
            }

            inputStream = socket?.getInputStream()
            outputStream = socket?.getOutputStream()

            updateState(ConnectionState.CONNECTED)
            log("Connection established to ${config.host}:${config.port}")

        } catch (e: SocketTimeoutException) {
            updateState(ConnectionState.ERROR)
            log("Connection timeout: ${config.host}:${config.port}")
            throw ZvtError.TimeoutError("Connection timeout: ${config.host}:${config.port}")
        } catch (e: IOException) {
            updateState(ConnectionState.ERROR)
            log("Connection error: ${e.message}")
            throw ZvtError.ConnectionError(
                "Connection error: ${e.message}",
                cause = e
            )
        }
    }

    /**
     * Closes the TCP connection and releases all resources.
     */
    fun disconnect() {
        try {
            log("Disconnecting from ${config.host}:${config.port}")
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (e: Exception) {
            log("Disconnect error: ${e.message}")
        } finally {
            socket = null
            inputStream = null
            outputStream = null
            updateState(ConnectionState.DISCONNECTED)
            log("Disconnected")
        }
    }

    /**
     * Checks whether the TCP connection is active.
     *
     * @return `true` if the socket is connected and not closed.
     */
    val isConnected: Boolean
        get() = socket?.isConnected == true && socket?.isClosed == false

    // =====================================================
    // ZVT Commands
    // =====================================================

    /**
     * Sends a Registration command (06 00) to the terminal.
     *
     * This must be called as the first command after connecting. It registers
     * the ECR with the terminal and establishes the session configuration.
     *
     * @param configByte Configuration bitmask byte.
     * @return `true` if registration was successful.
     * @throws ZvtError on protocol or connection errors.
     */
    suspend fun register(
        configByte: Byte = ZvtConstants.REG_INTERMEDIATE_STATUS
    ): Boolean = withContext(Dispatchers.IO) {
        ensureConnected()
        updateState(ConnectionState.REGISTERING)

        val packet = ZvtCommandBuilder.buildRegistration(
            password = config.password,
            configByte = configByte,
            currencyCode = config.currencyCode
        )

        log("=== REGISTRATION (06 00) ===")
        val result = executeCommand(packet)

        if (result.success) {
            updateState(ConnectionState.REGISTERED)
            log("Registration successful")
        } else {
            updateState(ConnectionState.CONNECTED)
            log("Registration failed: ${result.resultMessage}")
        }

        result.success
    }

    /**
     * Sends an Authorization command (06 01) to perform a payment transaction.
     *
     * @param amountInCents Amount in cents, e.g. 1250 = 12.50 EUR.
     * @param paymentType Payment type byte (default: automatic detection).
     * @return [TransactionResult] with the transaction outcome and details.
     * @throws ZvtError on protocol or connection errors.
     */
    suspend fun authorize(
        amountInCents: Long,
        paymentType: Byte = ZvtConstants.PAY_TYPE_DEFAULT
    ): TransactionResult = withContext(Dispatchers.IO) {
        ensureConnected()
        receiptLines.clear()

        val packet = ZvtCommandBuilder.buildAuthorization(
            amountInCents = amountInCents,
            paymentType = paymentType,
            currencyCode = config.currencyCode
        )

        log("=== AUTHORIZATION (06 01) === amount=${amountInCents} cents (${amountInCents / 100.0} EUR)")
        val result = executeCommand(packet)
        result.copy(receiptLines = receiptLines.toList())
    }

    /**
     * Convenience method to authorize with a Euro amount.
     *
     * @param amountEuro Amount in Euro, e.g. 12.50.
     * @param paymentType Payment type byte.
     * @return [TransactionResult] with the transaction outcome.
     */
    suspend fun authorizeEuro(
        amountEuro: Double,
        paymentType: Byte = ZvtConstants.PAY_TYPE_DEFAULT
    ): TransactionResult {
        return authorize((amountEuro * 100).toLong(), paymentType)
    }

    /**
     * Sends a Reversal command (06 30) to cancel a previous transaction.
     *
     * @param receiptNumber Optional receipt number of the transaction to cancel.
     * @return [TransactionResult] with the reversal outcome.
     * @throws ZvtError on protocol or connection errors.
     */
    suspend fun reversal(receiptNumber: Int? = null): TransactionResult =
        withContext(Dispatchers.IO) {
            ensureConnected()
            receiptLines.clear()

            val packet = ZvtCommandBuilder.buildReversal(receiptNumber)
            log("=== REVERSAL (06 30) ===" + (receiptNumber?.let { " receipt=$it" } ?: ""))
            val result = executeCommand(packet)
            result.copy(receiptLines = receiptLines.toList())
        }

    /**
     * Sends a Refund command (06 31).
     *
     * @param amountInCents Refund amount in cents.
     * @return [TransactionResult] with the refund outcome.
     * @throws ZvtError on protocol or connection errors.
     */
    suspend fun refund(amountInCents: Long): TransactionResult =
        withContext(Dispatchers.IO) {
            ensureConnected()
            receiptLines.clear()

            val packet = ZvtCommandBuilder.buildRefund(
                amountInCents = amountInCents,
                currencyCode = config.currencyCode
            )
            log("=== REFUND (06 31) === amount=$amountInCents cents")
            val result = executeCommand(packet)
            result.copy(receiptLines = receiptLines.toList())
        }

    /**
     * Sends an End of Day command (06 50) to close the daily batch.
     *
     * @return [EndOfDayResult] with the batch close outcome.
     * @throws ZvtError on protocol or connection errors.
     */
    suspend fun endOfDay(): EndOfDayResult = withContext(Dispatchers.IO) {
        ensureConnected()
        receiptLines.clear()

        val packet = ZvtCommandBuilder.buildEndOfDay()
        log("=== END OF DAY (06 50) ===")
        val result = executeCommand(packet)

        EndOfDayResult(
            success = result.success,
            message = result.resultMessage,
            receiptLines = receiptLines.toList()
        )
    }

    /**
     * Sends a Diagnosis command (06 70) to query the terminal status.
     *
     * @return [DiagnosisResult] with the terminal diagnosis.
     * @throws ZvtError on protocol or connection errors.
     */
    suspend fun diagnosis(): DiagnosisResult = withContext(Dispatchers.IO) {
        ensureConnected()

        val packet = ZvtCommandBuilder.buildDiagnosis()
        log("=== DIAGNOSIS (06 70) ===")
        val result = executeCommand(packet)

        DiagnosisResult(
            success = result.success,
            status = TerminalStatus(
                isConnected = isConnected,
                terminalId = result.terminalId,
                statusMessage = result.resultMessage
            ),
            errorMessage = if (!result.success) result.resultMessage else ""
        )
    }

    /**
     * Sends a Status Enquiry command (05 01) to check the terminal state.
     *
     * @return [TerminalStatus] with the current terminal state.
     * @throws ZvtError on protocol or connection errors.
     */
    suspend fun statusEnquiry(): TerminalStatus = withContext(Dispatchers.IO) {
        ensureConnected()

        val packet = ZvtCommandBuilder.buildStatusEnquiry()
        log("=== STATUS ENQUIRY (05 01) ===")
        val result = executeCommand(packet)

        TerminalStatus(
            isConnected = isConnected,
            terminalId = result.terminalId,
            statusMessage = result.resultMessage
        )
    }

    /**
     * Sends a Pre-Authorization command (06 22) to reserve an amount.
     *
     * Used in hotel, car rental, and similar scenarios where the final
     * amount is not yet known. The reserved amount can later be completed
     * with [bookTotal] or cancelled with [reversal].
     *
     * @param amountInCents Amount to reserve in cents.
     * @return [TransactionResult] with the pre-authorization outcome (includes receipt number for book total).
     * @throws ZvtError on protocol or connection errors.
     */
    suspend fun preAuthorize(amountInCents: Long): TransactionResult =
        withContext(Dispatchers.IO) {
            ensureConnected()
            receiptLines.clear()

            val packet = ZvtCommandBuilder.buildPreAuthorization(
                amountInCents = amountInCents,
                currencyCode = config.currencyCode
            )
            log("=== PRE-AUTHORIZATION (06 22) === amount=$amountInCents cents")
            val result = executeCommand(packet)
            result.copy(receiptLines = receiptLines.toList())
        }

    /**
     * Sends a Book Total command (06 24) to complete a pre-authorization.
     *
     * @param amountInCents Final amount to book in cents.
     * @param receiptNumber Receipt number from the original pre-authorization.
     * @return [TransactionResult] with the book total outcome.
     * @throws ZvtError on protocol or connection errors.
     */
    suspend fun bookTotal(amountInCents: Long, receiptNumber: Int): TransactionResult =
        withContext(Dispatchers.IO) {
            ensureConnected()
            receiptLines.clear()

            val packet = ZvtCommandBuilder.buildBookTotal(
                amountInCents = amountInCents,
                receiptNumber = receiptNumber,
                currencyCode = config.currencyCode
            )
            log("=== BOOK TOTAL (06 24) === amount=$amountInCents cents, receipt=$receiptNumber")
            val result = executeCommand(packet)
            result.copy(receiptLines = receiptLines.toList())
        }

    /**
     * Sends a Partial Reversal command (06 25) to partially reverse a transaction.
     *
     * @param amountInCents Amount to partially reverse in cents.
     * @param receiptNumber Receipt number of the original transaction.
     * @return [TransactionResult] with the partial reversal outcome.
     * @throws ZvtError on protocol or connection errors.
     */
    suspend fun partialReversal(amountInCents: Long, receiptNumber: Int): TransactionResult =
        withContext(Dispatchers.IO) {
            ensureConnected()
            receiptLines.clear()

            val packet = ZvtCommandBuilder.buildPartialReversal(
                amountInCents = amountInCents,
                receiptNumber = receiptNumber,
                currencyCode = config.currencyCode
            )
            log("=== PARTIAL REVERSAL (06 25) === amount=$amountInCents cents, receipt=$receiptNumber")
            val result = executeCommand(packet)
            result.copy(receiptLines = receiptLines.toList())
        }

    /**
     * Sends a Repeat Receipt command (06 20) to re-print the last receipt.
     *
     * @return [TransactionResult] with the receipt data.
     * @throws ZvtError on protocol or connection errors.
     */
    suspend fun repeatReceipt(): TransactionResult =
        withContext(Dispatchers.IO) {
            ensureConnected()
            receiptLines.clear()

            val packet = ZvtCommandBuilder.buildRepeatReceipt()
            log("=== REPEAT RECEIPT (06 20) ===")
            val result = executeCommand(packet)
            result.copy(receiptLines = receiptLines.toList())
        }

    /**
     * Sends an Abort command (06 1E) to cancel an ongoing operation.
     *
     * @return `true` if the abort was sent successfully.
     */
    suspend fun abort(): Boolean = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext false

        val packet = ZvtCommandBuilder.buildAbort()
        log("=== ABORT (06 1E) ===")
        sendPacket(packet)
        true
    }

    /**
     * Sends a Log Off command (06 02) to disconnect from the terminal gracefully.
     *
     * @return `true` if the terminal acknowledged the log off.
     */
    suspend fun logOff(): Boolean = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext false

        val packet = ZvtCommandBuilder.buildLogOff()
        log("=== LOG OFF (06 02) ===")
        sendPacket(packet)

        // Wait for ACK
        val response = readPacket()
        val success = response?.isAck == true
        log("Log Off ${if (success) "acknowledged" else "not acknowledged"}")
        success
    }

    // =====================================================
    // Command Execution Engine
    // =====================================================

    /**
     * Sends a command and manages the full response loop.
     *
     * Protocol flow:
     * 1. Send command packet
     * 2. Wait for ACK from terminal
     * 3. Loop: Read terminal responses
     *    - `04 FF` (Intermediate Status) -> Send ACK, continue
     *    - `06 D1` (Print Line) -> Send ACK, continue
     *    - `04 0F` (Status Info) -> Send ACK, record result, continue
     *    - `06 0F` (Completion) -> Transaction finished
     *    - `06 1E` (Abort) -> Transaction aborted
     *
     * @param command The [ZvtPacket] command to execute.
     * @return [TransactionResult] with the final outcome.
     * @throws ZvtError on protocol or connection errors.
     */
    private suspend fun executeCommand(command: ZvtPacket): TransactionResult {
        // 1. Send command
        sendPacket(command)

        // 2. Wait for initial ACK
        val ackResponse = readPacket()
            ?: throw ZvtError.TimeoutError("ACK expected but no response received")

        if (ackResponse.isNack) {
            log("Terminal returned NACK - command rejected")
            throw ZvtError.ProtocolError("Terminal returned NACK")
        }

        if (!ackResponse.isAck) {
            // Some terminals send a direct response instead of ACK
            log("Direct response received instead of ACK: ${ZvtConstants.getCommandName(ackResponse.command)}")
            return handleResponse(ackResponse)
        }

        log("ACK received, entering response loop")

        // 3. Response loop
        var finalResult = TransactionResult(success = false, resultMessage = "No response received")
        var transactionComplete = false

        while (!transactionComplete) {
            val response = readPacket() ?: break

            when {
                // Intermediate Status (04 FF) - "Insert card", "Enter PIN", etc.
                response.isIntermediateStatus -> {
                    val status = ZvtResponseParser.parseIntermediateStatus(response)
                    _intermediateStatus.value = status
                    callback?.onIntermediateStatus(status)
                    log("Intermediate Status: [0x${String.format("%02X", status.statusCode)}] ${status.message}")

                    if (config.autoAck) sendAck()
                }

                // Print Line (06 D1) - Receipt text line
                response.isPrintLine -> {
                    val line = ZvtResponseParser.parsePrintLine(response)
                    if (line.isNotEmpty()) {
                        receiptLines.add(line)
                        callback?.onPrintLine(line)
                        log("Print Line: '$line'")
                    }

                    if (config.autoAck) sendAck()
                }

                // Status Information (04 0F) - Transaction result with BMP fields
                response.isStatusInfo -> {
                    finalResult = ZvtResponseParser.parseStatusInfo(response)
                    log("Status Info: success=${finalResult.success}, resultCode=0x${String.format("%02X", finalResult.resultCode)}, message='${finalResult.resultMessage}'")

                    if (config.autoAck) sendAck()
                }

                // Completion (06 0F) - Transaction finished
                response.isCompletion -> {
                    if (!finalResult.success && finalResult.resultMessage == "No response received") {
                        finalResult = ZvtResponseParser.parseCompletion(response)
                    }
                    log("Completion received -> transaction finished")
                    transactionComplete = true
                }

                // Abort (06 1E) - Transaction aborted by terminal
                response.isAbort -> {
                    finalResult = ZvtResponseParser.parseAbort(response)
                    log("Abort received: ${finalResult.resultMessage}")
                    transactionComplete = true
                }

                // Unexpected response
                else -> {
                    log("Unexpected response: ${ZvtConstants.getCommandName(response.command)}, data=${response.data.toHexString()}")
                    if (config.autoAck) sendAck()
                }
            }
        }

        _intermediateStatus.value = null
        log("=== Command completed: success=${finalResult.success}, message='${finalResult.resultMessage}' ===")
        return finalResult
    }

    /**
     * Handles a direct response received instead of an ACK.
     *
     * @param packet The response packet.
     * @return Parsed [TransactionResult].
     */
    private fun handleResponse(packet: ZvtPacket): TransactionResult {
        return when {
            packet.isCompletion -> ZvtResponseParser.parseCompletion(packet)
            packet.isStatusInfo -> ZvtResponseParser.parseStatusInfo(packet)
            packet.isAbort -> ZvtResponseParser.parseAbort(packet)
            else -> TransactionResult(
                success = false,
                resultMessage = "Unexpected response: ${packet.commandHex}"
            )
        }
    }

    // =====================================================
    // TCP I/O Operations
    // =====================================================

    /**
     * Sends a ZVT packet over the TCP connection.
     *
     * The entire packet (command + length + data) is written and flushed.
     * Every sent byte is logged via Timber.
     *
     * @param packet The [ZvtPacket] to send.
     * @throws ZvtError.ConnectionError if the output stream is null (connection lost).
     */
    private fun sendPacket(packet: ZvtPacket) {
        synchronized(sendLock) {
            val bytes = packet.toBytes()
            val cmdName = ZvtConstants.getCommandName(packet.command)

            Timber.tag(TAG).d("ECR -> PT | TX %s | %d bytes | %s",
                cmdName, bytes.size, bytes.toHexString())

            // Also forward to callback
            callback?.onDebugLog(TAG, "ECR -> PT | TX $cmdName | ${bytes.size} bytes | ${bytes.toHexString()}")

            outputStream?.write(bytes)
            outputStream?.flush()
                ?: throw ZvtError.ConnectionError("OutputStream is null - connection may be lost")
        }
    }

    /**
     * Sends an ACK (positive acknowledgement) packet to the terminal.
     */
    private fun sendAck() {
        sendPacket(ZvtPacket.ack())
    }

    /**
     * Reads a complete ZVT packet from the TCP input stream.
     *
     * Reads the 3-byte header (CMD + LEN), then the data payload.
     * Every received byte is logged via Timber.
     *
     * @return The received [ZvtPacket], or `null` on timeout.
     * @throws ZvtError.ConnectionError if the connection is closed or an I/O error occurs.
     */
    private fun readPacket(): ZvtPacket? {
        try {
            val input = inputStream
                ?: throw ZvtError.ConnectionError("InputStream is null")

            // Read header (minimum 3 bytes: CMD(2) + LEN(1))
            val headerBytes = ByteArray(3)
            var totalRead = 0
            while (totalRead < 3) {
                val read = input.read(headerBytes, totalRead, 3 - totalRead)
                if (read == -1) throw ZvtError.ConnectionError("Connection closed (EOF)")
                totalRead += read
            }

            // ACK/NACK check (exactly 3 bytes)
            if (headerBytes[0] == 0x80.toByte() || headerBytes[0] == 0x84.toByte()) {
                val cmdName = if (headerBytes[0] == 0x80.toByte()) "ACK" else "NACK"
                Timber.tag(TAG).d("PT -> ECR | RX %s | 3 bytes | %s",
                    cmdName, headerBytes.toHexString())
                callback?.onDebugLog(TAG, "PT -> ECR | RX $cmdName | 3 bytes | ${headerBytes.toHexString()}")
                return ZvtPacket(headerBytes.copyOfRange(0, 3))
            }

            // Calculate data length
            val lenByte = headerBytes[2].toInt() and 0xFF
            val dataLength: Int
            val extraLenBytes: ByteArray

            if (lenByte == 0xFF) {
                // Extended length format: read 2 more bytes
                extraLenBytes = ByteArray(2)
                readFully(input, extraLenBytes)
                dataLength = (extraLenBytes[0].toInt() and 0xFF) or
                        ((extraLenBytes[1].toInt() and 0xFF) shl 8)
            } else {
                dataLength = lenByte
                extraLenBytes = byteArrayOf()
            }

            // Read data payload
            val data = if (dataLength > 0) {
                ByteArray(dataLength).also { readFully(input, it) }
            } else {
                byteArrayOf()
            }

            val packet = ZvtPacket(
                command = headerBytes.copyOfRange(0, 2),
                data = data
            )

            val fullBytes = packet.toBytes()
            val cmdName = ZvtConstants.getCommandName(packet.command)
            Timber.tag(TAG).d("PT -> ECR | RX %s | %d bytes | %s",
                cmdName, fullBytes.size, fullBytes.toHexString())
            callback?.onDebugLog(TAG, "PT -> ECR | RX $cmdName | ${fullBytes.size} bytes | ${fullBytes.toHexString()}")

            return packet

        } catch (e: SocketTimeoutException) {
            log("Read timeout (${config.readTimeoutMs}ms)")
            return null
        } catch (e: IOException) {
            log("Read error: ${e.message}")
            throw ZvtError.ConnectionError("Read error: ${e.message}", cause = e)
        }
    }

    /**
     * Reads exactly [buffer.size] bytes from the input stream.
     *
     * Blocks until all bytes are read or an error/EOF occurs.
     *
     * @param input The input stream to read from.
     * @param buffer The buffer to fill.
     * @throws ZvtError.ConnectionError if EOF is reached before the buffer is full.
     */
    private fun readFully(input: InputStream, buffer: ByteArray) {
        var offset = 0
        while (offset < buffer.size) {
            val read = input.read(buffer, offset, buffer.size - offset)
            if (read == -1) throw ZvtError.ConnectionError("Connection closed (EOF)")
            offset += read
        }
    }

    // =====================================================
    // Helper Functions
    // =====================================================

    /**
     * Ensures the client is connected to the terminal.
     *
     * @throws ZvtError.ConnectionError if not connected.
     */
    private fun ensureConnected() {
        if (!isConnected) {
            throw ZvtError.ConnectionError("Not connected to terminal")
        }
    }

    /**
     * Updates the connection state and notifies the callback.
     *
     * @param state The new [ConnectionState].
     */
    private fun updateState(state: ConnectionState) {
        _connectionState.value = state
        callback?.onConnectionStateChanged(state)
        Timber.tag(TAG).d("Connection state changed: %s", state.name)
    }

    /**
     * Logs a message via Timber and optionally forwards to the callback.
     *
     * Logging is always active regardless of [ZvtConfig.debugMode].
     * The callback is only invoked when [ZvtConfig.debugMode] is `true`.
     *
     * @param message The log message.
     */
    private fun log(message: String) {
        Timber.tag(TAG).d(message)
        if (config.debugMode) {
            callback?.onDebugLog(TAG, message)
        }
    }

    /**
     * Cleans up all resources (connection, coroutine scope).
     *
     * Call this when the client is no longer needed.
     */
    fun destroy() {
        disconnect()
        scope.cancel()
    }
}
