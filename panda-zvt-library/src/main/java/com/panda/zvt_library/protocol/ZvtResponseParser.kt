package com.panda.zvt_library.protocol

import com.panda.zvt_library.model.*
import com.panda.zvt_library.util.BcdHelper
import com.panda.zvt_library.util.TlvParser
import com.panda.zvt_library.util.toHexString
import com.panda.zvt_library.util.toSafeAscii
import com.panda.zvt_library.util.toUnsignedInt
import timber.log.Timber

/**
 * ZVT response parser.
 *
 * Parses response packets received from the payment terminal (PT -> ECR direction)
 * into structured model objects. Handles Status Information (04 0F), Intermediate
 * Status (04 FF), Completion (06 0F), Abort (06 1E), and Print Line (06 D1) packets.
 *
 * Supports parsing of all standard BMP fields including fixed-length, LLVAR, and LLLVAR
 * encoded data, as well as nested TLV containers.
 *
 * Reference: ZVT Protocol Specification v13.13
 *
 * @author Erkan Kaplan
 * @since 2026-02-10
 */
object ZvtResponseParser {

    private const val TAG = "ZVT"

    /**
     * Parses a Status Information packet (04 0F) into a [TransactionResult].
     *
     * The Status Info packet contains BMP fields with transaction details
     * such as amount, card data, trace number, receipt number, etc.
     *
     * @param packet The received [ZvtPacket] with command 04 0F.
     * @return Parsed [TransactionResult] with all extracted fields.
     */
    fun parseStatusInfo(packet: ZvtPacket): TransactionResult {
        Timber.tag(TAG).d("[ResponseParser] Parsing Status Info (04 0F), data length=%d bytes", packet.dataLength)
        return parseBmpData(packet.data)
    }

    /**
     * Parses an Intermediate Status packet (04 FF) into an [IntermediateStatus].
     *
     * The first byte of data is the status code which maps to a human-readable message
     * (e.g. "Insert card", "Enter PIN", "Please wait").
     *
     * @param packet The received [ZvtPacket] with command 04 FF.
     * @return Parsed [IntermediateStatus] with code and message.
     */
    fun parseIntermediateStatus(packet: ZvtPacket): IntermediateStatus {
        val statusCode = if (packet.data.isNotEmpty()) packet.data[0] else 0x00
        val message = ZvtConstants.getIntermediateStatusMessage(statusCode)
        Timber.tag(TAG).d("[ResponseParser] Intermediate Status: code=0x%02X, message='%s'",
            statusCode, message)
        return IntermediateStatus(
            statusCode = statusCode,
            message = message
        )
    }

    /**
     * Parses a Completion packet (06 0F) into a [TransactionResult].
     *
     * Completion packets usually have no data payload (indicating success),
     * but may contain BMP fields in some cases.
     *
     * @param packet The received [ZvtPacket] with command 06 0F.
     * @return Parsed [TransactionResult].
     */
    fun parseCompletion(packet: ZvtPacket): TransactionResult {
        Timber.tag(TAG).d("[ResponseParser] Parsing Completion (06 0F), data length=%d bytes", packet.dataLength)
        return if (packet.data.isEmpty()) {
            Timber.tag(TAG).d("[ResponseParser] Completion with no data -> success")
            TransactionResult(success = true, resultMessage = "Transaction completed")
        } else {
            parseBmpData(packet.data)
        }
    }

    /**
     * Parses an Abort packet (06 1E) into a [TransactionResult].
     *
     * The first byte of data contains the result/error code.
     *
     * @param packet The received [ZvtPacket] with command 06 1E.
     * @return Parsed [TransactionResult] with success=false and the error message.
     */
    fun parseAbort(packet: ZvtPacket): TransactionResult {
        val resultCode = if (packet.data.isNotEmpty()) packet.data[0] else ZvtConstants.RC_SYSTEM_ERROR
        val message = ZvtConstants.getResultMessage(resultCode)
        Timber.tag(TAG).d("[ResponseParser] Abort: resultCode=0x%02X, message='%s'",
            resultCode, message)
        return TransactionResult(
            success = false,
            resultCode = resultCode,
            resultMessage = message
        )
    }

