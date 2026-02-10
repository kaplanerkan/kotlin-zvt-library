package com.panda.zvt.simulator.protocol

import org.junit.Test
import org.junit.Assert.*

class BcdEncoderTest {

    // --- amountToBcd ---

    @Test
    fun amountToBcd_zero_returns6ZeroBytes() {
        val result = BcdEncoder.amountToBcd(0)
        assertEquals(6, result.size)
        assertArrayEquals(byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00), result)
    }

    @Test
    fun amountToBcd_1250_encodesCorrectly() {
        val result = BcdEncoder.amountToBcd(1250)
        assertEquals(6, result.size)
        assertArrayEquals(
            byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x12, 0x50),
            result
        )
    }

    @Test
    fun amountToBcd_bcdToAmount_roundTrip() {
        val original = 999999L
        val bcd = BcdEncoder.amountToBcd(original)
        val decoded = BcdEncoder.bcdToAmount(bcd)
        assertEquals(original, decoded)
    }

    @Test(expected = IllegalArgumentException::class)
    fun amountToBcd_negative_throwsException() {
        BcdEncoder.amountToBcd(-1)
    }

    // --- traceNumberToBcd ---

    @Test
    fun traceNumberToBcd_1_encodesCorrectly() {
        val result = BcdEncoder.traceNumberToBcd(1)
        assertEquals(3, result.size)
        assertArrayEquals(byteArrayOf(0x00, 0x00, 0x01), result)
    }

    @Test
    fun traceNumberToBcd_999999_encodesMaxValue() {
        val result = BcdEncoder.traceNumberToBcd(999999)
        assertEquals(3, result.size)
        assertArrayEquals(
            byteArrayOf(0x99.toByte(), 0x99.toByte(), 0x99.toByte()),
            result
        )
    }

    // --- receiptNumberToBcd ---

    @Test
    fun receiptNumberToBcd_42_encodesCorrectly() {
        val result = BcdEncoder.receiptNumberToBcd(42)
        assertEquals(2, result.size)
        assertArrayEquals(byteArrayOf(0x00, 0x42), result)
    }

    // --- turnoverNumberToBcd ---

    @Test
    fun turnoverNumberToBcd_1_encodesCorrectly() {
        val result = BcdEncoder.turnoverNumberToBcd(1)
        assertEquals(3, result.size)
        assertArrayEquals(byteArrayOf(0x00, 0x00, 0x01), result)
    }

    // --- timeToBcd ---

    @Test
    fun timeToBcd_123045_encodesCorrectly() {
        val result = BcdEncoder.timeToBcd(12, 30, 45)
        assertEquals(3, result.size)
        assertArrayEquals(byteArrayOf(0x12, 0x30, 0x45), result)
    }

    // --- dateToBcd ---

    @Test
    fun dateToBcd_february14_encodesCorrectly() {
        val result = BcdEncoder.dateToBcd(2, 14)
        assertEquals(2, result.size)
        assertArrayEquals(byteArrayOf(0x02, 0x14), result)
    }

    // --- expiryDateToBcd ---

    @Test
    fun expiryDateToBcd_2812_encodesCorrectly() {
        val result = BcdEncoder.expiryDateToBcd("2812")
        assertEquals(2, result.size)
        assertArrayEquals(byteArrayOf(0x28, 0x12), result)
    }

    // --- currencyToBcd ---

    @Test
    fun currencyToBcd_978_encodesEuro() {
        val result = BcdEncoder.currencyToBcd(978)
        assertEquals(2, result.size)
        assertArrayEquals(byteArrayOf(0x09, 0x78), result)
    }

    // --- terminalIdToBcd ---

    @Test
    fun terminalIdToBcd_29001234_encodesCorrectly() {
        val result = BcdEncoder.terminalIdToBcd("29001234")
        assertEquals(4, result.size)
        assertArrayEquals(byteArrayOf(0x29, 0x00, 0x12, 0x34), result)
    }

    // --- panToBcd ---

    @Test
    fun panToBcd_16digits_eMasksMiddle() {
        // PAN: 6763890000001230
        // First 6: 676389, Last 4: 1230, Middle masked: 000000 -> EEEEEE
        // Masked: 6 7 6 3 8 9 E E E E E E 1 2 3 0
        // Bytes:  0x67 0x63 0x89 0xEE 0xEE 0xEE 0x12 0x30
        val result = BcdEncoder.panToBcd("6763890000001230")
        assertEquals(8, result.size)
        assertEquals(0x67.toByte(), result[0])
        assertEquals(0x63.toByte(), result[1])
        assertEquals(0x89.toByte(), result[2])
        // Middle bytes should have E nibbles
        assertEquals(0xEE.toByte(), result[3])
        assertEquals(0xEE.toByte(), result[4])
        assertEquals(0xEE.toByte(), result[5])
        // Last 4 digits: 1230
        assertEquals(0x12.toByte(), result[6])
        assertEquals(0x30.toByte(), result[7])
    }

    // --- stringToBcd / bcdToString round-trip ---

    @Test
    fun stringToBcd_bcdToString_roundTrip() {
        val original = "1234567890"
        val bcd = BcdEncoder.stringToBcd(original)
        val decoded = BcdEncoder.bcdToString(bcd)
        assertEquals(original, decoded)
    }

    @Test
    fun stringToBcd_oddLength_padsWithZero() {
        // Odd-length "123" should be padded to "0123" -> [0x01, 0x23]
        val result = BcdEncoder.stringToBcd("123")
        assertEquals(2, result.size)
        assertArrayEquals(byteArrayOf(0x01, 0x23), result)
    }

    // --- cardSequenceNrToBcd ---

    @Test
    fun cardSequenceNrToBcd_encodesCorrectly() {
        val result = BcdEncoder.cardSequenceNrToBcd(5)
        assertEquals(2, result.size)
        assertArrayEquals(byteArrayOf(0x00, 0x05), result)
    }
}
