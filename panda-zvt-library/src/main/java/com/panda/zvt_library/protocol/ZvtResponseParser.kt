package com.panda.zvt_library.protocol

import com.panda.zvt_library.model.*
import com.panda.zvt_library.util.BcdHelper
import com.panda.zvt_library.util.TlvParser
import com.panda.zvt_library.util.toHexString
import com.panda.zvt_library.util.toSafeAscii
import com.panda.zvt_library.util.toUnsignedInt

/**
 * ZVT Yanıt Ayrıştırıcı
 *
 * Terminal'den gelen yanıt paketlerini model nesnelerine dönüştürür.
 * Status Info (04 0F) ve Completion (06 0F) yanıtlarındaki BMP alanlarını okur.
 */
object ZvtResponseParser {

    /**
     * Status Info (04 0F) paketinden TransactionResult oluşturur
     */
    fun parseStatusInfo(packet: ZvtPacket): TransactionResult {
        return parseBmpData(packet.data)
    }

    /**
     * Intermediate Status (04 FF) paketini okur
     */
    fun parseIntermediateStatus(packet: ZvtPacket): IntermediateStatus {
        val statusCode = if (packet.data.isNotEmpty()) packet.data[0] else 0x00
        return IntermediateStatus(
            statusCode = statusCode,
            message = ZvtConstants.getIntermediateStatusMessage(statusCode)
        )
    }

    /**
     * Completion (06 0F) paketini okur (genellikle data boş olur)
     */
    fun parseCompletion(packet: ZvtPacket): TransactionResult {
        return if (packet.data.isEmpty()) {
            TransactionResult(success = true, resultMessage = "İşlem tamamlandı")
        } else {
            parseBmpData(packet.data)
        }
    }

    /**
     * Abort (06 1E) paketini okur
     */
    fun parseAbort(packet: ZvtPacket): TransactionResult {
        val resultCode = if (packet.data.isNotEmpty()) packet.data[0] else ZvtConstants.RC_SYSTEM_ERROR
        return TransactionResult(
            success = false,
            resultCode = resultCode,
            resultMessage = ZvtConstants.getResultMessage(resultCode)
        )
    }

    /**
     * Print Line (06 D1) paketinden metin satırı okur
     */
    fun parsePrintLine(packet: ZvtPacket): String {
        if (packet.data.isEmpty()) return ""
        // İlk byte attribute, geri kalanı text
        return if (packet.data.size > 1) {
            packet.data.copyOfRange(1, packet.data.size).toSafeAscii().trim()
        } else {
            ""
        }
    }

    // =====================================================
    // BMP Data Parser
    // =====================================================

