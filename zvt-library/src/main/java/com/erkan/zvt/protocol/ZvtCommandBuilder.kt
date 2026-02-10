package com.erkan.zvt.protocol

import com.erkan.zvt.util.BcdHelper
import com.erkan.zvt.util.TlvParser

/**
 * ZVT Komut Oluşturucu
 *
 * ECR → Terminal yönünde gönderilecek tüm komut paketlerini oluşturur.
 * Her metod bir ZvtPacket döner.
 */
object ZvtCommandBuilder {

    // =====================================================
    // Registration (06 00)
    // =====================================================

    /** TLV tag 26: List of permitted ZVT commands (strongly recommended) */
    private const val TLV_TAG_PERMITTED_COMMANDS = 0x26

    /**
     * Terminal kayıt komutu oluşturur.
     * Terminal ile ilk bağlantıda gönderilir.
     *
     * @param password Terminal şifresi (genellikle "000000" - 3 byte BCD)
     * @param configByte Yapılandırma byte'ı (bitmask, hangi taraf fiş yazdıracak vs.)
     * @param currencyCode ISO 4217 para birimi kodu (varsayılan EUR: 978)
     */
    fun buildRegistration(
        password: String = "000000",
        configByte: Byte = ZvtConstants.REG_INTERMEDIATE_STATUS,
        currencyCode: Int = ZvtConstants.CURRENCY_EUR
    ): ZvtPacket {
        val data = mutableListOf<Byte>()

        // Password (3 byte BCD)
        data.addAll(BcdHelper.stringToBcd(password.padStart(6, '0')).toList())

        // Config byte
        data.add(configByte)

        // Currency code (BMP 0x49, CC tag)
        data.add(ZvtConstants.BMP_CURRENCY_CODE)
        data.addAll(BcdHelper.currencyToBcd(currencyCode).toList())

        // Service byte (BMP 0x03)
        data.add(ZvtConstants.BMP_SERVICE_BYTE)
        data.add(0x00) // Service byte value

        // TLV container (BMP 0x06) with list of permitted commands (tag 26)
        // Strongly recommended per ZVT spec v13.13
        val permittedCommands = buildPermittedCommandsList()
        val tlvData = TlvParser.buildTlv(TLV_TAG_PERMITTED_COMMANDS, permittedCommands)

        data.add(ZvtConstants.BMP_TLV_CONTAINER)
        // BER-TLV length encoding
        val tlvLen = tlvData.size
        if (tlvLen <= 0x7F) {
            data.add(tlvLen.toByte())
        } else {
            data.add(0x81.toByte())
            data.add(tlvLen.toByte())
        }
        data.addAll(tlvData.toList())

        return ZvtPacket(
            command = ZvtConstants.CMD_REGISTRATION,
            data = data.toByteArray()
        )
    }

    /**
     * Desteklenen ZVT komutlarının listesini oluşturur (tag 26 içeriği).
     * Her komut 2 byte: [CLASS][INSTR]
     */
    private fun buildPermittedCommandsList(): ByteArray {
        val commands = mutableListOf<Byte>()
        // Status Info (04 0F)
        commands.add(0x04); commands.add(0x0F)
        // Intermediate Status (04 FF)
        commands.add(0x04); commands.add(0xFF.toByte())
        // Completion (06 0F)
        commands.add(0x06); commands.add(0x0F)
        // Abort (06 1E)
        commands.add(0x06); commands.add(0x1E)
        // Print Line (06 D1)
        commands.add(0x06); commands.add(0xD1.toByte())
        // Print Text-Block (06 D3)
        commands.add(0x06); commands.add(0xD3.toByte())
        return commands.toByteArray()
    }

    // =====================================================
    // Authorization / Ödeme (06 01)
    // =====================================================

    /**
     * Ödeme komutu oluşturur.
     *
     * @param amountInCents Tutar (cent cinsinden) - ör: 1250 = 12.50 EUR
     * @param paymentType Ödeme tipi (varsayılan: otomatik algılama)
     * @param currencyCode Para birimi
     */
    fun buildAuthorization(
        amountInCents: Long,
        paymentType: Byte = ZvtConstants.PAY_TYPE_DEFAULT,
        currencyCode: Int = ZvtConstants.CURRENCY_EUR
    ): ZvtPacket {
        require(amountInCents > 0) { "Tutar 0'dan büyük olmalı" }

        val data = mutableListOf<Byte>()

        // Amount (BMP 0x04, 6 byte BCD)
        data.add(ZvtConstants.BMP_AMOUNT)
        data.addAll(BcdHelper.amountToBcd(amountInCents).toList())

        // Payment type (BMP 0x19)
        if (paymentType != ZvtConstants.PAY_TYPE_DEFAULT) {
            data.add(ZvtConstants.BMP_PAYMENT_TYPE)
            data.add(paymentType)
        }

        // Currency code (BMP 0x49)
        data.add(ZvtConstants.BMP_CURRENCY_CODE)
        data.addAll(BcdHelper.currencyToBcd(currencyCode).toList())

        return ZvtPacket(
            command = ZvtConstants.CMD_AUTHORIZATION,
            data = data.toByteArray()
        )
    }

    /**
     * Euro tutarı ile ödeme komutu (kolay kullanım)
     */
    fun buildAuthorization(
        amountEuro: Double,
        paymentType: Byte = ZvtConstants.PAY_TYPE_DEFAULT,
        currencyCode: Int = ZvtConstants.CURRENCY_EUR
    ): ZvtPacket {
        val cents = (amountEuro * 100).toLong()
        return buildAuthorization(cents, paymentType, currencyCode)
    }