    /**
     * Parses a Print Line packet (06 D1) to extract the receipt text line.
     *
     * The first byte is a print attribute, and the remaining bytes are ASCII text.
     *
     * @param packet The received [ZvtPacket] with command 06 D1.
     * @return The text line to print, or empty string if no text is present.
     */
    fun parsePrintLine(packet: ZvtPacket): String {
        if (packet.data.isEmpty()) return ""
        // First byte is attribute, rest is text
        val line = if (packet.data.size > 1) {
            packet.data.copyOfRange(1, packet.data.size).toSafeAscii().trim()
        } else {
            ""
        }
        if (line.isNotEmpty()) {
            Timber.tag(TAG).d("[ResponseParser] Print Line: attr=0x%02X, text='%s'",
                packet.data[0], line)
        }
        return line
    }

    // =====================================================
    // BMP Data Parser
    // =====================================================

    /**
     * Parses BMP (Bitmap) fields from a data block into a [TransactionResult].
     *
     * BMP field format (ZVT Spec v13.13):
     * - **Fixed length**: `[TAG(1)] [DATA(n)]` - length determined by tag
     * - **LLVAR**: `[TAG(1)] [LEN(2 byte FxFy)] [DATA(n)]` - FxFy = nibble-coded length
     * - **LLLVAR**: `[TAG(1)] [LEN(3 byte FxFyFz)] [DATA(n)]` - FxFyFz = nibble-coded length
     *
     * @param data Raw byte array containing BMP fields.
     * @return Populated [TransactionResult] with all parsed fields.
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

        Timber.tag(TAG).d("[ResponseParser] Parsing BMP data: %d bytes, hex=%s",
            data.size, data.toHexString())

        while (offset < data.size) {
            val tag = data[offset]
            offset++

            try {
                when (tag) {
                    // Service byte - 1 byte
                    ZvtConstants.BMP_SERVICE_BYTE -> {
                        if (offset < data.size) {
                            Timber.tag(TAG).d("[ResponseParser] BMP 0x03 Service byte: 0x%02X", data[offset])
                            offset++
                        }
                    }

                    // Amount - 6 byte BCD
                    ZvtConstants.BMP_AMOUNT -> {
                        if (offset + 6 <= data.size) {
                            amount = BcdHelper.bcdToAmount(data.copyOfRange(offset, offset + 6))
                            Timber.tag(TAG).d("[ResponseParser] BMP 0x04 Amount: %d cents (%.2f EUR), raw=%s",
                                amount, amount / 100.0, data.copyOfRange(offset, offset + 6).toHexString())
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
                                Timber.tag(TAG).d("[ResponseParser] BMP 0x06 TLV container: %d bytes, hex=%s",
                                    len, tlvData.toHexString())
                                parseTlvEntries(tlvData).let { entries ->
                                    entries.firstOrNull { it.tag == ZvtConstants.TLV_CARD_NAME }?.let {
                                        cardName = it.valueAscii
                                        Timber.tag(TAG).d("[ResponseParser]   TLV card name: '%s'", cardName)
                                    }
                                }
                                offset += len
                            }
                        }
                    }

                    // Trace number - 3 byte BCD
                    ZvtConstants.BMP_TRACE_NUMBER -> {
                        if (offset + 3 <= data.size) {
                            traceNumber = BcdHelper.bcdToTraceNumber(data.copyOfRange(offset, offset + 3))
                            Timber.tag(TAG).d("[ResponseParser] BMP 0x0B Trace number: %d, raw=%s",
                                traceNumber, data.copyOfRange(offset, offset + 3).toHexString())
                            offset += 3
                        }
                    }

                    // Time - 3 byte BCD (HHMMSS)
                    ZvtConstants.BMP_TIME -> {
                        if (offset + 3 <= data.size) {
                            time = BcdHelper.bcdToString(data.copyOfRange(offset, offset + 3))
                            Timber.tag(TAG).d("[ResponseParser] BMP 0x0C Time: %s", time)
                            offset += 3
                        }
                    }

                    // Date - 2 byte BCD (MMDD)
                    ZvtConstants.BMP_DATE -> {
                        if (offset + 2 <= data.size) {
                            date = BcdHelper.bcdToString(data.copyOfRange(offset, offset + 2))
                            Timber.tag(TAG).d("[ResponseParser] BMP 0x0D Date: %s", date)
                            offset += 2
                        }
                    }

                    // Expiry date - 2 byte BCD (YYMM)
                    ZvtConstants.BMP_EXPIRY_DATE -> {
                        if (offset + 2 <= data.size) {
                            expiryDate = BcdHelper.bcdToString(data.copyOfRange(offset, offset + 2))
                            Timber.tag(TAG).d("[ResponseParser] BMP 0x0E Expiry date: %s", expiryDate)
                            offset += 2
                        }
                    }

                    // Card sequence number - 2 byte BCD
                    ZvtConstants.BMP_CARD_SEQUENCE_NR -> {
                        if (offset + 2 <= data.size) {
                            sequenceNumber = BcdHelper.bcdToString(data.copyOfRange(offset, offset + 2)).toIntOrNull() ?: 0
                            Timber.tag(TAG).d("[ResponseParser] BMP 0x17 Card sequence number: %d", sequenceNumber)
                            offset += 2
                        }
                    }

                    // Payment type - 1 byte
                    ZvtConstants.BMP_PAYMENT_TYPE -> {
                        if (offset < data.size) {
                            paymentType = data[offset]
                            Timber.tag(TAG).d("[ResponseParser] BMP 0x19 Payment type: 0x%02X", paymentType)
                            offset++
                        }
                    }

                    // PAN/EF_ID - LLVAR BCD (2 byte FxFy counter)
                    ZvtConstants.BMP_CARD_NUMBER -> {
                        if (offset + 2 <= data.size) {
                            val len = decodeLlvar(data, offset)
                            Timber.tag(TAG).d("[ResponseParser] BMP 0x22 PAN/EF_ID: LLVAR len=%d", len)
                            offset += 2
                            if (offset + len <= data.size) {
                                maskedPan = BcdHelper.bcdToString(data.copyOfRange(offset, offset + len))
                                Timber.tag(TAG).d("[ResponseParser]   PAN: %s", maskedPan)
                                offset += len
                            }
                        }
                    }

                    // Result code - 1 byte
                    ZvtConstants.BMP_RESULT_CODE -> {
                        if (offset < data.size) {
                            resultCode = data[offset]
                            success = resultCode == ZvtConstants.RC_SUCCESS
                            Timber.tag(TAG).d("[ResponseParser] BMP 0x27 Result code: 0x%02X (%s) -> success=%b",
                                resultCode, ZvtConstants.getResultMessage(resultCode), success)
                            offset++
                        }
                    }

                    // Terminal ID - 4 byte BCD
                    ZvtConstants.BMP_TERMINAL_ID -> {
                        if (offset + 4 <= data.size) {
                            terminalId = BcdHelper.bcdToString(data.copyOfRange(offset, offset + 4))
                            Timber.tag(TAG).d("[ResponseParser] BMP 0x29 Terminal ID: %s", terminalId)
                            offset += 4
                        }
                    }

                    // VU number - 15 byte ASCII (fixed)
                    ZvtConstants.BMP_VU_NUMBER -> {
                        if (offset + 15 <= data.size) {
                            vuNumber = data.copyOfRange(offset, offset + 15).toSafeAscii().trim()
                            Timber.tag(TAG).d("[ResponseParser] BMP 0x2A VU number: '%s'", vuNumber)
                            offset += 15
                        }
                    }

                    // Original trace number - 3 byte BCD (reversal only)
                    ZvtConstants.BMP_ORIGINAL_TRACE -> {
                        if (offset + 3 <= data.size) {
                            originalTrace = BcdHelper.bcdToTraceNumber(data.copyOfRange(offset, offset + 3))
                            Timber.tag(TAG).d("[ResponseParser] BMP 0x37 Original trace: %d", originalTrace)
                            offset += 3
                        }
                    }

                    // AID - 8 byte fixed
                    ZvtConstants.BMP_AID -> {
                        if (offset + 8 <= data.size) {
                            aid = data.copyOfRange(offset, offset + 8).toHexString("")
                            Timber.tag(TAG).d("[ResponseParser] BMP 0x3B AID: %s", aid)
                            offset += 8
                        }
                    }

                    // Additional data / TLV container - LLLVAR (3 byte FxFyFz counter)
                    ZvtConstants.BMP_ADDITIONAL_DATA -> {
                        if (offset + 3 <= data.size) {
                            val len = decodeLllvar(data, offset)
                            Timber.tag(TAG).d("[ResponseParser] BMP 0x3C Additional data: LLLVAR len=%d", len)
                            offset += 3
                            if (offset + len <= data.size) {
                                val tlvData = data.copyOfRange(offset, offset + len)
                                Timber.tag(TAG).d("[ResponseParser]   TLV data: %s", tlvData.toHexString())
                                parseTlvEntries(tlvData).let { entries ->
                                    entries.firstOrNull { it.tag == ZvtConstants.TLV_CARD_NAME }?.let {
                                        cardName = it.valueAscii
                                        Timber.tag(TAG).d("[ResponseParser]   TLV card name: '%s'", cardName)
                                    }
                                }
                                offset += len
                            }
                        }
                    }

                    // Currency code - 2 byte BCD
                    ZvtConstants.BMP_CURRENCY_CODE -> {
                        if (offset + 2 <= data.size) {
                            val cc = BcdHelper.bcdToString(data.copyOfRange(offset, offset + 2))
                            Timber.tag(TAG).d("[ResponseParser] BMP 0x49 Currency code: %s", cc)
                            offset += 2
                        }
                    }

                    // Blocked goods groups - LLVAR
                    ZvtConstants.BMP_BLOCKED_GOODS_GROUPS -> {
                        if (offset + 2 <= data.size) {
                            val len = decodeLlvar(data, offset)
                            Timber.tag(TAG).d("[ResponseParser] BMP 0x4C Blocked goods groups: LLVAR len=%d", len)
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
                            Timber.tag(TAG).d("[ResponseParser] BMP 0x60 Single amounts: LLLVAR len=%d", len)
                            offset += 3
                            if (offset + len <= data.size) {
                                offset += len // skip for now
                            }
                        }
                    }

                    // Receipt number - 2 byte BCD
                    ZvtConstants.BMP_RECEIPT_NR -> {
                        if (offset + 2 <= data.size) {
                            receiptNumber = BcdHelper.bcdToString(data.copyOfRange(offset, offset + 2)).toIntOrNull() ?: 0
                            Timber.tag(TAG).d("[ResponseParser] BMP 0x87 Receipt number: %d", receiptNumber)
                            offset += 2
                        }
                    }

                    // Turnover number - 3 byte BCD
                    ZvtConstants.BMP_TURNOVER_NR -> {
                        if (offset + 3 <= data.size) {
                            turnoverNumber = BcdHelper.bcdToTraceNumber(data.copyOfRange(offset, offset + 3))
                            Timber.tag(TAG).d("[ResponseParser] BMP 0x88 Turnover number: %d", turnoverNumber)
                            offset += 3
                        }
                    }

                    // Card type - 1 byte
                    ZvtConstants.BMP_CARD_TYPE -> {
                        if (offset < data.size) {
                            cardType = resolveCardType(data[offset])
                            Timber.tag(TAG).d("[ResponseParser] BMP 0x8A Card type: 0x%02X -> '%s'",
                                data[offset], cardType)
                            offset++
                        }
                    }

                    // Card name - LLVAR ASCII null-terminated
                    ZvtConstants.BMP_CARD_NAME -> {
                        if (offset + 2 <= data.size) {
                            val len = decodeLlvar(data, offset)
                            Timber.tag(TAG).d("[ResponseParser] BMP 0x8B Card name: LLVAR len=%d", len)
                            offset += 2
                            if (offset + len <= data.size) {
                                cardName = data.copyOfRange(offset, offset + len)
                                    .toSafeAscii().trimEnd('\u0000').trim()
                                Timber.tag(TAG).d("[ResponseParser]   Card name: '%s'", cardName)
                                offset += len
                            }
                        }
                    }

                    // Card type ID / Network identifier - 1 byte
                    ZvtConstants.BMP_CARD_TYPE_ID -> {
                        if (offset < data.size) {
                            Timber.tag(TAG).d("[ResponseParser] BMP 0x8C Card type network ID: 0x%02X", data[offset])
                            offset++
                        }
                    }

                    // Result code AS - 1 byte
                    ZvtConstants.BMP_RESULT_CODE_AS -> {
                        if (offset < data.size) {
                            resultCodeAs = data[offset]
                            Timber.tag(TAG).d("[ResponseParser] BMP 0xA0 Result code AS: 0x%02X", resultCodeAs)
                            offset++
                        }
                    }

                    // AID parameter - 5 byte fixed
                    ZvtConstants.BMP_AID_PARAMETER -> {
                        if (offset + 5 <= data.size) {
                            Timber.tag(TAG).d("[ResponseParser] BMP 0xBA AID parameter: %s",
                                data.copyOfRange(offset, offset + 5).toHexString())
                            offset += 5
                        }
                    }

                    // Unknown tag -> log and stop
                    // (Cannot determine unknown BMP tag's length, continuing is unsafe)
                    else -> {
                        Timber.tag(TAG).w("[ResponseParser] Unknown BMP tag 0x%02X at offset %d, stopping parse",
                            tag, offset - 1)
                        break
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e("[ResponseParser] Parse error at BMP tag 0x%02X, offset %d: %s",
                    tag, offset, e.message)
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

        val result = TransactionResult(
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

        Timber.tag(TAG).d("[ResponseParser] Parse result: success=%b, resultCode=0x%02X, amount=%d, trace=%d, receipt=%d",
            result.success, result.resultCode, result.amountInCents, result.traceNumber, result.receiptNumber)

        return result
    }

    // =====================================================
    // LLVAR / LLLVAR Length Decoders (ZVT Spec v13.13)
    // =====================================================

    /**
     * Decodes an LLVAR length field (2-byte FxFy format).
     *
     * Each byte is in `0xFn` format where `n` is a nibble digit.
     * Example: `F0 F3` -> "03" -> 3 bytes.
     *
     * @param data The data array.
     * @param offset Position of the first length byte.
     * @return Decoded length value.
     */
    private fun decodeLlvar(data: ByteArray, offset: Int): Int {
        val d1 = data[offset].toInt() and 0x0F
        val d2 = data[offset + 1].toInt() and 0x0F
        return d1 * 10 + d2
    }

