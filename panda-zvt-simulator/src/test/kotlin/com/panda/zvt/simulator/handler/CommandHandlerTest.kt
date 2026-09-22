package com.panda.zvt.simulator.handler

import com.panda.zvt.simulator.config.ErrorSimulation
import com.panda.zvt.simulator.config.SimulatedCardData
import com.panda.zvt.simulator.config.SimulatorConfig
import com.panda.zvt.simulator.protocol.ApduParser
import com.panda.zvt.simulator.protocol.BcdEncoder
import com.panda.zvt.simulator.state.SimulatorState
import com.panda.zvt.simulator.state.TransactionStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CommandHandlerTest {

    private lateinit var state: SimulatorState
    private lateinit var store: TransactionStore

    private val defaultConfig = SimulatorConfig()

    /** Builds a ParsedApdu with the given data payload. */
    private fun buildApdu(data: ByteArray = byteArrayOf()): ApduParser.ParsedApdu {
        val command = byteArrayOf(0x06, 0x01) // Authorization command
        return ApduParser.ParsedApdu(
            command = command,
            data = data,
            totalLength = command.size + 1 + data.size
        )
    }

    @Before
    fun setUp() {
        state = SimulatorState(defaultConfig)
        store = TransactionStore()
    }

    // =========================================================================
    // RegistrationHandler
    // =========================================================================

    @Test
    fun registrationHandler_returns3Responses() = runTest {
        val handler = RegistrationHandler(state)
        val apdu = ApduParser.ParsedApdu(
            command = byteArrayOf(0x06, 0x00),
            data = byteArrayOf(),
            totalLength = 3
        )
        val responses = handler.handle(apdu)
        assertEquals(3, responses.size)
    }

    @Test
    fun registrationHandler_firstResponseIsAck() = runTest {
        val handler = RegistrationHandler(state)
        val apdu = ApduParser.ParsedApdu(
            command = byteArrayOf(0x06, 0x00),
            data = byteArrayOf(),
            totalLength = 3
        )
        val responses = handler.handle(apdu)
        val ack = responses[0]
        assertEquals(0x80.toByte(), ack[0])
        assertEquals(0x00.toByte(), ack[1])
        assertEquals(0x00.toByte(), ack[2])
    }

    @Test
    fun registrationHandler_lastResponseIsCompletion() = runTest {
        val handler = RegistrationHandler(state)
        val apdu = ApduParser.ParsedApdu(
            command = byteArrayOf(0x06, 0x00),
            data = byteArrayOf(),
            totalLength = 3
        )
        val responses = handler.handle(apdu)
        val completion = responses.last()
        assertEquals(0x06.toByte(), completion[0])
        assertEquals(0x0F.toByte(), completion[1])
    }

    @Test
    fun registrationHandler_setsStateRegistered() = runTest {
        val handler = RegistrationHandler(state)
        assertFalse(state.isRegistered())

        val apdu = ApduParser.ParsedApdu(
            command = byteArrayOf(0x06, 0x00),
            data = byteArrayOf(),
            totalLength = 3
        )
        handler.handle(apdu)
        assertTrue(state.isRegistered())
    }

    // =========================================================================
    // AuthorizationHandler — normal flow
    // =========================================================================

    @Test
    fun authorizationHandler_returns6Responses() = runTest {
        val handler = AuthorizationHandler(state, store)
        val data = byteArrayOf(0x04) + BcdEncoder.amountToBcd(1250)
        val apdu = buildApdu(data)

        val responses = handler.handle(apdu)
        // ACK + 3 intermediate + StatusInfo + Completion = 6
        // (paymentPrintLines is opt-in and off by default)
        assertEquals(6, responses.size)
    }

    @Test
    fun authorizationHandler_withoutPrintLines_sendsNoPrintLine() = runTest {
        val handler = AuthorizationHandler(state, store)
        val data = byteArrayOf(0x04) + BcdEncoder.amountToBcd(1250)

        val responses = handler.handle(buildApdu(data))
        assertFalse(
            "No 06 D1 print line expected while paymentPrintLines is disabled",
            responses.any { it.size >= 2 && it[0] == 0x06.toByte() && it[1] == 0xD1.toByte() }
        )
    }

    @Test
    fun authorizationHandler_withPrintLines_returns14Responses() = runTest {
        val printState = SimulatorState(defaultConfig.copy(paymentPrintLines = true))
        val handler = AuthorizationHandler(printState, store)
        val data = byteArrayOf(0x04) + BcdEncoder.amountToBcd(1250)

        val responses = handler.handle(buildApdu(data))
        // ACK + 3 intermediate + StatusInfo + 8 print lines + Completion = 14
        assertEquals(14, responses.size)
    }

    @Test
    fun authorizationHandler_withPrintLines_printLinesSitBeforeCompletion() = runTest {
        val printState = SimulatorState(defaultConfig.copy(paymentPrintLines = true))
        val handler = AuthorizationHandler(printState, store)
        val data = byteArrayOf(0x04) + BcdEncoder.amountToBcd(1250)

        val responses = handler.handle(buildApdu(data))
        val printLines = responses.filter {
            it.size >= 2 && it[0] == 0x06.toByte() && it[1] == 0xD1.toByte()
        }
        assertEquals(8, printLines.size)

        val lastPrintIndex = responses.indexOfLast {
            it.size >= 2 && it[0] == 0x06.toByte() && it[1] == 0xD1.toByte()
        }
        assertEquals(responses.size - 2, lastPrintIndex)
    }

    @Test
    fun authorizationHandler_withPrintLines_amountUsesConfiguredCurrency() = runTest {
        val printState = SimulatorState(
            defaultConfig.copy(paymentPrintLines = true, currencyCode = 756)
        )
        val handler = AuthorizationHandler(printState, store)
        val data = byteArrayOf(0x04) + BcdEncoder.amountToBcd(1250)

        val responses = handler.handle(buildApdu(data))
        val text = responses.joinToString(" ") { String(it, Charsets.US_ASCII) }
        assertTrue("Print lines should use CHF, not a hardcoded EUR", text.contains("12,50 CHF"))
        assertFalse(text.contains("EUR"))
    }

    @Test
    fun authorizationHandler_firstResponseIsAck() = runTest {
        val handler = AuthorizationHandler(state, store)
        val data = byteArrayOf(0x04) + BcdEncoder.amountToBcd(1250)
        val apdu = buildApdu(data)

        val responses = handler.handle(apdu)
        assertEquals(0x80.toByte(), responses[0][0])
        assertEquals(0x00.toByte(), responses[0][1])
    }

    @Test
    fun authorizationHandler_lastResponseIsCompletion() = runTest {
        val handler = AuthorizationHandler(state, store)
        val data = byteArrayOf(0x04) + BcdEncoder.amountToBcd(1250)
        val apdu = buildApdu(data)

        val responses = handler.handle(apdu)
        val completion = responses.last()
        assertEquals(0x06.toByte(), completion[0])
        assertEquals(0x0F.toByte(), completion[1])
    }

    @Test
    fun authorizationHandler_storesTransactionInStore() = runTest {
        val handler = AuthorizationHandler(state, store)
        val data = byteArrayOf(0x04) + BcdEncoder.amountToBcd(1250)
        val apdu = buildApdu(data)

        assertEquals(0, store.getTransactionCount())
        handler.handle(apdu)
        assertEquals(1, store.getTransactionCount())
    }

    @Test
    fun authorizationHandler_storedTransactionHasCorrectAmount() = runTest {
        val handler = AuthorizationHandler(state, store)
        val data = byteArrayOf(0x04) + BcdEncoder.amountToBcd(1250)
        val apdu = buildApdu(data)

        handler.handle(apdu)
        val txn = store.getLastTransaction()
        assertNotNull(txn)
        assertEquals(1250L, txn!!.amount)
    }

    @Test
    fun authorizationHandler_incrementsCounters() = runTest {
        val handler = AuthorizationHandler(state, store)
        val data = byteArrayOf(0x04) + BcdEncoder.amountToBcd(1250)

        // First call: trace=1, receipt=1, turnover=1
        handler.handle(buildApdu(data))
        val firstTxn = store.getLastTransaction()!!
        assertEquals(1, firstTxn.trace)
        assertEquals(1, firstTxn.receipt)
        assertEquals(1, firstTxn.turnover)

        // Second call: trace=2, receipt=2, turnover=2
        handler.handle(buildApdu(data))
        val secondTxn = store.getLastTransaction()!!
        assertEquals(2, secondTxn.trace)
        assertEquals(2, secondTxn.receipt)
        assertEquals(2, secondTxn.turnover)
    }

    @Test
    fun authorizationHandler_intermediateStatusesStartWith04FF() = runTest {
        val handler = AuthorizationHandler(state, store)
        val data = byteArrayOf(0x04) + BcdEncoder.amountToBcd(1250)
        val apdu = buildApdu(data)

        val responses = handler.handle(apdu)
        // Responses[1], [2], [3] are intermediate statuses
        for (i in 1..3) {
            assertEquals(
                "Intermediate status at index $i should start with 0x04",
                0x04.toByte(), responses[i][0]
            )
            assertEquals(
                "Intermediate status at index $i should have 0xFF as second byte",
                0xFF.toByte(), responses[i][1]
            )
        }
    }

    @Test
    fun authorizationHandler_statusInfoStartsWith040F() = runTest {
        val handler = AuthorizationHandler(state, store)
        val data = byteArrayOf(0x04) + BcdEncoder.amountToBcd(1250)
        val apdu = buildApdu(data)

        val responses = handler.handle(apdu)
        // Response[4] is the StatusInfo
        assertEquals(0x04.toByte(), responses[4][0])
        assertEquals(0x0F.toByte(), responses[4][1])
    }

    // =========================================================================
    // AuthorizationHandler — error simulation
    // =========================================================================

    @Test
    fun authorizationHandler_withErrorSimulation_returns3Responses() = runTest {
        val errorConfig = defaultConfig.copy(
            errorSimulation = ErrorSimulation(enabled = true, forcedErrorCode = 0x64)
        )
        val errorState = SimulatorState(errorConfig)
        val handler = AuthorizationHandler(errorState, store)
        val data = byteArrayOf(0x04) + BcdEncoder.amountToBcd(1250)
        val apdu = buildApdu(data)

        val responses = handler.handle(apdu)
        // ACK + ErrorStatusInfo + Completion = 3
        assertEquals(3, responses.size)
    }

    @Test
    fun authorizationHandler_withErrorSimulation_noTransactionStored() = runTest {
        val errorConfig = defaultConfig.copy(
            errorSimulation = ErrorSimulation(enabled = true, forcedErrorCode = 0x64)
        )
        val errorState = SimulatorState(errorConfig)
        val handler = AuthorizationHandler(errorState, store)
        val data = byteArrayOf(0x04) + BcdEncoder.amountToBcd(1250)
        val apdu = buildApdu(data)

        handler.handle(apdu)
        assertEquals(0, store.getTransactionCount())
        assertNull(store.getLastTransaction())
    }
}
