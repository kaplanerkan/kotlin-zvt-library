package com.panda.zvt.simulator.state

import com.panda.zvt.simulator.config.SimulatorConfig
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

class SimulatorStateTest {

    private lateinit var state: SimulatorState

    @Before
    fun setUp() {
        state = SimulatorState(SimulatorConfig())
    }

    @Test
    fun `initial state - isRegistered is false`() {
        assertFalse(state.isRegistered())
    }

    @Test
    fun `initial state - isBusy is false`() {
        assertFalse(state.isBusy())
    }

    @Test
    fun `initial counters - currentTrace is 1`() {
        assertEquals(1, state.currentTrace())
    }

    @Test
    fun `initial counters - currentReceipt is 1`() {
        assertEquals(1, state.currentReceipt())
    }

    @Test
    fun `nextTraceNumber increments - first call returns 1 second returns 2`() {
        val first = state.nextTraceNumber()
        val second = state.nextTraceNumber()
        assertEquals(1, first)
        assertEquals(2, second)
    }

    @Test
    fun `nextReceiptNumber increments - first call returns 1 second returns 2`() {
        val first = state.nextReceiptNumber()
        val second = state.nextReceiptNumber()
        assertEquals(1, first)
        assertEquals(2, second)
    }

    @Test
    fun `nextTurnoverNumber increments - first call returns 1 second returns 2`() {
        val first = state.nextTurnoverNumber()
        val second = state.nextTurnoverNumber()
        assertEquals(1, first)
        assertEquals(2, second)
    }

    @Test
    fun `currentTrace reads without incrementing`() {
        val firstRead = state.currentTrace()
        val secondRead = state.currentTrace()
        assertEquals(firstRead, secondRead)
    }

    @Test
    fun `setRegistered true makes isRegistered return true`() {
        state.setRegistered(true)
        assertTrue(state.isRegistered())
    }

    @Test
    fun `setBusy true makes isBusy return true`() {
        state.setBusy(true)
        assertTrue(state.isBusy())
    }

    @Test
    fun `reset restores all counters and flags to initial values`() {
        // Advance counters and set flags
        state.nextTraceNumber()
        state.nextTraceNumber()
        state.nextReceiptNumber()
        state.nextReceiptNumber()
        state.nextTurnoverNumber()
        state.setRegistered(true)
        state.setBusy(true)

        state.reset()

        assertEquals(1, state.currentTrace())
        assertEquals(1, state.currentReceipt())
        assertFalse(state.isRegistered())
        assertFalse(state.isBusy())
    }

    @Test
    fun `updateConfig changes the config`() {
        val newConfig = SimulatorConfig(terminalId = "99998888")
        state.updateConfig(newConfig)
        assertEquals("99998888", state.config.terminalId)
    }

    @Test
    fun `config property get and set works`() {
        val newConfig = SimulatorConfig(vuNumber = "NEWVU999999")
        state.config = newConfig
        assertEquals("NEWVU999999", state.config.vuNumber)
    }

    @Test
    fun `multiple nextTraceNumber calls produce sequential values`() {
        val values = (1..5).map { state.nextTraceNumber() }
        assertEquals(listOf(1, 2, 3, 4, 5), values)
    }
}
