package com.panda.zvt.simulator.protocol

import org.junit.Test
import org.junit.Assert.*

class BmpFieldBuilderTest {

    // --- resultCode ---

    @Test
    fun resultCode_success_returnsTagAndCode() {
        val result = BmpFieldBuilder.resultCode(0x00)
        assertEquals(2, result.size)
        assertArrayEquals(byteArrayOf(0x27, 0x00), result)
    }

    @Test
    fun resultCode_timeout_returnsTagAndCode() {
        val result = BmpFieldBuilder.resultCode(0x6C)
        assertEquals(2, result.size)
        assertArrayEquals(byteArrayOf(0x27, 0x6C), result)
    }

    // --- amount ---

    @Test
    fun amount_1250_returnsTagPlus6BcdBytes() {
        val result = BmpFieldBuilder.amount(1250)
        assertEquals(7, result.size)
        assertEquals(0x04.toByte(), result[0])
        // BCD for 1250: 00 00 00 00 12 50
        assertArrayEquals(
            byteArrayOf(0x04, 0x00, 0x00, 0x00, 0x00, 0x12, 0x50),
            result
        )
    }

    @Test
    fun amount_zero_returnsTagPlusZeroBcd() {
        val result = BmpFieldBuilder.amount(0)
        assertEquals(7, result.size)
        assertEquals(0x04.toByte(), result[0])
        // All BCD bytes should be zero
        for (i in 1..6) {
            assertEquals(0x00.toByte(), result[i])
        }
    }

    // --- traceNumber ---

    @Test
    fun traceNumber_1_returnsTagPlus3BcdBytes() {
        val result = BmpFieldBuilder.traceNumber(1)
        assertEquals(4, result.size)
        assertEquals(0x0B.toByte(), result[0])
    }

    // --- originalTrace ---

    @Test
    fun originalTrace_42_returnsTagPlus3BcdBytes() {
        val result = BmpFieldBuilder.originalTrace(42)
        assertEquals(4, result.size)
        assertEquals(0x37.toByte(), result[0])
    }

    // --- time ---

    @Test
    fun time_123045_returnsTagPlus3BcdBytes() {
        val result = BmpFieldBuilder.time(12, 30, 45)
        assertEquals(4, result.size)
        assertEquals(0x0C.toByte(), result[0])
        assertArrayEquals(
            byteArrayOf(0x0C, 0x12, 0x30, 0x45),
            result
        )
    }

    // --- date ---

    @Test
    fun date_february14_returnsTagPlus2BcdBytes() {
        val result = BmpFieldBuilder.date(2, 14)
        assertEquals(3, result.size)
        assertEquals(0x0D.toByte(), result[0])
        assertArrayEquals(byteArrayOf(0x0D, 0x02, 0x14), result)
    }

    // --- terminalId ---

    @Test
    fun terminalId_29001234_returnsTagPlus4BcdBytes() {
        val result = BmpFieldBuilder.terminalId("29001234")
        assertEquals(5, result.size)
        assertEquals(0x29.toByte(), result[0])
        assertArrayEquals(
            byteArrayOf(0x29, 0x00, 0x12, 0x34),
            result.copyOfRange(1, 5)
        )
    }

    // --- vuNumber ---

    @Test
    fun vuNumber_sim_returnsTagPlus15AsciiBytes() {
        val result = BmpFieldBuilder.vuNumber("SIM")
        assertEquals(16, result.size)
        assertEquals(0x2A.toByte(), result[0])
        // "SIM" padded to 15 characters with spaces
        val expectedAscii = "SIM".padEnd(15, ' ').toByteArray(Charsets.US_ASCII)
        assertArrayEquals(expectedAscii, result.copyOfRange(1, 16))
    }

    // --- currencyCode ---

    @Test
    fun currencyCode_978_returnsTagPlus2BcdBytes() {
        val result = BmpFieldBuilder.currencyCode(978)
        assertEquals(3, result.size)
        assertEquals(0x49.toByte(), result[0])
        assertArrayEquals(byteArrayOf(0x49, 0x09, 0x78), result)
    }

    // --- receiptNumber ---

    @Test
    fun receiptNumber_42_returnsTagPlus2BcdBytes() {
        val result = BmpFieldBuilder.receiptNumber(42)
        assertEquals(3, result.size)
        assertEquals(0x87.toByte(), result[0])
        assertArrayEquals(
            byteArrayOf(0x87.toByte(), 0x00, 0x42),
            result
        )
    }

    // --- turnoverNumber ---

    @Test
    fun turnoverNumber_1_returnsTagPlus3BcdBytes() {
        val result = BmpFieldBuilder.turnoverNumber(1)
        assertEquals(4, result.size)
        assertEquals(0x88.toByte(), result[0])
    }

    // --- cardType ---

    @Test
    fun cardType_girocard_returnsTagAndType() {
        val result = BmpFieldBuilder.cardType(0x06)
        assertEquals(2, result.size)
        assertArrayEquals(byteArrayOf(0x8A.toByte(), 0x06), result)
    }

    // --- pan ---

    @Test
    fun pan_16digits_returnsTagPlusLlvarPlusBcd() {
        val result = BmpFieldBuilder.pan("6763890000001230")
        assertEquals(0x22.toByte(), result[0])
        // Next 2 bytes should be LLVAR FxFy length prefix
        val fx = result[1].toInt() and 0xFF
        val fy = result[2].toInt() and 0xFF
        assertTrue("FxFy high nibble should be 0xF", fx >= 0xF0)
        assertTrue("FxFy high nibble should be 0xF", fy >= 0xF0)
    }

    // --- cardName ---

    @Test
    fun cardName_mastercard_returnsTagLlvarAsciiNullTerminated() {
        val result = BmpFieldBuilder.cardName("Mastercard")
        assertEquals(0x8B.toByte(), result[0])
        // LLVAR: 2 bytes FxFy
        val fx = result[1].toInt() and 0xFF
        val fy = result[2].toInt() and 0xFF
        assertTrue("FxFy high nibble should be 0xF", fx >= 0xF0)
        assertTrue("FxFy high nibble should be 0xF", fy >= 0xF0)
        // Data length = "Mastercard".length + 1 (null terminator) = 11
        val dataLen = (fx and 0x0F) * 10 + (fy and 0x0F)
        assertEquals(11, dataLen)
        // Last byte should be null terminator
        assertEquals(0x00.toByte(), result[result.size - 1])
    }

    // --- aid ---

    @Test
    fun aid_validHex_returnsTagPlus8Bytes() {
        val result = BmpFieldBuilder.aid("A000000004101001")
        assertEquals(9, result.size)
        assertEquals(0x3B.toByte(), result[0])
        // Verify AID bytes
        assertEquals(0xA0.toByte(), result[1])
        assertEquals(0x00.toByte(), result[2])
        assertEquals(0x00.toByte(), result[3])
        assertEquals(0x00.toByte(), result[4])
        assertEquals(0x04.toByte(), result[5])
        assertEquals(0x10.toByte(), result[6])
        assertEquals(0x10.toByte(), result[7])
        assertEquals(0x01.toByte(), result[8])
    }

    // --- paymentType ---

    @Test
    fun paymentType_contactless_returnsTagAndType() {
        val result = BmpFieldBuilder.paymentType(0x40)
        assertEquals(2, result.size)
        assertArrayEquals(byteArrayOf(0x19, 0x40), result)
    }
}
