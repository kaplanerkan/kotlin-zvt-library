package com.panda.zvt_library

import com.panda.zvt_library.util.BcdHelper
import org.junit.Test
import org.junit.Assert.*

class BcdHelperTest {

    // =====================================================
    // 1. amountToBcd / bcdToAmount round-trips
    // =====================================================

    @Test
    fun amountToBcd_zero_returnsAllZeroBytes() {
        val bcd = BcdHelper.amountToBcd(0L)
        assertEquals(6, bcd.size)
        assertArrayEquals(byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00), bcd)
        assertEquals(0L, BcdHelper.bcdToAmount(bcd))
    }

    @Test
    fun amountToBcd_one_roundTrips() {
        val bcd = BcdHelper.amountToBcd(1L)
        assertArrayEquals(byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x01), bcd)
        assertEquals(1L, BcdHelper.bcdToAmount(bcd))
    }

    @Test
    fun amountToBcd_100_roundTrips() {
        val bcd = BcdHelper.amountToBcd(100L)
        assertArrayEquals(byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x01, 0x00), bcd)
        assertEquals(100L, BcdHelper.bcdToAmount(bcd))
    }

    @Test
    fun amountToBcd_1250_roundTrips() {
        val bcd = BcdHelper.amountToBcd(1250L)
        assertArrayEquals(byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x12, 0x50), bcd)
        assertEquals(1250L, BcdHelper.bcdToAmount(bcd))
    }

    @Test
    fun amountToBcd_maxValue_roundTrips() {
        val bcd = BcdHelper.amountToBcd(999999999999L)
        assertArrayEquals(
            byteArrayOf(0x99.toByte(), 0x99.toByte(), 0x99.toByte(), 0x99.toByte(), 0x99.toByte(), 0x99.toByte()),
            bcd
        )
        assertEquals(999999999999L, BcdHelper.bcdToAmount(bcd))
    }

    // =====================================================
    // 2. amountToBcd negative -> IllegalArgumentException
    // =====================================================

    @Test(expected = IllegalArgumentException::class)
    fun amountToBcd_negative_throwsException() {
        BcdHelper.amountToBcd(-1L)
    }

    // =====================================================
    // 3. amountToBcd too large -> IllegalArgumentException
    // =====================================================

    @Test(expected = IllegalArgumentException::class)
    fun amountToBcd_tooLarge_throwsException() {
        BcdHelper.amountToBcd(1000000000000L)
    }

    // =====================================================
    // 4. euroToBcd / bcdToEuro round-trips
    // =====================================================

    @Test
    fun euroToBcd_zero_roundTrips() {
        val bcd = BcdHelper.euroToBcd(0.0)
        assertEquals(0.0, BcdHelper.bcdToEuro(bcd), 0.001)
    }

    @Test
    fun euroToBcd_12_50_roundTrips() {
        val bcd = BcdHelper.euroToBcd(12.50)
        assertArrayEquals(byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x12, 0x50), bcd)
        assertEquals(12.50, BcdHelper.bcdToEuro(bcd), 0.001)
    }

    @Test
    fun euroToBcd_99_99_roundTrips() {
        val bcd = BcdHelper.euroToBcd(99.99)
        assertArrayEquals(byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x99.toByte(), 0x99.toByte()), bcd)
        assertEquals(99.99, BcdHelper.bcdToEuro(bcd), 0.001)
    }

    // =====================================================
    // 5. timeToBcd / bcdToTime round-trips
    // =====================================================

    @Test
    fun timeToBcd_midnight_roundTrips() {
        val bcd = BcdHelper.timeToBcd(0, 0, 0)
        assertEquals(3, bcd.size)
        assertArrayEquals(byteArrayOf(0x00, 0x00, 0x00), bcd)
        val (h, m, s) = BcdHelper.bcdToTime(bcd)
        assertEquals(0, h)
        assertEquals(0, m)
        assertEquals(0, s)
    }

    @Test
    fun timeToBcd_endOfDay_roundTrips() {
        val bcd = BcdHelper.timeToBcd(23, 59, 59)
        assertArrayEquals(byteArrayOf(0x23, 0x59, 0x59), bcd)
        val (h, m, s) = BcdHelper.bcdToTime(bcd)
        assertEquals(23, h)
        assertEquals(59, m)
        assertEquals(59, s)
    }

    @Test
    fun timeToBcd_midDay_roundTrips() {
        val bcd = BcdHelper.timeToBcd(12, 30, 45)
        assertArrayEquals(byteArrayOf(0x12, 0x30, 0x45), bcd)
        val (h, m, s) = BcdHelper.bcdToTime(bcd)
        assertEquals(12, h)
        assertEquals(30, m)
        assertEquals(45, s)
    }

    // =====================================================
    // 6. timeToBcd invalid hour -> IllegalArgumentException
    // =====================================================

    @Test(expected = IllegalArgumentException::class)
    fun timeToBcd_invalidHour_throwsException() {
        BcdHelper.timeToBcd(24, 0, 0)
    }

    // =====================================================
    // 7. dateToBcd / bcdToDate round-trips
    // =====================================================

    @Test
    fun dateToBcd_jan1_roundTrips() {
        val bcd = BcdHelper.dateToBcd(1, 1)
        assertEquals(2, bcd.size)
        assertArrayEquals(byteArrayOf(0x01, 0x01), bcd)
        val (month, day) = BcdHelper.bcdToDate(bcd)
        assertEquals(1, month)
        assertEquals(1, day)
    }

    @Test
    fun dateToBcd_dec31_roundTrips() {
        val bcd = BcdHelper.dateToBcd(12, 31)
        assertArrayEquals(byteArrayOf(0x12, 0x31), bcd)
        val (month, day) = BcdHelper.bcdToDate(bcd)
        assertEquals(12, month)
        assertEquals(31, day)
    }

    @Test
    fun dateToBcd_jun15_roundTrips() {
        val bcd = BcdHelper.dateToBcd(6, 15)
        assertArrayEquals(byteArrayOf(0x06, 0x15), bcd)
        val (month, day) = BcdHelper.bcdToDate(bcd)
        assertEquals(6, month)
        assertEquals(15, day)
    }

    // =====================================================
    // 8. dateToBcd invalid month -> IllegalArgumentException
    // =====================================================

    @Test(expected = IllegalArgumentException::class)
    fun dateToBcd_invalidMonth_throwsException() {
        BcdHelper.dateToBcd(13, 1)
    }

    // =====================================================
    // 9. traceNumberToBcd / bcdToTraceNumber round-trips
    // =====================================================

    @Test
    fun traceNumberToBcd_zero_roundTrips() {
        val bcd = BcdHelper.traceNumberToBcd(0)
        assertEquals(3, bcd.size)
        assertArrayEquals(byteArrayOf(0x00, 0x00, 0x00), bcd)
        assertEquals(0, BcdHelper.bcdToTraceNumber(bcd))
    }

    @Test
    fun traceNumberToBcd_one_roundTrips() {
        val bcd = BcdHelper.traceNumberToBcd(1)
        assertArrayEquals(byteArrayOf(0x00, 0x00, 0x01), bcd)
        assertEquals(1, BcdHelper.bcdToTraceNumber(bcd))
    }

    @Test
    fun traceNumberToBcd_max_roundTrips() {
        val bcd = BcdHelper.traceNumberToBcd(999999)
        assertArrayEquals(byteArrayOf(0x99.toByte(), 0x99.toByte(), 0x99.toByte()), bcd)
        assertEquals(999999, BcdHelper.bcdToTraceNumber(bcd))
    }

    // =====================================================
    // 10. traceNumberToBcd out of range -> IllegalArgumentException
    // =====================================================

    @Test(expected = IllegalArgumentException::class)
    fun traceNumberToBcd_negative_throwsException() {
        BcdHelper.traceNumberToBcd(-1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun traceNumberToBcd_tooLarge_throwsException() {
        BcdHelper.traceNumberToBcd(1000000)
    }

    // =====================================================
    // 11. currencyToBcd / bcdToCurrency
    // =====================================================

    @Test
    fun currencyToBcd_eur978_roundTrips() {
        val bcd = BcdHelper.currencyToBcd(978)
        assertEquals(2, bcd.size)
        assertArrayEquals(byteArrayOf(0x09, 0x78), bcd)
        assertEquals(978, BcdHelper.bcdToCurrency(bcd))
    }

    @Test
    fun currencyToBcd_try949_roundTrips() {
        val bcd = BcdHelper.currencyToBcd(949)
        assertArrayEquals(byteArrayOf(0x09, 0x49), bcd)
        assertEquals(949, BcdHelper.bcdToCurrency(bcd))
    }

    @Test
    fun currencyToBcd_usd840_roundTrips() {
        val bcd = BcdHelper.currencyToBcd(840)
        assertArrayEquals(byteArrayOf(0x08, 0x40), bcd)
        assertEquals(840, BcdHelper.bcdToCurrency(bcd))
    }

    // =====================================================
    // 12. stringToBcd / bcdToString round-trips
    // =====================================================

    @Test
    fun stringToBcd_001250_roundTrips() {
        val bcd = BcdHelper.stringToBcd("001250")
        assertArrayEquals(byteArrayOf(0x00, 0x12, 0x50), bcd)
        assertEquals("001250", BcdHelper.bcdToString(bcd))
    }

    @Test
    fun stringToBcd_123456_roundTrips() {
        val bcd = BcdHelper.stringToBcd("123456")
        assertArrayEquals(byteArrayOf(0x12, 0x34, 0x56), bcd)
        assertEquals("123456", BcdHelper.bcdToString(bcd))
    }

    // =====================================================
    // 13. stringToBcd odd length pads with leading 0
    // =====================================================

    @Test
    fun stringToBcd_oddLength_padsWithZero() {
        val bcd = BcdHelper.stringToBcd("123")
        // "123" -> padded to "0123" -> [0x01, 0x23]
        assertArrayEquals(byteArrayOf(0x01, 0x23), bcd)
        assertEquals("0123", BcdHelper.bcdToString(bcd))
    }

    // =====================================================
    // 14. stringToBcd non-digit -> IllegalArgumentException
    // =====================================================

    @Test(expected = IllegalArgumentException::class)
    fun stringToBcd_nonDigit_throwsException() {
        BcdHelper.stringToBcd("12AB")
    }

    // =====================================================
    // 15. bcdToPan with E-masking
    // =====================================================

    @Test
    fun bcdToPan_withEMasking_returnsMaskedPan() {
        val bcd = byteArrayOf(
            0x67, 0x63,
            0x89.toByte(), 0xEE.toByte(),
            0xEE.toByte(), 0xEE.toByte(),
            0x12, 0x30
        )
        assertEquals("676389******1230", BcdHelper.bcdToPan(bcd))
    }

    // =====================================================
    // 16. bcdToPan with F-padding
    // =====================================================

    @Test
    fun bcdToPan_withFPadding_skipsPaddedNibbles() {
        // PAN "12345" stored as [0x12, 0x34, 0x5F] -- F nibble is padding
        val bcd = byteArrayOf(0x12, 0x34, 0x5F)
        assertEquals("12345", BcdHelper.bcdToPan(bcd))
    }

    // =====================================================
    // 17. formatAmount
    // =====================================================

    @Test
    fun formatAmount_zero_returnsZeroCommaZeroZero() {
        assertEquals("0,00", BcdHelper.formatAmount(0L))
    }

    @Test
    fun formatAmount_1250_returns12Comma50() {
        assertEquals("12,50", BcdHelper.formatAmount(1250L))
    }

    @Test
    fun formatAmount_100_returns1Comma00() {
        assertEquals("1,00", BcdHelper.formatAmount(100L))
    }
}
