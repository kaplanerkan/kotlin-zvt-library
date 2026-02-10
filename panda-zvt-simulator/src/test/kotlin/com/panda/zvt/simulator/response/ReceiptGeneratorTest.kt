package com.panda.zvt.simulator.response

import com.panda.zvt.simulator.config.SimulatedCardData
import com.panda.zvt.simulator.config.SimulatorConfig
import com.panda.zvt.simulator.state.StoredTransaction
import com.panda.zvt.simulator.state.TransactionStore
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class ReceiptGeneratorTest {

    private val defaultConfig = SimulatorConfig()

    // =========================================================================
    // generateEndOfDayReceipt
    // =========================================================================

    @Test
    fun generateEndOfDayReceipt_returnsNonEmptyList() {
        val store = TransactionStore()
        val lines = ReceiptGenerator.generateEndOfDayReceipt(store, defaultConfig)
        assertTrue("End-of-day receipt should not be empty", lines.isNotEmpty())
    }

    @Test
    fun generateEndOfDayReceipt_containsTagesabschluss() {
        val store = TransactionStore()
        val lines = ReceiptGenerator.generateEndOfDayReceipt(store, defaultConfig)
        val joined = lines.joinToString("\n")
        assertTrue(
            "End-of-day receipt should contain 'TAGESABSCHLUSS'",
            joined.contains("TAGESABSCHLUSS")
        )
    }

    @Test
    fun generateEndOfDayReceipt_containsTerminalId() {
        val store = TransactionStore()
        val lines = ReceiptGenerator.generateEndOfDayReceipt(store, defaultConfig)
        val joined = lines.joinToString("\n")
        assertTrue(
            "End-of-day receipt should contain terminal ID '29001234'",
            joined.contains("29001234")
        )
    }

    // =========================================================================
    // generateTransactionReceipt
    // =========================================================================

    @Test
    fun generateTransactionReceipt_returnsNonEmptyList() {
        val txn = StoredTransaction(
            type = "Payment",
            amount = 1250L,
            trace = 1,
            receipt = 1,
            turnover = 1,
            timestamp = LocalDateTime.of(2026, 2, 10, 14, 30, 0),
            cardData = SimulatedCardData()
        )
        val lines = ReceiptGenerator.generateTransactionReceipt(txn, defaultConfig)
        assertTrue("Transaction receipt should not be empty", lines.isNotEmpty())
    }

    @Test
    fun generateTransactionReceipt_containsCardName() {
        val txn = StoredTransaction(
            type = "Payment",
            amount = 1250L,
            trace = 1,
            receipt = 1,
            turnover = 1,
            timestamp = LocalDateTime.of(2026, 2, 10, 14, 30, 0),
            cardData = SimulatedCardData()
        )
        val lines = ReceiptGenerator.generateTransactionReceipt(txn, defaultConfig)
        val joined = lines.joinToString("\n")
        assertTrue(
            "Transaction receipt should contain card name 'Mastercard'",
            joined.contains("Mastercard")
        )
    }

    @Test
    fun generateTransactionReceipt_containsFormattedAmount() {
        val txn = StoredTransaction(
            type = "Payment",
            amount = 1250L,
            trace = 1,
            receipt = 1,
            turnover = 1,
            timestamp = LocalDateTime.of(2026, 2, 10, 14, 30, 0),
            cardData = SimulatedCardData()
        )
        val lines = ReceiptGenerator.generateTransactionReceipt(txn, defaultConfig)
        val joined = lines.joinToString("\n")
        // formatAmount(1250) = "12,50 EUR"
        assertTrue(
            "Transaction receipt should contain formatted amount '12,50 EUR'",
            joined.contains("12,50 EUR")
        )
    }
}