    /**
     * BMP alanlarını içeren veri bloğunu ayrıştırır.
     *
     * BMP formatı (ZVT Spec v13.13):
     * - Fixed length: [TAG(1)] [DATA(n)] — uzunluk tag'e göre sabit
     * - LLVAR: [TAG(1)] [LEN(2 byte FxFy)] [DATA(n)] — FxFy = nibble coded length
     * - LLLVAR: [TAG(1)] [LEN(3 byte FxFyFz)] [DATA(n)] — FxFyFz = nibble coded length
     */
    private fun parseBmpData(data: ByteArray): TransactionResult {
        var success = true
        var resultCode: Byte = ZvtConstants.RC_SUCCESS
        var amount: Long = 0
        var traceNumber = 0
        var receiptNumber = 0
        var turnoverNumber = 0
        var terminalId = ""
        var vuNumber = ""
        var date = ""
        var time = ""
        var maskedPan = ""
        var cardType = ""
        var cardName = ""
        var expiryDate = ""
        var sequenceNumber = 0
        var aid = ""
        var paymentType: Byte = 0
        var originalTrace = 0
        var resultCodeAs: Byte = 0

        var offset = 0

        while (offset < data.size) {
            val tag = data[offset]
            offset++

            try {
                when (tag) {
                    // Service byte - 1 byte
                    ZvtConstants.BMP_SERVICE_BYTE -> {
                        if (offset < data.size) {
                            offset++ // read and skip
                        }
                    }

                    // Tutar - 6 byte BCD
                    ZvtConstants.BMP_AMOUNT -> {
                        if (offset + 6 <= data.size) {
                            amount = BcdHelper.bcdToAmount(data.copyOfRange(offset, offset + 6))
                            offset += 6
                        }
                    }

                    // TLV container via BMP 06 (BER-TLV length encoding)
                    ZvtConstants.BMP_TLV_CONTAINER -> {
                        if (offset < data.size) {
                            val (len, bytesRead) = readBerTlvLength(data, offset)
                            offset += bytesRead
                            if (offset + len <= data.size) {
                                val tlvData = data.copyOfRange(offset, offset + len)
                                parseTlvEntries(tlvData).let { entries ->
                                    entries.firstOrNull { it.tag == ZvtConstants.TLV_CARD_NAME }?.let {
                                        cardName = it.valueAscii
                                    }
                                }
                                offset += len
                            }
                        }
                    }

                    // Trace numarası - 3 byte BCD
                    ZvtConstants.BMP_TRACE_NUMBER -> {
                        if (offset + 3 <= data.size) {
                            traceNumber = BcdHelper.bcdToTraceNumber(data.copyOfRange(offset, offset + 3))
                            offset += 3
                        }
                    }

                    // Saat - 3 byte BCD (HHMMSS)
                    ZvtConstants.BMP_TIME -> {
                        if (offset + 3 <= data.size) {
                            time = BcdHelper.bcdToString(data.copyOfRange(offset, offset + 3))
                            offset += 3
                        }
                    }

                    // Tarih - 2 byte BCD (MMDD)
                    ZvtConstants.BMP_DATE -> {
                        if (offset + 2 <= data.size) {
                            date = BcdHelper.bcdToString(data.copyOfRange(offset, offset + 2))
                            offset += 2
                        }
                    }

                    // Son kullanma tarihi - 2 byte BCD (YYMM)
                    ZvtConstants.BMP_EXPIRY_DATE -> {
                        if (offset + 2 <= data.size) {
                            expiryDate = BcdHelper.bcdToString(data.copyOfRange(offset, offset + 2))
                            offset += 2
                        }
                    }

                    // Kart sıra numarası - 2 byte BCD
                    ZvtConstants.BMP_CARD_SEQUENCE_NR -> {
                        if (offset + 2 <= data.size) {
                            sequenceNumber = BcdHelper.bcdToString(data.copyOfRange(offset, offset + 2)).toIntOrNull() ?: 0
                            offset += 2
                        }
                    }

                    // Ödeme tipi - 1 byte
                    ZvtConstants.BMP_PAYMENT_TYPE -> {
                        if (offset < data.size) {
                            paymentType = data[offset]
                            offset++
                        }
                    }

                    // PAN/EF_ID - LLVAR BCD (2 byte FxFy counter)
                    ZvtConstants.BMP_CARD_NUMBER -> {
                        if (offset + 2 <= data.size) {
                            val len = decodeLlvar(data, offset)
                            offset += 2
                            if (offset + len <= data.size) {
                                maskedPan = BcdHelper.bcdToString(data.copyOfRange(offset, offset + len))
                                offset += len
                            }
                        }
                    }

                    // Sonuç kodu - 1 byte
                    ZvtConstants.BMP_RESULT_CODE -> {
                        if (offset < data.size) {
                            resultCode = data[offset]
                            success = resultCode == ZvtConstants.RC_SUCCESS
                            offset++
                        }
                    }

                    // Terminal ID - 4 byte BCD
                    ZvtConstants.BMP_TERMINAL_ID -> {
                        if (offset + 4 <= data.size) {
                            terminalId = BcdHelper.bcdToString(data.copyOfRange(offset, offset + 4))
                            offset += 4
                        }
                    }

                    // VU numarası - 15 byte ASCII (fixed)
                    ZvtConstants.BMP_VU_NUMBER -> {
                        if (offset + 15 <= data.size) {
                            vuNumber = data.copyOfRange(offset, offset + 15).toSafeAscii().trim()
                            offset += 15
                        }
                    }

                    // Original trace numarası - 3 byte BCD (reversal only)
                    ZvtConstants.BMP_ORIGINAL_TRACE -> {
                        if (offset + 3 <= data.size) {
                            originalTrace = BcdHelper.bcdToTraceNumber(data.copyOfRange(offset, offset + 3))
                            offset += 3
                        }
                    }

                    // AID - 8 byte fixed
                    ZvtConstants.BMP_AID -> {
                        if (offset + 8 <= data.size) {
                            aid = data.copyOfRange(offset, offset + 8).toHexString("")
                            offset += 8
                        }
                    }

                    // Additional data / TLV container - LLLVAR (3 byte FxFyFz counter)
                    ZvtConstants.BMP_ADDITIONAL_DATA -> {
                        if (offset + 3 <= data.size) {
                            val len = decodeLllvar(data, offset)
                            offset += 3
                            if (offset + len <= data.size) {
                                val tlvData = data.copyOfRange(offset, offset + len)
                                parseTlvEntries(tlvData).let { entries ->
                                    entries.firstOrNull { it.tag == ZvtConstants.TLV_CARD_NAME }?.let {
                                        cardName = it.valueAscii
                                    }
                                }
                                offset += len
                            }
                        }
                    }

                    // Para birimi kodu - 2 byte BCD
                    ZvtConstants.BMP_CURRENCY_CODE -> {
                        if (offset + 2 <= data.size) {
                            offset += 2
                        }
                    }

                    // Blocked goods groups - LLVAR
                    ZvtConstants.BMP_BLOCKED_GOODS_GROUPS -> {
                        if (offset + 2 <= data.size) {
                            val len = decodeLlvar(data, offset)
                            offset += 2
                            if (offset + len <= data.size) {
                                offset += len // skip
                            }
                        }
                    }

                    // Single amounts End-of-Day - LLLVAR
                    ZvtConstants.BMP_SINGLE_AMOUNTS -> {
                        if (offset + 3 <= data.size) {
                            val len = decodeLllvar(data, offset)
                            offset += 3
                            if (offset + len <= data.size) {
                                offset += len // skip for now
                            }
                        }
                    }

                    // Fiş numarası - 2 byte BCD
                    ZvtConstants.BMP_RECEIPT_NR -> {
                        if (offset + 2 <= data.size) {
                            receiptNumber = BcdHelper.bcdToString(data.copyOfRange(offset, offset + 2)).toIntOrNull() ?: 0
                            offset += 2
                        }
                    }

                    // Ciro/turnover numarası - 3 byte BCD
                    ZvtConstants.BMP_TURNOVER_NR -> {
                        if (offset + 3 <= data.size) {
                            turnoverNumber = BcdHelper.bcdToTraceNumber(data.copyOfRange(offset, offset + 3))
                            offset += 3
                        }
                    }

                    // Kart tipi - 1 byte
                    ZvtConstants.BMP_CARD_TYPE -> {
                        if (offset < data.size) {
                            cardType = resolveCardType(data[offset])
                            offset++
                        }
                    }

                    // Kart adı - LLVAR ASCII null-terminated
                    ZvtConstants.BMP_CARD_NAME -> {
                        if (offset + 2 <= data.size) {
                            val len = decodeLlvar(data, offset)
                            offset += 2
                            if (offset + len <= data.size) {
                                cardName = data.copyOfRange(offset, offset + len)
                                    .toSafeAscii().trimEnd('\u0000').trim()
                                offset += len
                            }
                        }
                    }

                    // Kart tipi ID network - 1 byte
                    ZvtConstants.BMP_CARD_TYPE_ID -> {
                        if (offset < data.size) {
                            offset++ // read and skip
                        }
                    }

                    // Result code AS - 1 byte
                    ZvtConstants.BMP_RESULT_CODE_AS -> {
                        if (offset < data.size) {
                            resultCodeAs = data[offset]
                            offset++
                        }
                    }

                    // AID parameter - 5 byte fixed
                    ZvtConstants.BMP_AID_PARAMETER -> {
                        if (offset + 5 <= data.size) {
                            offset += 5 // read and skip
                        }
                    }

                    // Bilinmeyen tag → veriyi log'la ve dur
                    // (Bilinmeyen BMP tag'in uzunluğu belirlenemez, devam etmek tehlikeli)
                    else -> {
                        break
                    }
                }
            } catch (e: Exception) {
                // Parse hatası, dur
                break
            }
        }

        val cardData = if (maskedPan.isNotEmpty() || cardType.isNotEmpty() || cardName.isNotEmpty()) {
            CardData(
                maskedPan = maskedPan,
                cardType = cardType,
                cardName = cardName,
                expiryDate = expiryDate,
                sequenceNumber = sequenceNumber,
                aid = aid
            )
        } else null

        return TransactionResult(
            success = success,
            resultCode = resultCode,
            resultMessage = ZvtConstants.getResultMessage(resultCode),
            amountInCents = amount,
            cardData = cardData,
            receiptNumber = receiptNumber,
            traceNumber = traceNumber,
            terminalId = terminalId,
            vuNumber = vuNumber,
            date = date,
            time = time,
            originalTrace = originalTrace,
            turnoverNumber = turnoverNumber,
            rawData = data
        )
    }