    // =====================================================
    // Reversal / İptal (06 30)
    // =====================================================

    /**
     * İptal komutu oluşturur.
     * Bir önceki işlemi iptal eder.
     *
     * @param receiptNumber İptal edilecek fiş numarası (BCD 2 byte)
     */
    fun buildReversal(receiptNumber: Int? = null): ZvtPacket {
        val data = mutableListOf<Byte>()

        // Password (3 byte BCD - "000000")
        data.addAll(BcdHelper.stringToBcd("000000").toList())

        // Receipt number (opsiyonel)
        if (receiptNumber != null) {
            data.add(ZvtConstants.BMP_RECEIPT_NR)
            data.addAll(BcdHelper.stringToBcd(receiptNumber.toString().padStart(4, '0')).toList())
        }

        return ZvtPacket(
            command = ZvtConstants.CMD_REVERSAL,
            data = data.toByteArray()
        )
    }

    // =====================================================
    // Refund / İade (06 31)
    // =====================================================

    /**
     * İade komutu oluşturur.
     *
     * @param amountInCents İade tutarı (cent)
     * @param currencyCode Para birimi
     */
    fun buildRefund(
        amountInCents: Long,
        currencyCode: Int = ZvtConstants.CURRENCY_EUR
    ): ZvtPacket {
        require(amountInCents > 0) { "İade tutarı 0'dan büyük olmalı" }

        val data = mutableListOf<Byte>()

        // Password (3 byte BCD)
        data.addAll(BcdHelper.stringToBcd("000000").toList())

        // Amount (BMP 0x04, 6 byte BCD)
        data.add(ZvtConstants.BMP_AMOUNT)
        data.addAll(BcdHelper.amountToBcd(amountInCents).toList())

        // Currency code (BMP 0x49)
        data.add(ZvtConstants.BMP_CURRENCY_CODE)
        data.addAll(BcdHelper.currencyToBcd(currencyCode).toList())

        return ZvtPacket(
            command = ZvtConstants.CMD_REFUND,
            data = data.toByteArray()
        )
    }

    // =====================================================
    // End of Day / Gün Sonu (06 50)
    // =====================================================

    /**
     * Gün sonu komutu oluşturur.
     * Günlük işlemleri kapatır.
     */
    fun buildEndOfDay(): ZvtPacket {
        val data = mutableListOf<Byte>()

        // Password (3 byte BCD)
        data.addAll(BcdHelper.stringToBcd("000000").toList())

        return ZvtPacket(
            command = ZvtConstants.CMD_END_OF_DAY,
            data = data.toByteArray()
        )
    }

    // =====================================================
    // Diagnosis / Tanılama (06 70)
    // =====================================================

    /**
     * Tanılama komutu oluşturur.
     * Terminal durumunu sorgular.
     */
    fun buildDiagnosis(): ZvtPacket {
        return ZvtPacket(
            command = ZvtConstants.CMD_DIAGNOSIS,
            data = byteArrayOf()
        )
    }

    // =====================================================
    // Status Enquiry / Durum Sorgulama (05 01)
    // =====================================================

    /**
     * Terminal durum sorgulama komutu
     */
    fun buildStatusEnquiry(): ZvtPacket {
        return ZvtPacket(
            command = ZvtConstants.CMD_STATUS_ENQUIRY,
            data = byteArrayOf()
        )
    }

    // =====================================================
    // Abort / Abbruch (06 1E)
    // =====================================================

    /**
     * Devam eden işlemi iptal etme komutu
     */
    fun buildAbort(): ZvtPacket {
        return ZvtPacket(
            command = ZvtConstants.CMD_ABORT,
            data = byteArrayOf()
        )
    }

    // =====================================================
    // Log Off (06 02)
    // =====================================================

    /**
     * Terminal bağlantısını sonlandırma komutu
     */
    fun buildLogOff(): ZvtPacket {
        return ZvtPacket(
            command = ZvtConstants.CMD_LOG_OFF,
            data = byteArrayOf()
        )
    }

    // =====================================================
    // Pre-Authorization (06 22)
    // =====================================================

    /**
     * Ön yetkilendirme komutu (otel, araç kiralama vs.)
     *
     * @param amountInCents Bloke tutarı (cent)
     * @param currencyCode Para birimi
     */
    fun buildPreAuthorization(
        amountInCents: Long,
        currencyCode: Int = ZvtConstants.CURRENCY_EUR
    ): ZvtPacket {
        require(amountInCents > 0) { "Tutar 0'dan büyük olmalı" }

        val data = mutableListOf<Byte>()

        // Amount (BMP 0x04, 6 byte BCD)
        data.add(ZvtConstants.BMP_AMOUNT)
        data.addAll(BcdHelper.amountToBcd(amountInCents).toList())

        // Currency (BMP 0x49)
        data.add(ZvtConstants.BMP_CURRENCY_CODE)
        data.addAll(BcdHelper.currencyToBcd(currencyCode).toList())

        return ZvtPacket(
            command = ZvtConstants.CMD_PRE_AUTHORIZATION,
            data = data.toByteArray()
        )
    }

    // =====================================================
    // Print Line (06 D1) - Terminal'den gelen print'e yanıt
    // =====================================================

    /**
     * Print satırı alındı onayı
     */
    fun buildPrintLineAck(): ZvtPacket {
        return ZvtPacket.ack()
    }
}
