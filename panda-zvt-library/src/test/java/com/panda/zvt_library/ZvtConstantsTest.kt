package com.panda.zvt_library

import com.panda.zvt_library.protocol.ZvtConstants
import org.junit.Test
import org.junit.Assert.*

class ZvtConstantsTest {

    @Test
    fun cmdRegistration_isCorrectBytes() {
        val expected = byteArrayOf(0x06, 0x00)
        assertArrayEquals(expected, ZvtConstants.CMD_REGISTRATION)
    }

    @Test
    fun cmdAuthorization_isCorrectBytes() {
        val expected = byteArrayOf(0x06, 0x01)
        assertArrayEquals(expected, ZvtConstants.CMD_AUTHORIZATION)
    }

    @Test
    fun ack_isCorrectBytes() {
        val expected = byteArrayOf(0x80.toByte(), 0x00, 0x00)
        assertArrayEquals(expected, ZvtConstants.ACK)
    }

    @Test
    fun bmpConstants_haveCorrectValues() {
        assertEquals(0x04.toByte(), ZvtConstants.BMP_AMOUNT)
        assertEquals(0x0B.toByte(), ZvtConstants.BMP_TRACE_NUMBER)
    }

    @Test
    fun networkConstants_haveCorrectValues() {
        assertEquals(20007, ZvtConstants.DEFAULT_PORT)
        assertEquals(978, ZvtConstants.CURRENCY_EUR)
    }

    @Test
    fun getResultMessage_success_containsSuccessful() {
        val message = ZvtConstants.getResultMessage(ZvtConstants.RC_SUCCESS)
        assertTrue("Expected message to contain 'Successful', got: $message", message.contains("Successful"))
    }

    @Test
    fun getResultMessage_timeout_containsTimeout() {
        val message = ZvtConstants.getResultMessage(0x6C)
        assertTrue("Expected message to contain 'timeout', got: $message", message.contains("timeout", ignoreCase = true))
    }

    @Test
    fun getResultMessage_networkRange_containsNetwork() {
        val message = ZvtConstants.getResultMessage(0x30)
        assertTrue("Expected message to contain 'Network', got: $message", message.contains("Network", ignoreCase = true))
    }

    @Test
    fun getIntermediateStatusMessage_insertCard() {
        val message = ZvtConstants.getIntermediateStatusMessage(0x0A)
        assertTrue("Expected message to contain 'Insert card', got: $message", message.contains("Insert card", ignoreCase = true))
    }

    @Test
    fun getIntermediateStatusMessage_unknownCode_startsWithStatus() {
        val message = ZvtConstants.getIntermediateStatusMessage(0x7F)
        assertTrue("Expected message to start with 'Status:', got: $message", message.startsWith("Status:"))
    }

    @Test
    fun getCommandName_registration_containsRegistration() {
        val name = ZvtConstants.getCommandName(ZvtConstants.CMD_REGISTRATION)
        assertTrue("Expected name to contain 'Registration', got: $name", name.contains("Registration"))
    }

    @Test
    fun getCommandName_unknownCommand_containsUnknown() {
        val name = ZvtConstants.getCommandName(byteArrayOf(0x99.toByte(), 0x99.toByte()))
        assertTrue("Expected name to contain 'Unknown', got: $name", name.contains("Unknown"))
    }

    @Test
    fun getCommandName_tooShortArray_returnsUnknown() {
        val name = ZvtConstants.getCommandName(byteArrayOf(0x06))
        assertEquals("Unknown", name)
    }
}