    // =====================================================
    // LLVAR / LLLVAR Length Decoders (ZVT Spec v13.13)
    // =====================================================

    /**
     * LLVAR uzunluk çözücü: 2 byte FxFy formatı
     * Her byte 0xFn formatında, n = nibble digit
     * Ör: F0 F3 → "03" → 3 byte
     */
    private fun decodeLlvar(data: ByteArray, offset: Int): Int {
        val d1 = data[offset].toInt() and 0x0F
        val d2 = data[offset + 1].toInt() and 0x0F
        return d1 * 10 + d2
    }

    /**
     * LLLVAR uzunluk çözücü: 3 byte FxFyFz formatı
     * Ör: F0 F1 F5 → "015" → 15 byte
     */
    private fun decodeLllvar(data: ByteArray, offset: Int): Int {
        val d1 = data[offset].toInt() and 0x0F
        val d2 = data[offset + 1].toInt() and 0x0F
        val d3 = data[offset + 2].toInt() and 0x0F
        return d1 * 100 + d2 * 10 + d3
    }

    /**
     * BER-TLV length encoding decoder (BMP 06 TLV container)
     * Returns (length, bytesConsumed)
     */
    private fun readBerTlvLength(data: ByteArray, offset: Int): Pair<Int, Int> {
        if (offset >= data.size) return Pair(0, 0)
        val firstByte = data[offset].toInt() and 0xFF
        return if (firstByte < 0x80) {
            Pair(firstByte, 1)
        } else if (firstByte == 0x81) {
            if (offset + 1 < data.size) {
                Pair(data[offset + 1].toInt() and 0xFF, 2)
            } else Pair(0, 1)
        } else if (firstByte == 0x82) {
            if (offset + 2 < data.size) {
                val len = ((data[offset + 1].toInt() and 0xFF) shl 8) or
                        (data[offset + 2].toInt() and 0xFF)
                Pair(len, 3)
            } else Pair(0, 1)
        } else {
            Pair(0, 1)
        }
    }

    /**
     * TLV entries'i parse eder
     */
    private fun parseTlvEntries(data: ByteArray) = TlvParser.parse(data)

    /**
     * Kart tipi byte'ını okunabilir isme çevirir
     */
    private fun resolveCardType(typeByte: Byte): String = when (typeByte.toUnsignedInt()) {
        0x01 -> "EC / Girocard"
        0x02 -> "Maestro"
        0x05 -> "AMEX"
        0x06 -> "VISA"
        0x07 -> "VISA Electron"
        0x0A -> "Mastercard"
        0x0B -> "JCB"
        0x0C -> "Diners Club"
        0x0E -> "Discover"
        0x0F -> "UnionPay"
        0x46 -> "girocard kontaktlos"
        else -> "Bilinmeyen (${String.format("0x%02X", typeByte)})"
    }
}
