package com.panda.zvt.simulator.state

import com.panda.zvt.simulator.config.SimulatedCardData
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

class TransactionStoreTest {

    private lateinit var store: TransactionStore

    @Before
    fun setUp() {
        store = TransactionStore()
    }

    private fun createTransaction(
        type: String = "Payment",
        amount: Long = 1000L,
        trace: Int = 1,
        receipt: Int = 1,
        turnover: Int = 1,
        cardData: SimulatedCardData = SimulatedCardData(),
        resultCode: Byte = 0x00
    ): StoredTransaction = StoredTransaction(
        type = type,
        amount = amount,
        trace = trace,
        receipt = receipt,
        turnover = turnover,
        cardData = cardData,
        resultCode = resultCode
    )

    @Test
    fun `empty store - getAllTransactions is empty and getLastTransaction is null`() {
        assertTrue(store.getAllTransactions().isEmpty())
        assertNull(store.getLastTransaction())
        assertEquals(0, store.getTransactionCount())
        assertEquals(0L, store.getTotalAmount())
    }

    @Test
    fun `recordTransaction makes getLastTransaction return it`() {
        val txn = createTransaction(amount = 2500L, trace = 1)
        store.recordTransaction(txn)

        val last = store.getLastTransaction()
        assertNotNull(last)
        assertEquals(2500L, last!!.amount)
        assertEquals(1, last.trace)
    }

    @Test
    fun `recordTransaction adds to getAllTransactions`() {
        store.recordTransaction(createTransaction())
        assertEquals(1, store.getAllTransactions().size)
    }

    @Test
    fun `multiple records produce correct getTransactionCount`() {
        store.recordTransaction(createTransaction(trace = 1))
        store.recordTransaction(createTransaction(trace = 2))
        store.recordTransaction(createTransaction(trace = 3))
        assertEquals(3, store.getTransactionCount())
    }

    @Test
    fun `multiple records produce correct getTotalAmount`() {
        store.recordTransaction(createTransaction(amount = 1000L))
        store.recordTransaction(createTransaction(amount = 2500L))
        store.recordTransaction(createTransaction(amount = 500L))
        assertEquals(4000L, store.getTotalAmount())
    }

    @Test
    fun `clearBatch empties transactions but preserves lastTransaction`() {
        store.recordTransaction(createTransaction(amount = 1000L, trace = 1))
        store.recordTransaction(createTransaction(amount = 2000L, trace = 2))

        store.clearBatch()

        assertTrue(store.getAllTransactions().isEmpty())
        assertEquals(0, store.getTransactionCount())
        assertNotNull(store.getLastTransaction())
        assertEquals(2, store.getLastTransaction()!!.trace)
    }

    @Test
    fun `clearAll empties transactions and clears lastTransaction`() {
        store.recordTransaction(createTransaction(amount = 1000L))
        store.recordTransaction(createTransaction(amount = 2000L))

        store.clearAll()

        assertTrue(store.getAllTransactions().isEmpty())
        assertNull(store.getLastTransaction())
    }

    @Test
    fun `toInfo extension maps fields correctly`() {
        val txn = createTransaction(
            type = "Refund",
            amount = 4250L,
            trace = 7,
            receipt = 3,
            turnover = 5,
            cardData = SimulatedCardData(cardName = "Visa"),
            resultCode = 0x00
        )
        val info = txn.toInfo()

        assertEquals("Refund", info.type)
        assertEquals(4250L, info.amountCents)
        assertEquals(7, info.trace)
        assertEquals(3, info.receipt)
        assertEquals(5, info.turnover)
        assertEquals("Visa", info.cardName)
        assertTrue(info.success)
    }

    @Test
    fun `toInfo with resultCode 0x00 sets success true`() {
        val txn = createTransaction(resultCode = 0x00)
        assertTrue(txn.toInfo().success)
    }

    @Test
    fun `toInfo with non-zero resultCode sets success false`() {
        val txn = createTransaction(resultCode = 0x6C.toByte())
        assertFalse(txn.toInfo().success)
    }
}
