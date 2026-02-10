package com.panda.zvt_library

import com.panda.zvt_library.protocol.ZvtCommandBuilder
import com.panda.zvt_library.protocol.ZvtConstants
import org.junit.Test
import org.junit.Assert.*

class ZvtCommandBuilderTest {

    // =====================================================
    // 1. Registration (06 00) - default parameters
    // =====================================================

    @Test
    fun buildRegistration_default_commandIs0600AndDataStartsWithPasswordAndConfigByte() {
        val packet = ZvtCommandBuilder.buildRegistration()

        // Command must be 06 00
        assertArrayEquals(byteArrayOf(0x06, 0x00), packet.command)

        // Data: password BCD "000000" = [0x00, 0x00, 0x00], configByte = 0x08
        assertTrue("Data must have at least 4 bytes", packet.data.size >= 4)
        assertEquals(0x00.toByte(), packet.data[0]) // password byte 1
        assertEquals(0x00.toByte(), packet.data[1]) // password byte 2
        assertEquals(0x00.toByte(), packet.data[2]) // password byte 3
        assertEquals(0x08.toByte(), packet.data[3]) // configByte = REG_INTERMEDIATE_STATUS
    }

    // =====================================================
    // 2. Registration - custom configByte
    // =====================================================

    @Test
    fun buildRegistration_customConfigByte_configByteAppearsInData() {
        val customConfig: Byte = 0x0E // combined flags
        val packet = ZvtCommandBuilder.buildRegistration(configByte = customConfig)

        assertArrayEquals(byteArrayOf(0x06, 0x00), packet.command)
        assertEquals(customConfig, packet.data[3])
    }

    // =====================================================
    // 3. Authorization (cents) - 1250 cents
    // =====================================================

    @Test
    fun buildAuthorization_1250cents_commandIs0601AndAmountBcdCorrect() {
        val packet = ZvtCommandBuilder.buildAuthorization(amountInCents = 1250L)

        // Command must be 06 01
        assertArrayEquals(byteArrayOf(0x06, 0x01), packet.command)

        // data[0] = BMP_AMOUNT (0x04)
        assertEquals(ZvtConstants.BMP_AMOUNT, packet.data[0])

        // BCD of 1250 padded to 12 digits "000000001250" = [0x00, 0x00, 0x00, 0x00, 0x12, 0x50]
        val expectedAmountBcd = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x12, 0x50)
        val actualAmountBcd = packet.data.copyOfRange(1, 7)
        assertArrayEquals(expectedAmountBcd, actualAmountBcd)

        // After amount: BMP_CURRENCY_CODE (0x49)
        assertEquals(ZvtConstants.BMP_CURRENCY_CODE, packet.data[7])

