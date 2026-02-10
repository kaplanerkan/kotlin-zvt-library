package com.panda.zvt.simulator.state

import com.panda.zvt.simulator.config.SimulatedCardData
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentLinkedQueue

class TransactionStore {

    private val transactions = ConcurrentLinkedQueue<StoredTransaction>()
    @Volatile
    private var lastTransaction: StoredTransaction? = null

    fun recordTransaction(txn: StoredTransaction) {
        transactions.add(txn)
        lastTransaction = txn
    }

    fun getLastTransaction(): StoredTransaction? = lastTransaction

    fun getAllTransactions(): List<StoredTransaction> = transactions.toList()

    fun getTransactionCount(): Int = transactions.size

    fun getTotalAmount(): Long = transactions.sumOf { it.amount }

    fun clearBatch() {
        transactions.clear()
    }

    fun clearAll() {
        transactions.clear()
        lastTransaction = null
    }
}

data class StoredTransaction(
    val type: String,
    val amount: Long,
    val trace: Int,
    val receipt: Int,
    val turnover: Int,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val cardData: SimulatedCardData,
    val resultCode: Byte = 0x00
)

@Serializable
data class TransactionInfo(
    val type: String,
    val amountCents: Long,
    val amountFormatted: String,
    val trace: Int,
    val receipt: Int,
    val turnover: Int,
    val timestamp: String,
    val cardName: String,
    val success: Boolean
)

fun StoredTransaction.toInfo() = TransactionInfo(
    type = type,
    amountCents = amount,
    amountFormatted = "%.2f EUR".format(amount / 100.0),
    trace = trace,
    receipt = receipt,
    turnover = turnover,
    timestamp = timestamp.toString(),
    cardName = cardData.cardName,
    success = resultCode == 0x00.toByte()
)
