package com.panda.zvt_library

import com.panda.zvt_library.model.*
import com.panda.zvt_library.protocol.*
import com.panda.zvt_library.util.toHexString
import com.panda.zvt_library.util.toLogString
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

/**
 * ZVT TCP/IP Client
 *
 * Ödeme terminali ile TCP/IP üzerinden ZVT protokolü iletişimi sağlar.
 * Thread-safe ve coroutine tabanlı asenkron mimari kullanır.
 *
 * Temel Kullanım:
 * ```kotlin
 * val config = ZvtConfig(host = "192.168.1.100", port = 20007)
 * val client = ZvtClient(config)
 *
 * // Bağlan ve kayıt ol
 * client.connect()
 * client.register()
 *
 * // Ödeme yap
 * val result = client.authorize(1250) // 12.50 EUR
 *
 * // Bağlantıyı kapat
 * client.disconnect()
 * ```
 *
 * İşlem Akışı:
 * 1. ECR komut gönderir → Terminal ACK döner
 * 2. Terminal ara durum bilgileri gönderir (04 FF) → ECR ACK döner
 * 3. Terminal sonuç bilgisi gönderir (04 0F) → ECR ACK döner
 * 4. Terminal tamamlandı bilgisi gönderir (06 0F) → İşlem biter
 */