        // Currency BCD for 978: "0978" = [0x09, 0x78]
        val expectedCurrencyBcd = byteArrayOf(0x09, 0x78)
        val actualCurrencyBcd = packet.data.copyOfRange(8, 10)
        assertArrayEquals(expectedCurrencyBcd, actualCurrencyBcd)
    }

    // =====================================================
    // 4. Authorization (cents) - 0 throws
    // =====================================================

    @Test(expected = IllegalArgumentException::class)
    fun buildAuthorization_zeroAmount_throwsIllegalArgumentException() {
        ZvtCommandBuilder.buildAuthorization(amountInCents = 0L)
    }

    // =====================================================
    // 5. Authorization (Double euro) - 12.50
    // =====================================================

    @Test
    fun buildAuthorization_doubleEuro_convertsCorrectlyToCents() {
        val packet = ZvtCommandBuilder.buildAuthorization(amountEuro = 12.50)

        // Same command
        assertArrayEquals(byteArrayOf(0x06, 0x01), packet.command)

        // Same BCD amount as 1250 cents
        assertEquals(ZvtConstants.BMP_AMOUNT, packet.data[0])
        val expectedAmountBcd = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x12, 0x50)
        val actualAmountBcd = packet.data.copyOfRange(1, 7)
        assertArrayEquals(expectedAmountBcd, actualAmountBcd)
    }

    // =====================================================
    // 6. Reversal - no receipt number
    // =====================================================

    @Test
    fun buildReversal_noReceipt_commandIs0630AndDataIsPasswordOnly() {
        val packet = ZvtCommandBuilder.buildReversal(receiptNumber = null)

        assertArrayEquals(byteArrayOf(0x06, 0x30), packet.command)

        // Data = password BCD "000000" = 3 bytes only
        assertEquals(3, packet.data.size)
        assertEquals(0x00.toByte(), packet.data[0])
        assertEquals(0x00.toByte(), packet.data[1])
        assertEquals(0x00.toByte(), packet.data[2])
    }

    // =====================================================
    // 7. Reversal - with receipt number 42
    // =====================================================

    @Test
    fun buildReversal_withReceipt42_dataHasPasswordThenBmpReceiptAndBcd0042() {
        val packet = ZvtCommandBuilder.buildReversal(receiptNumber = 42)

        assertArrayEquals(byteArrayOf(0x06, 0x30), packet.command)

        // Password (3 bytes)
        assertEquals(0x00.toByte(), packet.data[0])
        assertEquals(0x00.toByte(), packet.data[1])
        assertEquals(0x00.toByte(), packet.data[2])

        // BMP_RECEIPT_NR (0x87)
        assertEquals(ZvtConstants.BMP_RECEIPT_NR, packet.data[3])

        // Receipt "0042" BCD = [0x00, 0x42]
        assertEquals(0x00.toByte(), packet.data[4])
        assertEquals(0x42.toByte(), packet.data[5])
    }

    // =====================================================
    // 8. Refund - 500 cents
    // =====================================================

    @Test
    fun buildRefund_500cents_commandIs0631AndDataHasPasswordAmountCurrency() {
        val packet = ZvtCommandBuilder.buildRefund(amountInCents = 500L)

        assertArrayEquals(byteArrayOf(0x06, 0x31), packet.command)

        // Password (3 bytes)
        assertEquals(0x00.toByte(), packet.data[0])
        assertEquals(0x00.toByte(), packet.data[1])
        assertEquals(0x00.toByte(), packet.data[2])

        // BMP_AMOUNT (0x04)
        assertEquals(ZvtConstants.BMP_AMOUNT, packet.data[3])

        // Amount 500 -> "000000000500" BCD = [0x00, 0x00, 0x00, 0x00, 0x05, 0x00]
        val expectedAmountBcd = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x05, 0x00)
        val actualAmountBcd = packet.data.copyOfRange(4, 10)
        assertArrayEquals(expectedAmountBcd, actualAmountBcd)

        // BMP_CURRENCY_CODE (0x49)
        assertEquals(ZvtConstants.BMP_CURRENCY_CODE, packet.data[10])

        // Currency 978 BCD = [0x09, 0x78]
        val expectedCurrencyBcd = byteArrayOf(0x09, 0x78)
        val actualCurrencyBcd = packet.data.copyOfRange(11, 13)
        assertArrayEquals(expectedCurrencyBcd, actualCurrencyBcd)
    }

    // =====================================================
    // 9. Refund - 0 throws
    // =====================================================

    @Test(expected = IllegalArgumentException::class)
    fun buildRefund_zeroAmount_throwsIllegalArgumentException() {
        ZvtCommandBuilder.buildRefund(amountInCents = 0L)
    }

    // =====================================================
    // 10. End of Day
    // =====================================================

    @Test
    fun buildEndOfDay_commandIs0650AndDataIsPassword() {
        val packet = ZvtCommandBuilder.buildEndOfDay()

        assertArrayEquals(byteArrayOf(0x06, 0x50), packet.command)

        // Data = password BCD "000000" = 3 bytes
        assertEquals(3, packet.data.size)
        assertEquals(0x00.toByte(), packet.data[0])
        assertEquals(0x00.toByte(), packet.data[1])
        assertEquals(0x00.toByte(), packet.data[2])
    }

    // =====================================================
    // 11. Diagnosis
    // =====================================================

    @Test
    fun buildDiagnosis_commandIs0670AndDataIsEmpty() {
        val packet = ZvtCommandBuilder.buildDiagnosis()

        assertArrayEquals(byteArrayOf(0x06, 0x70), packet.command)
        assertEquals(0, packet.data.size)
    }

    // =====================================================
    // 12. Status Enquiry
    // =====================================================

    @Test
    fun buildStatusEnquiry_commandIs0501AndDataIsEmpty() {
        val packet = ZvtCommandBuilder.buildStatusEnquiry()

        assertArrayEquals(byteArrayOf(0x05, 0x01), packet.command)
        assertEquals(0, packet.data.size)
    }

    // =====================================================
    // 13. Abort
    // =====================================================

    @Test
    fun buildAbort_commandIs06B0AndDataIsEmpty() {
        val packet = ZvtCommandBuilder.buildAbort()

        assertArrayEquals(byteArrayOf(0x06, 0xB0.toByte()), packet.command)
        assertEquals(0, packet.data.size)
    }

    // =====================================================
    // 14. Log Off
    // =====================================================

    @Test
    fun buildLogOff_commandIs0602AndDataIsEmpty() {
        val packet = ZvtCommandBuilder.buildLogOff()

        assertArrayEquals(byteArrayOf(0x06, 0x02), packet.command)
        assertEquals(0, packet.data.size)
    }

    // =====================================================
    // 15. Pre-Authorization - 5000 cents
    // =====================================================

    @Test
    fun buildPreAuthorization_5000cents_commandIs0622AndDataStartsWithBmpAmount() {
        val packet = ZvtCommandBuilder.buildPreAuthorization(amountInCents = 5000L)

        assertArrayEquals(byteArrayOf(0x06, 0x22), packet.command)

        // data[0] = BMP_AMOUNT (0x04)
        assertEquals(ZvtConstants.BMP_AMOUNT, packet.data[0])

        // Amount 5000 -> "000000005000" BCD = [0x00, 0x00, 0x00, 0x00, 0x50, 0x00]
        val expectedAmountBcd = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x50, 0x00)
        val actualAmountBcd = packet.data.copyOfRange(1, 7)
        assertArrayEquals(expectedAmountBcd, actualAmountBcd)
    }

    // =====================================================
    // 16. Pre-Authorization - 0 throws
    // =====================================================

    @Test(expected = IllegalArgumentException::class)
    fun buildPreAuthorization_zeroAmount_throwsIllegalArgumentException() {
        ZvtCommandBuilder.buildPreAuthorization(amountInCents = 0L)
    }

    // =====================================================
    // 17. Book Total - receipt number 1
    // =====================================================

    @Test
    fun buildBookTotal_receiptNumber1_commandIs0624AndDataStartsWithBmpReceipt() {
        val packet = ZvtCommandBuilder.buildBookTotal(receiptNumber = 1)

        assertArrayEquals(byteArrayOf(0x06, 0x24), packet.command)

        // data[0] = BMP_RECEIPT_NR (0x87)
        assertEquals(ZvtConstants.BMP_RECEIPT_NR, packet.data[0])

        // Receipt "0001" BCD = [0x00, 0x01]
        assertEquals(0x00.toByte(), packet.data[1])
        assertEquals(0x01.toByte(), packet.data[2])
    }

    // =====================================================
    // 18. Pre-Auth Reversal - receipt number 5
    // =====================================================

    @Test
    fun buildPreAuthReversal_receiptNumber5_commandIs0625AndDataStartsWithBmpReceipt() {
        val packet = ZvtCommandBuilder.buildPreAuthReversal(receiptNumber = 5)

        assertArrayEquals(byteArrayOf(0x06, 0x25), packet.command)

        // data[0] = BMP_RECEIPT_NR (0x87)
        assertEquals(ZvtConstants.BMP_RECEIPT_NR, packet.data[0])

        // Receipt "0005" BCD = [0x00, 0x05]
        assertEquals(0x00.toByte(), packet.data[1])
        assertEquals(0x05.toByte(), packet.data[2])
    }

    // =====================================================
    // 19. Repeat Receipt
    // =====================================================

    @Test
    fun buildRepeatReceipt_commandIs0620AndDataIsPassword() {
        val packet = ZvtCommandBuilder.buildRepeatReceipt()

        assertArrayEquals(byteArrayOf(0x06, 0x20), packet.command)

        // Data = password BCD "000000" = 3 bytes
        assertEquals(3, packet.data.size)
        assertEquals(0x00.toByte(), packet.data[0])
        assertEquals(0x00.toByte(), packet.data[1])
        assertEquals(0x00.toByte(), packet.data[2])
    }

    // =====================================================
    // 20. Print Line ACK
    // =====================================================

    @Test
    fun buildPrintLineAck_returnsAckPacket() {
        val packet = ZvtCommandBuilder.buildPrintLineAck()

        assertTrue("Packet must be an ACK", packet.isAck)
    }
}
