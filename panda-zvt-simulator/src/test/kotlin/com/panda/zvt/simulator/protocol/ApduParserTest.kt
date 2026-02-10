package com.panda.zvt.simulator.protocol

import org.junit.Test
import org.junit.Assert.*

class ApduParserTest {

    // --- isAck ---

    @Test
    fun isAck_validAck_returnsTrue() {
        val data = byteArrayOf(0x80.toByte(), 0x00, 0x00)
        assertTrue(ApduParser.isAck(data))
    }

    @Test
    fun isAck_nonAckCommand_returnsFalse() {
        val data = byteArrayOf(0x06, 0x00)
        assertFalse(ApduParser.isAck(data))
    }

    // --- isNack ---

    @Test
    fun isNack_validNack_returnsTrue() {
        val data = byteArrayOf(0x84.toByte(), 0x00, 0x00)
        assertTrue(ApduParser.isNack(data))
    }

    @Test
    fun isNack_nonNackCommand_returnsFalse() {
        val data = byteArrayOf(0x80.toByte(), 0x00)
        assertFalse(ApduParser.isNack(data))
    }

    // --- extractAmount ---

    @Test
    fun extractAmount_withBmp04_returnsAmount() {
        // Build data: some prefix byte, then BMP 0x04 tag, then 6 BCD bytes for 1250
        val data = byteArrayOf(
            0x27, 0x00,         // some other BMP field
            0x04,               // BMP_AMOUNT tag
            0x00, 0x00, 0x00, 0x00, 0x12, 0x50  // 1250 in BCD
        )
        val amount = ApduParser.extractAmount(data)
        assertNotNull(amount)
        assertEquals(1250L, amount)
    }

    @Test
    fun extractAmount_missingTag_returnsNull() {
        val data = byteArrayOf(0x27, 0x00, 0x0B, 0x00, 0x00, 0x01)
        val amount = ApduParser.extractAmount(data)
        assertNull(amount)
    }

    // --- extractReceiptNumber ---

    @Test
    fun extractReceiptNumber_withBmp87_returnsNumber() {
        val data = byteArrayOf(
            0x87.toByte(),      // BMP_RECEIPT_NR tag
            0x00, 0x42          // receipt number 42 in BCD
        )
        val receipt = ApduParser.extractReceiptNumber(data)
        assertNotNull(receipt)
        assertEquals(42, receipt)
    }

    @Test
    fun extractReceiptNumber_missingTag_returnsNull() {
        val data = byteArrayOf(0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        val receipt = ApduParser.extractReceiptNumber(data)
        assertNull(receipt)
    }

    // --- extractTraceNumber ---

    @Test
    fun extractTraceNumber_withBmp0B_returnsNumber() {
        val data = byteArrayOf(
            0x0B,               // BMP_TRACE_NUMBER tag
            0x00, 0x00, 0x01    // trace number 1 in BCD
        )
        val trace = ApduParser.extractTraceNumber(data)
        assertNotNull(trace)
        assertEquals(1, trace)
    }

    @Test
    fun extractTraceNumber_missingTag_returnsNull() {
        val data = byteArrayOf(0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        val trace = ApduParser.extractTraceNumber(data)
        assertNull(trace)
    }

    // --- extractAid ---

    @Test
    fun extractAid_withBmp3B_returns8Bytes() {
        val aidBytes = byteArrayOf(
            0xA0.toByte(), 0x00, 0x00, 0x00,
            0x04, 0x10, 0x10, 0x01
        )
        val data = byteArrayOf(0x3B) + aidBytes
        val aid = ApduParser.extractAid(data)
        assertNotNull(aid)
        assertEquals(8, aid!!.size)
        assertArrayEquals(aidBytes, aid)
    }
}