    /**
     * Decodes an LLLVAR length field (3-byte FxFyFz format).
     *
     * Example: `F0 F1 F5` -> "015" -> 15 bytes.
     *
     * @param data The data array.
     * @param offset Position of the first length byte.
     * @return Decoded length value.
     */
    private fun decodeLllvar(data: ByteArray, offset: Int): Int {
        val d1 = data[offset].toInt() and 0x0F
        val d2 = data[offset + 1].toInt() and 0x0F
        val d3 = data[offset + 2].toInt() and 0x0F
        return d1 * 100 + d2 * 10 + d3
    }

    /**
     * Decodes a BER-TLV length field (used in BMP 06 TLV container).
     *
     * @param data The data array.
     * @param offset Position of the length field.
     * @return Pair(decoded length, bytes consumed by the length field).
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
     * Parses TLV entries using the [TlvParser].
     *
     * @param data Raw TLV byte data.
     * @return List of parsed TLV entries.
     */
    private fun parseTlvEntries(data: ByteArray) = TlvParser.parse(data)

    /**
     * Resolves a card type byte to a human-readable card name.
     *
     * Based on ZVT Spec v13.13 card type definitions.
     *
     * @param typeByte The card type byte from BMP 0x8A.
     * @return Human-readable card type name.
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
        0x46 -> "girocard contactless"
        else -> "Unknown (${String.format("0x%02X", typeByte)})"
    }
}
