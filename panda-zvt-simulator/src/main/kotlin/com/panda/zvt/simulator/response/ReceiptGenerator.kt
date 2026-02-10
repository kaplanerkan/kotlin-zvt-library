package com.panda.zvt.simulator.response

import com.panda.zvt.simulator.config.SimulatorConfig
import com.panda.zvt.simulator.state.StoredTransaction
import com.panda.zvt.simulator.state.TransactionStore
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object ReceiptGenerator {

    fun generateEndOfDayReceipt(store: TransactionStore, config: SimulatorConfig): List<String> {
        val transactions = store.getAllTransactions()
        val total = transactions.sumOf { it.amount }
        val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        return listOf(
            "================================",
            "       TAGESABSCHLUSS          ",
            "================================",
            "Terminal: ${config.terminalId}",
            "VU:       ${config.vuNumber.trim()}",
            "Datum:    $dateStr",
            "--------------------------------",
            "Anzahl:   ${transactions.size}",
            "Gesamt:   ${formatAmount(total)}",
            "================================",
            "     BATCH ABGESCHLOSSEN       ",
            "================================"
        )
    }

    fun generateTransactionReceipt(txn: StoredTransaction, config: SimulatorConfig): List<String> {
        val dateStr = txn.timestamp.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        val timeStr = txn.timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        return listOf(
            "================================",
            "       ${txn.type.uppercase().padEnd(20)}",
            "================================",
            "Terminal: ${config.terminalId}",
            "Datum:    $dateStr  $timeStr",
            "Trace:    ${txn.trace}",
            "Beleg:    ${txn.receipt}",
            "Karte:    ${txn.cardData.cardName}",
            "Betrag:   ${formatAmount(txn.amount)}",
            "================================",
            "       GENEHMIGT               ",
            "================================"
        )
    }

    private fun formatAmount(cents: Long): String {
        return "%d,%02d EUR".format(cents / 100, cents % 100)
    }
}
