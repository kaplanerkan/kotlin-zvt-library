package com.panda.zvt.simulator.response

import com.panda.zvt.simulator.config.SimulatedCardData
import com.panda.zvt.simulator.config.SimulatorConfig
import com.panda.zvt.simulator.state.StoredTransaction
import com.panda.zvt.simulator.state.TransactionStore
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class StatusInfoBuilderTest {

    private val defaultConfig = SimulatorConfig()

    // =========================================================================
    // buildPaymentStatusInfo
    // =========================================================================

    @Test
    fun buildPaymentStatusInfo_startsWithStatusInfoCommand() {
        val result = StatusInfoBuilder.buildPaymentStatusInfo(
            amount = 1250L,
            trace = 1,
            receipt = 1,
            turnover = 1,
            dateTime = LocalDateTime.of(2026, 2, 10, 14, 30, 0),
            config = defaultConfig
        )
        assertEquals(0x04.toByte(), result[0])
        assertEquals(0x0F.toByte(), result[1])
        assertTrue("PaymentStatusInfo should have more than 10 bytes", result.size > 10)
    }

    @Test
    fun buildPaymentStatusInfo_containsSuccessResultCode() {
        val result = StatusInfoBuilder.buildPaymentStatusInfo(
            amount = 1250L,
            trace = 1,
            receipt = 1,
            turnover = 1,
            dateTime = LocalDateTime.of(2026, 2, 10, 14, 30, 0),
            config = defaultConfig
        )
        // Data starts after command (2 bytes) + length (1 byte) = index 3
        // First BMP in data is result code: BMP_RESULT_CODE (0x27) + value (0x00)
        val dataStart = if (result[2] == 0xFF.toByte()) 5 else 3
        var foundResultCode = false
        for (i in dataStart until result.size - 1) {
            if (result[i] == 0x27.toByte() && result[i + 1] == 0x00.toByte()) {
                foundResultCode = true
                break
            }
        }
        assertTrue("PaymentStatusInfo should contain BMP 0x27 with value 0x00", foundResultCode)
    }

    // =========================================================================
    // buildRegistrationStatusInfo
    // =========================================================================

    @Test
    fun buildRegistrationStatusInfo_startsWithStatusInfoCommand() {
        val result = StatusInfoBuilder.buildRegistrationStatusInfo(defaultConfig)
        assertEquals(0x04.toByte(), result[0])
        assertEquals(0x0F.toByte(), result[1])
    }

    // =========================================================================
    // buildDiagnosisStatusInfo
    // =========================================================================

    @Test
    fun buildDiagnosisStatusInfo_startsWithStatusInfoCommand() {
        val result = StatusInfoBuilder.buildDiagnosisStatusInfo(defaultConfig)
        assertEquals(0x04.toByte(), result[0])
        assertEquals(0x0F.toByte(), result[1])
        assertTrue("DiagnosisStatusInfo should have more than 5 bytes", result.size > 5)
    }

    // =========================================================================
    // buildErrorStatusInfo
    // =========================================================================

    @Test
    fun buildErrorStatusInfo_containsErrorCode() {
        val result = StatusInfoBuilder.buildErrorStatusInfo(0x6C.toByte(), defaultConfig)
        // Should contain BMP_RESULT_CODE (0x27) followed by error code 0x6C
        val dataStart = if (result[2] == 0xFF.toByte()) 5 else 3
        var foundErrorCode = false
        for (i in dataStart until result.size - 1) {
            if (result[i] == 0x27.toByte() && result[i + 1] == 0x6C.toByte()) {
                foundErrorCode = true
                break
            }
        }
        assertTrue("ErrorStatusInfo should contain BMP 0x27 with value 0x6C", foundErrorCode)
    }

    // =========================================================================
    // buildEndOfDayStatusInfo
    // =========================================================================

    @Test
    fun buildEndOfDayStatusInfo_withEmptyStore_startsWithStatusInfoCommand() {
        val store = TransactionStore()
        val result = StatusInfoBuilder.buildEndOfDayStatusInfo(store, defaultConfig)
        assertEquals(0x04.toByte(), result[0])
        assertEquals(0x0F.toByte(), result[1])
    }

    // =========================================================================
    // buildRepeatReceiptStatusInfo
    // =========================================================================

    @Test
    fun buildRepeatReceiptStatusInfo_withEmptyStore_returnsError() {
        val store = TransactionStore()
        val result = StatusInfoBuilder.buildRepeatReceiptStatusInfo(store, defaultConfig)
        // Should contain BMP_RESULT_CODE (0x27) followed by 0x6D (no transaction)
        val dataStart = if (result[2] == 0xFF.toByte()) 5 else 3
        var foundError = false
        for (i in dataStart until result.size - 1) {
            if (result[i] == 0x27.toByte() && result[i + 1] == 0x6D.toByte()) {
                foundError = true
                break
            }
        }
        assertTrue("RepeatReceiptStatusInfo with empty store should contain 0x27, 0x6D", foundError)
    }

    @Test
    fun buildRepeatReceiptStatusInfo_withTransaction_containsTransactionData() {
        val store = TransactionStore()
        store.recordTransaction(
            StoredTransaction(
                type = "Payment",
                amount = 1250L,
                trace = 1,
                receipt = 1,
                turnover = 1,
                timestamp = LocalDateTime.of(2026, 2, 10, 14, 30, 0),
                cardData = SimulatedCardData()
            )
        )
        val result = StatusInfoBuilder.buildRepeatReceiptStatusInfo(store, defaultConfig)
        // Should start with StatusInfo command
        assertEquals(0x04.toByte(), result[0])
        assertEquals(0x0F.toByte(), result[1])
        // Should contain success result code (0x27, 0x00), not error
        val dataStart = if (result[2] == 0xFF.toByte()) 5 else 3
        var foundSuccess = false
        for (i in dataStart until result.size - 1) {
            if (result[i] == 0x27.toByte() && result[i + 1] == 0x00.toByte()) {
                foundSuccess = true
                break
            }
        }
        assertTrue("RepeatReceiptStatusInfo with transaction should contain success code", foundSuccess)
        // Should be a full payment status info (larger packet)
        assertTrue("Result should be larger than a simple error response", result.size > 10)
    }
}