class ZvtClient(
    private val config: ZvtConfig
) {
    companion object {
        private const val TAG = "ZvtClient"
    }

    // =====================================================
    // State
    // =====================================================

    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _intermediateStatus = MutableStateFlow<IntermediateStatus?>(null)
    val intermediateStatus: StateFlow<IntermediateStatus?> = _intermediateStatus.asStateFlow()

    private var callback: ZvtCallback? = null
    private val receiptLines = mutableListOf<String>()
    private val readBuffer = ByteArray(ZvtConstants.READ_BUFFER_SIZE)

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sendLock = Any()

    // =====================================================
    // Konfigürasyon
    // =====================================================

    /**
     * Olay dinleyici ayarlar
     */
    fun setCallback(callback: ZvtCallback?) {
        this.callback = callback
    }

    // =====================================================
    // Bağlantı Yönetimi
    // =====================================================

    /**
     * Terminal'e TCP bağlantısı açar
     * @throws ZvtError.ConnectionError Bağlantı hatası
     */
    suspend fun connect() = withContext(Dispatchers.IO) {
        if (_connectionState.value == ConnectionState.CONNECTED ||
            _connectionState.value == ConnectionState.REGISTERED) {
            log("Zaten bağlı")
            return@withContext
        }

        try {
            updateState(ConnectionState.CONNECTING)
            log("Bağlanıyor: ${config.host}:${config.port}")

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
            log("Bağlantı başarılı")

        } catch (e: SocketTimeoutException) {
            updateState(ConnectionState.ERROR)
            throw ZvtError.TimeoutError("Bağlantı zaman aşımı: ${config.host}:${config.port}")
        } catch (e: IOException) {
            updateState(ConnectionState.ERROR)
            throw ZvtError.ConnectionError(
                "Bağlantı hatası: ${e.message}",
                cause = e
            )
        }
    }

    /**
     * Bağlantıyı kapatır
     */
    fun disconnect() {
        try {
            log("Bağlantı kapatılıyor")
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (e: Exception) {
            log("Kapatma hatası: ${e.message}")
        } finally {
            socket = null
            inputStream = null
            outputStream = null
            updateState(ConnectionState.DISCONNECTED)
        }
    }

    /**
     * Bağlantı durumunu kontrol eder
     */
    val isConnected: Boolean
        get() = socket?.isConnected == true && socket?.isClosed == false

    // =====================================================
    // ZVT Komutları
    // =====================================================

    /**
     * Terminal kayıt işlemi (Registration - 06 00)
     *
     * Terminal ile ilk iletişimde çağrılmalıdır. Terminal'in ECR'yi tanımasını sağlar.
     *
     * @param configByte Yapılandırma byte'ı
     * @return true: kayıt başarılı
     * @throws ZvtError İşlem hatası
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

        log("Registration gönderiliyor")
        val result = executeCommand(packet)

        if (result.success) {
            updateState(ConnectionState.REGISTERED)
            log("Registration başarılı")
        } else {
            updateState(ConnectionState.CONNECTED)
            log("Registration başarısız: ${result.resultMessage}")
        }

        result.success
    }

    /**
     * Ödeme işlemi (Authorization - 06 01)
     *
     * @param amountInCents Tutar (cent) - ör: 1250 = 12.50 EUR
     * @param paymentType Ödeme tipi (varsayılan: otomatik)
     * @return İşlem sonucu
     * @throws ZvtError İşlem hatası
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

        log("Authorization gönderiliyor: ${amountInCents} cent")
        val result = executeCommand(packet)
        result.copy(receiptLines = receiptLines.toList())
    }

    /**
     * Euro tutarı ile ödeme (kolay kullanım)
     */
    suspend fun authorizeEuro(
        amountEuro: Double,
        paymentType: Byte = ZvtConstants.PAY_TYPE_DEFAULT
    ): TransactionResult {
        return authorize((amountEuro * 100).toLong(), paymentType)
    }

    /**
     * İptal işlemi (Reversal - 06 30)
     *
     * @param receiptNumber İptal edilecek fiş numarası (opsiyonel)
     * @return İşlem sonucu
     */
    suspend fun reversal(receiptNumber: Int? = null): TransactionResult =
        withContext(Dispatchers.IO) {
            ensureConnected()
            receiptLines.clear()

            val packet = ZvtCommandBuilder.buildReversal(receiptNumber)
            log("Reversal gönderiliyor" + (receiptNumber?.let { " (Fiş: $it)" } ?: ""))
            val result = executeCommand(packet)
            result.copy(receiptLines = receiptLines.toList())
        }

    /**
     * İade işlemi (Refund - 06 31)
     *
     * @param amountInCents İade tutarı (cent)
     * @return İşlem sonucu
     */
    suspend fun refund(amountInCents: Long): TransactionResult =
        withContext(Dispatchers.IO) {
            ensureConnected()
            receiptLines.clear()

            val packet = ZvtCommandBuilder.buildRefund(
                amountInCents = amountInCents,
                currencyCode = config.currencyCode
            )
            log("Refund gönderiliyor: $amountInCents cent")
            val result = executeCommand(packet)
            result.copy(receiptLines = receiptLines.toList())
        }

    /**
     * Gün sonu işlemi (End of Day - 06 50)
     *
     * @return Gün sonu sonucu
     */
    suspend fun endOfDay(): EndOfDayResult = withContext(Dispatchers.IO) {
        ensureConnected()
        receiptLines.clear()

        val packet = ZvtCommandBuilder.buildEndOfDay()
        log("End of Day gönderiliyor")
        val result = executeCommand(packet)

        EndOfDayResult(
            success = result.success,
            message = result.resultMessage,
            receiptLines = receiptLines.toList()
        )
    }

    /**
     * Tanılama (Diagnosis - 06 70)
     *
     * @return Tanılama sonucu
     */
    suspend fun diagnosis(): DiagnosisResult = withContext(Dispatchers.IO) {
        ensureConnected()

        val packet = ZvtCommandBuilder.buildDiagnosis()
        log("Diagnosis gönderiliyor")
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
     * Durum sorgulama (Status Enquiry - 05 01)
     */
    suspend fun statusEnquiry(): TerminalStatus = withContext(Dispatchers.IO) {
        ensureConnected()

        val packet = ZvtCommandBuilder.buildStatusEnquiry()
        log("Status Enquiry gönderiliyor")
        val result = executeCommand(packet)

        TerminalStatus(
            isConnected = isConnected,
            terminalId = result.terminalId,
            statusMessage = result.resultMessage
        )
    }

    /**
     * Devam eden işlemi iptal et (Abort - 06 1E)
     */
    suspend fun abort(): Boolean = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext false

        val packet = ZvtCommandBuilder.buildAbort()
        log("Abort gönderiliyor")
        sendPacket(packet)
        true
    }

    /**
     * Terminal bağlantısını sonlandır (Log Off - 06 02)
     */
    suspend fun logOff(): Boolean = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext false

        val packet = ZvtCommandBuilder.buildLogOff()
        log("Log Off gönderiliyor")
        sendPacket(packet)

        // ACK bekle
        val response = readPacket()
        response?.isAck == true
    }

    // =====================================================
    // Komut Yürütme Motoru
    // =====================================================

    /**
     * Komut gönderir ve tam yanıt döngüsünü yönetir.
     *
     * Akış:
     * 1. Komutu gönder
     * 2. ACK bekle
     * 3. Döngü: Terminal yanıtlarını oku
     *    - 04 FF (Intermediate Status) → ACK gönder, devam et
     *    - 06 D1 (Print Line) → ACK gönder, devam et
     *    - 04 0F (Status Info) → ACK gönder, sonucu kaydet, devam et
     *    - 06 0F (Completion) → İşlem bitti
     *    - 06 1E (Abort) → İşlem iptal edildi
     */
    private suspend fun executeCommand(command: ZvtPacket): TransactionResult {
        // 1. Komutu gönder
        sendPacket(command)

        // 2. İlk ACK'ı bekle
        val ackResponse = readPacket()
            ?: throw ZvtError.TimeoutError("ACK beklendi, yanıt gelmedi")

        if (ackResponse.isNack) {
            throw ZvtError.ProtocolError("Terminal NACK döndü")
        }

        if (!ackResponse.isAck) {
            // Bazı terminaller direkt yanıt gönderir (ACK yerine)
            return handleResponse(ackResponse)
        }

        // 3. Yanıt döngüsü
        var finalResult = TransactionResult(success = false, resultMessage = "Yanıt alınamadı")
        var transactionComplete = false

        while (!transactionComplete) {
            val response = readPacket() ?: break

            when {
                // Ara durum (04 FF) - "Kart bekleniyor", "PIN girişi" vs.
                response.isIntermediateStatus -> {
                    val status = ZvtResponseParser.parseIntermediateStatus(response)
                    _intermediateStatus.value = status
                    callback?.onIntermediateStatus(status)
                    log("Intermediate: ${status.message}")

                    if (config.autoAck) sendAck()
                }

                // Yazdırma satırı (06 D1)
                response.isPrintLine -> {
                    val line = ZvtResponseParser.parsePrintLine(response)
                    if (line.isNotEmpty()) {
                        receiptLines.add(line)
                        callback?.onPrintLine(line)
                        log("Print: $line")
                    }

                    if (config.autoAck) sendAck()
                }

                // Durum bilgisi (04 0F) - İşlem sonucu
                response.isStatusInfo -> {
                    finalResult = ZvtResponseParser.parseStatusInfo(response)
                    log("Status Info: ${finalResult.resultMessage}")

                    if (config.autoAck) sendAck()
                }

                // Tamamlandı (06 0F)
                response.isCompletion -> {
                    if (!finalResult.success && finalResult.resultMessage == "Yanıt alınamadı") {
                        finalResult = ZvtResponseParser.parseCompletion(response)
                    }
                    log("Completion alındı")
                    transactionComplete = true
                }

                // İptal (06 1E)
                response.isAbort -> {
                    finalResult = ZvtResponseParser.parseAbort(response)
                    log("Abort alındı: ${finalResult.resultMessage}")
                    transactionComplete = true
                }

                // Beklenmeyen yanıt
                else -> {
                    log("Beklenmeyen yanıt: ${response.commandHex}")
                    if (config.autoAck) sendAck()
                }
            }
        }

        _intermediateStatus.value = null
        return finalResult
    }

    /**
     * ACK yerine doğrudan gelen yanıtı işler
     */
    private fun handleResponse(packet: ZvtPacket): TransactionResult {
        return when {
            packet.isCompletion -> ZvtResponseParser.parseCompletion(packet)
            packet.isStatusInfo -> ZvtResponseParser.parseStatusInfo(packet)
            packet.isAbort -> ZvtResponseParser.parseAbort(packet)
            else -> TransactionResult(
                success = false,
                resultMessage = "Beklenmeyen yanıt: ${packet.commandHex}"
            )
        }
    }

    // =====================================================
    // TCP I/O İşlemleri
    // =====================================================

    /**
     * ZVT paketi gönderir
     */
    private fun sendPacket(packet: ZvtPacket) {
        synchronized(sendLock) {
            val bytes = packet.toBytes()
            log("TX → ${bytes.toLogString()}")

            outputStream?.write(bytes)
            outputStream?.flush()
                ?: throw ZvtError.ConnectionError("OutputStream null - bağlantı kopmuş olabilir")
        }
    }

    /**
     * ACK paketi gönderir
     */
    private fun sendAck() {
        sendPacket(ZvtPacket.ack())
    }

    /**
     * Bir ZVT paketi okur
     * @return Okunan paket veya null (timeout)
     */
    private fun readPacket(): ZvtPacket? {
        try {
            val input = inputStream
                ?: throw ZvtError.ConnectionError("InputStream null")

            // Header'ı oku (en az 3 byte: CMD(2) + LEN(1))
            val headerBytes = ByteArray(3)
            var totalRead = 0
            while (totalRead < 3) {
                val read = input.read(headerBytes, totalRead, 3 - totalRead)
                if (read == -1) throw ZvtError.ConnectionError("Bağlantı kapandı")
                totalRead += read
            }

            // ACK/NACK kontrolü (sadece 3 byte)
            if (headerBytes[0] == 0x80.toByte() || headerBytes[0] == 0x84.toByte()) {
                log("RX ← ${headerBytes.toHexString()}")
                return ZvtPacket(headerBytes.copyOfRange(0, 3))
            }

            // Uzunluk hesapla
            val lenByte = headerBytes[2].toInt() and 0xFF
            val dataLength: Int
            val extraLenBytes: ByteArray

            if (lenByte == 0xFF) {
                // Uzun format: 2 byte daha oku
                extraLenBytes = ByteArray(2)
                readFully(input, extraLenBytes)
                dataLength = (extraLenBytes[0].toInt() and 0xFF) or
                        ((extraLenBytes[1].toInt() and 0xFF) shl 8)
            } else {
                dataLength = lenByte
                extraLenBytes = byteArrayOf()
            }

            // Data oku
            val data = if (dataLength > 0) {
                ByteArray(dataLength).also { readFully(input, it) }
            } else {
                byteArrayOf()
            }

            val packet = ZvtPacket(
                command = headerBytes.copyOfRange(0, 2),
                data = data
            )

            log("RX ← ${packet.toBytes().toLogString()}")
            return packet

        } catch (e: SocketTimeoutException) {
            log("Okuma timeout")
            return null
        } catch (e: IOException) {
            log("Okuma hatası: ${e.message}")
            throw ZvtError.ConnectionError("Okuma hatası: ${e.message}", cause = e)
        }
    }

    /**
     * InputStream'den belirtilen sayıda byte'ı tamamen okur
     */
    private fun readFully(input: InputStream, buffer: ByteArray) {
        var offset = 0
        while (offset < buffer.size) {
            val read = input.read(buffer, offset, buffer.size - offset)
            if (read == -1) throw ZvtError.ConnectionError("Bağlantı kapandı (EOF)")
            offset += read
        }
    }

    // =====================================================
    // Yardımcı Fonksiyonlar
    // =====================================================

    private fun ensureConnected() {
        if (!isConnected) {
            throw ZvtError.ConnectionError("Terminal'e bağlı değil")
        }
    }

    private fun updateState(state: ConnectionState) {
        _connectionState.value = state
        callback?.onConnectionStateChanged(state)
    }

    private fun log(message: String) {
        if (config.debugMode) {
            callback?.onDebugLog(TAG, message)
        }
    }

    /**
     * Kaynakları temizler
     */
    fun destroy() {
        disconnect()
        scope.cancel()
    }
}
