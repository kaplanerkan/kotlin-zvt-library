package com.panda.zvt_library

import com.panda.zvt_library.protocol.ZvtResponseParser
import com.panda.zvt_library.protocol.ZvtPacket
import com.panda.zvt_library.protocol.ZvtConstants
import org.junit.Test
import org.junit.Assert.*

class ZvtResponseParserTest {

    // =====================================================
    // 1. parseCompletion — empty data -> success
    // =====================================================

    @Test
    fun parseCompletion_emptyData_returnsSuccessWithCompletedMessage() {
        val packet = ZvtPacket(
            command = ZvtConstants.RESP_COMPLETION,
            data = byteArrayOf()
        )
        val result = ZvtResponseParser.parseCompletion(packet)
        assertTrue(result.success)
        assertTrue(result.resultMessage.contains("completed", ignoreCase = true))
    }

    // =====================================================
    // 2. parseCompletion — result code 0x00 -> success
    // =====================================================

    @Test
    fun parseCompletion_resultCodeSuccess_returnsSuccessTrue() {
        val packet = ZvtPacket(
            command = ZvtConstants.RESP_COMPLETION,
            data = byteArrayOf(0x27, 0x00)
        )
        val result = ZvtResponseParser.parseCompletion(packet)
        assertTrue(result.success)
        assertEquals(0x00.toByte(), result.resultCode)
    }

    // =====================================================
    // 3. parseCompletion — result code 0x64 -> failure
    // =====================================================

    @Test
    fun parseCompletion_resultCodeError_returnsSuccessFalse() {
        val packet = ZvtPacket(
            command = ZvtConstants.RESP_COMPLETION,
            data = byteArrayOf(0x27, 0x64)
        )
        val result = ZvtResponseParser.parseCompletion(packet)
        assertFalse(result.success)
        assertEquals(0x64.toByte(), result.resultCode)
    }

    // =====================================================
    // 4. parseCompletion — amount BCD extraction
    // =====================================================

    @Test
    fun parseCompletion_withAmount_extractsAmountInCents() {
        // BMP 0x04 followed by 6-byte BCD: 00 00 00 00 12 50 = 1250 cents
        val packet = ZvtPacket(
            command = ZvtConstants.RESP_COMPLETION,
            data = byteArrayOf(0x04, 0x00, 0x00, 0x00, 0x00, 0x12, 0x50)
        )
        val result = ZvtResponseParser.parseCompletion(packet)
        assertEquals(1250L, result.amountInCents)
    }

    // =====================================================
    // 5. parseCompletion — trace number extraction
    // =====================================================

    @Test
    fun parseCompletion_withTraceNumber_extractsTrace() {
        // BMP 0x0B followed by 3-byte BCD: 00 00 01 = trace 1
        val packet = ZvtPacket(
            command = ZvtConstants.RESP_COMPLETION,
            data = byteArrayOf(0x0B, 0x00, 0x00, 0x01)
        )
        val result = ZvtResponseParser.parseCompletion(packet)
        assertEquals(1, result.traceNumber)
    }

    // =====================================================
    // 6. parseCompletion — receipt number extraction
    // =====================================================

    @Test
    fun parseCompletion_withReceiptNumber_extractsReceipt() {
        // BMP 0x87 followed by 2-byte BCD: 00 42 = receipt 42
        val packet = ZvtPacket(
            command = ZvtConstants.RESP_COMPLETION,
            data = byteArrayOf(0x87.toByte(), 0x00, 0x42)
        )
        val result = ZvtResponseParser.parseCompletion(packet)
        assertEquals(42, result.receiptNumber)
    }

    // =====================================================
    // 7. parseCompletion — multiple BMPs combined
    // =====================================================

    @Test
    fun parseCompletion_multipleBmps_extractsAllFields() {
        // Amount(0x04) + Result(0x27) + Trace(0x0B) + Receipt(0x87)
        val packet = ZvtPacket(
            command = ZvtConstants.RESP_COMPLETION,
            data = byteArrayOf(
                // BMP 0x04: Amount = 5000 cents (50.00 EUR)
                0x04, 0x00, 0x00, 0x00, 0x00, 0x50, 0x00,
                // BMP 0x27: Result code = 0x00 (success)
                0x27, 0x00,
                // BMP 0x0B: Trace number = 123
                0x0B, 0x00, 0x01, 0x23,
                // BMP 0x87: Receipt number = 99
                0x87.toByte(), 0x00, 0x99.toByte()
            )
        )
        val result = ZvtResponseParser.parseCompletion(packet)
        assertTrue(result.success)
        assertEquals(0x00.toByte(), result.resultCode)
        assertEquals(5000L, result.amountInCents)
        assertEquals(123, result.traceNumber)
        assertEquals(99, result.receiptNumber)
    }

    // =====================================================
    // 8. parseStatusInfo — amount and result code
    // =====================================================

