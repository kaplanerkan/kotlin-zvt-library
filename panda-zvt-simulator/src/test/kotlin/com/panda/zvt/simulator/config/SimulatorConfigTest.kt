package com.panda.zvt.simulator.config

import org.junit.Test
import org.junit.Assert.*

class SimulatorConfigTest {

    @Test
    fun `default config values are correct`() {
        val config = SimulatorConfig()
        assertEquals(20007, config.zvtPort)
        assertEquals(8080, config.apiPort)
        assertEquals("29001234", config.terminalId)
        assertEquals("SIMULATOR123456", config.vuNumber)
        assertEquals(978, config.currencyCode)
    }

    @Test
    fun `default card data values are correct`() {
        val card = SimulatedCardData()
        assertEquals("6763890000001230", card.pan)
        assertEquals(6, card.cardType)
        assertEquals("Mastercard", card.cardName)
        assertEquals("2812", card.expiryDate)
        assertEquals(1, card.sequenceNumber)
        assertEquals("A000000004101001", card.aid)
    }

    @Test
    fun `ErrorSimulation disabled returns shouldError false`() {
        val error = ErrorSimulation(enabled = false, errorPercentage = 50)
        assertFalse(error.shouldError())
    }

    @Test
    fun `ErrorSimulation enabled with forcedErrorCode returns shouldError true and correct code`() {
        val error = ErrorSimulation(enabled = true, forcedErrorCode = 0x64)
        assertTrue(error.shouldError())
        assertEquals(0x64.toByte(), error.getErrorCode())
    }

    @Test
    fun `ErrorSimulation disabled returns default error code 0x6C`() {
        val error = ErrorSimulation(enabled = false)
        assertEquals(0x6C.toByte(), error.getErrorCode())
    }
}
