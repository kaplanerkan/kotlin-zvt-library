package com.panda.zvt_library.model

/**
 * ZVT bağlantı ayarları
 */
data class ZvtConfig(
    /** Terminal IP adresi */
    val host: String,
    /** Terminal port numarası (varsayılan: 20007) */
    val port: Int = 20007,
    /** Bağlantı timeout (ms) */
    val connectTimeoutMs: Int = 10_000,
    /** Okuma timeout (ms) */
    val readTimeoutMs: Int = 90_000,
    /** Terminal şifresi (6 haneli BCD) */
    val password: String = "000000",
    /** Para birimi kodu (ISO 4217) */
    val currencyCode: Int = 978,
    /** Otomatik ACK gönder */
    val autoAck: Boolean = true,
    /** Debug log aktif */
    val debugMode: Boolean = false
)

/**
 * İşlem sonucu
 */
data class TransactionResult(
    /** İşlem başarılı mı */
    val success: Boolean,
    /** Sonuç kodu (BMP 0x27) */
    val resultCode: Byte = 0x00,
    /** Sonuç mesajı */
    val resultMessage: String = "",
    /** Tutar (cent) */
    val amountInCents: Long = 0,
    /** Kart bilgileri */
    val cardData: CardData? = null,
    /** Fiş numarası */
    val receiptNumber: Int = 0,
    /** Trace numarası */
    val traceNumber: Int = 0,
    /** Terminal ID */
    val terminalId: String = "",
    /** VU numarası */
    val vuNumber: String = "",
    /** İşlem tarihi (MMDD) */
    val date: String = "",
    /** İşlem saati (HHMMSS) */
    val time: String = "",
    /** Orijinal trace numarası (reversal için, BMP 0x37) */
    val originalTrace: Int = 0,
    /** Ciro/turnover numarası (BMP 0x88) */
    val turnoverNumber: Int = 0,
    /** Fiş satırları (terminal'den gelen print verileri) */
    val receiptLines: List<String> = emptyList(),
    /** Ham yanıt verisi (debug amaçlı) */
    val rawData: ByteArray? = null
) {
    /** Tutarı Euro formatında döner: "12,50" */
    val amountFormatted: String
        get() {
            val euros = amountInCents / 100
            val cents = amountInCents % 100
            return String.format("%d,%02d €", euros, cents)
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
 * Kart bilgileri
 */
data class CardData(
    /** Kart numarası (maskeli) - ör: "****1234" */
    val maskedPan: String = "",
    /** Kart tipi - ör: "VISA", "MASTERCARD" */
    val cardType: String = "",
    /** Kart adı / Application label */
    val cardName: String = "",
    /** Son kullanma tarihi (YYMM) */
    val expiryDate: String = "",
    /** Kart sıra numarası */
    val sequenceNumber: Int = 0,
    /** AID (Application Identifier) */
    val aid: String = ""
)

/**
 * Terminal durumu
 */
data class TerminalStatus(
    /** Terminal bağlı mı */
    val isConnected: Boolean,
    /** Terminal ID */
    val terminalId: String = "",
    /** Yazılım versiyonu */
    val softwareVersion: String = "",
    /** Terminal modeli */
    val terminalModel: String = "",
    /** Durum mesajı */
    val statusMessage: String = "",
    /** Son işlem zamanı */
    val lastTransactionTime: String = ""
)

/**
 * Tanılama sonucu
 */
data class DiagnosisResult(
    /** Başarılı mı */
    val success: Boolean,
    /** Terminal durumu */
    val status: TerminalStatus = TerminalStatus(false),
    /** Hata mesajı (varsa) */
    val errorMessage: String = ""
)

/**
 * Gün sonu sonucu
 */
data class EndOfDayResult(
    /** Başarılı mı */
    val success: Boolean,
    /** Toplam işlem sayısı */
    val transactionCount: Int = 0,
    /** Toplam tutar (cent) */
    val totalAmountInCents: Long = 0,
    /** Sonuç mesajı */
    val message: String = "",
    /** Fiş satırları */
    val receiptLines: List<String> = emptyList()
)

/**
 * ZVT hata sınıfı
 */
sealed class ZvtError : Exception() {
    /** Bağlantı hatası */
    data class ConnectionError(
        override val message: String,
        override val cause: Throwable? = null
    ) : ZvtError()

    /** Timeout hatası */
    data class TimeoutError(
        override val message: String = "İşlem zaman aşımına uğradı"
    ) : ZvtError()

    /** Protokol hatası */
    data class ProtocolError(
        override val message: String,
        val rawData: ByteArray? = null
    ) : ZvtError()

    /** Terminal hatası */
    data class TerminalError(
        val resultCode: Byte,
        override val message: String
    ) : ZvtError()

    /** İşlem reddedildi */
    data class TransactionDeclined(
        val resultCode: Byte,
        override val message: String,
        val cardData: CardData? = null
    ) : ZvtError()
}

/**
 * ZVT bağlantı durumu
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
 * İşlem tipi
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
 * Ara durum bilgisi (04 FF)
 */
data class IntermediateStatus(
    /** Durum kodu */
    val statusCode: Byte,
    /** Okunabilir mesaj */
    val message: String,
    /** Zaman damgası */
    val timestamp: Long = System.currentTimeMillis()
)