    @Test
    fun parseStatusInfo_withAmountAndResult_extractsCorrectly() {
        val packet = ZvtPacket(
            command = ZvtConstants.RESP_STATUS_INFO,
            data = byteArrayOf(
                // BMP 0x04: Amount = 2500 cents (25.00 EUR)
                0x04, 0x00, 0x00, 0x00, 0x00, 0x25, 0x00,
                // BMP 0x27: Result code = 0x00 (success)
                0x27, 0x00
            )
        )
        val result = ZvtResponseParser.parseStatusInfo(packet)
        assertTrue(result.success)
        assertEquals(2500L, result.amountInCents)
        assertEquals(0x00.toByte(), result.resultCode)
    }

    // =====================================================
    // 9. parseStatusInfo — terminal ID extraction
    // =====================================================

    @Test
    fun parseStatusInfo_withTerminalId_extractsTerminalId() {
        // BMP 0x29 followed by 4-byte BCD: 29 00 12 34 = "29001234"
        val packet = ZvtPacket(
            command = ZvtConstants.RESP_STATUS_INFO,
            data = byteArrayOf(0x29, 0x29, 0x00, 0x12, 0x34)
        )
        val result = ZvtResponseParser.parseStatusInfo(packet)
        assertEquals("29001234", result.terminalId)
    }

    // =====================================================
    // 10. parseIntermediateStatus — code 0x0A "Insert card"
    // =====================================================

    @Test
    fun parseIntermediateStatus_insertCard_returnsCorrectMessage() {
        val packet = ZvtPacket(
            command = ZvtConstants.RESP_INTERMEDIATE_STATUS,
            data = byteArrayOf(0x0A)
        )
        val status = ZvtResponseParser.parseIntermediateStatus(packet)
        assertEquals(0x0A.toByte(), status.statusCode)
        assertTrue(
            "Expected message to contain 'Insert card' but was: '${status.message}'",
            status.message.contains("Insert card", ignoreCase = true)
        )
    }

    // =====================================================
    // 11. parseIntermediateStatus — code 0x0E "Please wait"
    // =====================================================

    @Test
    fun parseIntermediateStatus_pleaseWait_returnsCorrectMessage() {
        val packet = ZvtPacket(
            command = ZvtConstants.RESP_INTERMEDIATE_STATUS,
            data = byteArrayOf(0x0E)
        )
        val status = ZvtResponseParser.parseIntermediateStatus(packet)
        assertEquals(0x0E.toByte(), status.statusCode)
        assertTrue(
            "Expected message to contain 'wait' but was: '${status.message}'",
            status.message.contains("wait", ignoreCase = true)
        )
    }

    // =====================================================
    // 12. parseIntermediateStatus — empty data -> code 0x00
    // =====================================================

    @Test
    fun parseIntermediateStatus_emptyData_returnsStatusCodeZero() {
        val packet = ZvtPacket(
            command = ZvtConstants.RESP_INTERMEDIATE_STATUS,
            data = byteArrayOf()
        )
        val status = ZvtResponseParser.parseIntermediateStatus(packet)
        assertEquals(0x00.toByte(), status.statusCode)
    }

    // =====================================================
    // 13. parseAbort — result code 0x6C (timeout)
    // =====================================================

    @Test
    fun parseAbort_timeoutCode_returnsFailureWithTimeoutMessage() {
        val packet = ZvtPacket(
            command = ZvtConstants.RESP_ABORT,
            data = byteArrayOf(0x6C)
        )
        val result = ZvtResponseParser.parseAbort(packet)
        assertFalse(result.success)
        assertEquals(0x6C.toByte(), result.resultCode)
        assertTrue(
            "Expected message to contain 'timeout' but was: '${result.resultMessage}'",
            result.resultMessage.contains("timeout", ignoreCase = true)
        )
    }

    // =====================================================
    // 14. parseAbort — empty data -> resultCode 0xFF
    // =====================================================

    @Test
    fun parseAbort_emptyData_returnsFailureWithSystemError() {
        val packet = ZvtPacket(
            command = ZvtConstants.RESP_ABORT,
            data = byteArrayOf()
        )
        val result = ZvtResponseParser.parseAbort(packet)
        assertFalse(result.success)
        assertEquals(0xFF.toByte(), result.resultCode)
    }

    // =====================================================
    // 15. parsePrintLine — extracts ASCII text after attribute
    // =====================================================

    @Test
    fun parsePrintLine_withText_extractsReceiptLine() {
        // First byte = 0x00 (attribute), rest = "RECEIPT LINE" in ASCII
        val packet = ZvtPacket(
            command = ZvtConstants.RESP_PRINT_LINE,
            data = byteArrayOf(
                0x00,                                           // attribute byte
                0x52, 0x45, 0x43, 0x45, 0x49, 0x50, 0x54,     // "RECEIPT"
                0x20,                                           // space
                0x4C, 0x49, 0x4E, 0x45                         // "LINE"
            )
        )
        val line = ZvtResponseParser.parsePrintLine(packet)
        assertEquals("RECEIPT LINE", line)
    }

    // =====================================================
    // 16. parsePrintLine — empty data -> ""
    // =====================================================

    @Test
    fun parsePrintLine_emptyData_returnsEmptyString() {
        val packet = ZvtPacket(
            command = ZvtConstants.RESP_PRINT_LINE,
            data = byteArrayOf()
        )
        val line = ZvtResponseParser.parsePrintLine(packet)
        assertEquals("", line)
    }
}
