package com.panda.zvt_library

import com.panda.zvt_library.protocol.ZvtPacket
import com.panda.zvt_library.protocol.ZvtConstants
import org.junit.Test
import org.junit.Assert.*

class ZvtPacketTest {

    // --- 1. ack() toBytes ---
    @Test
    fun ack_toBytes_returns80_00_00() {
        val expected = byteArrayOf(0x80.toByte(), 0x00, 0x00)
        val actual = ZvtPacket.ack().toBytes()
        assertArrayEquals(expected, actual)
        assertEquals(3, actual.size)
    }

    // --- 2. nack() toBytes ---
    @Test
    fun nack_toBytes_returns84_00_00() {
        val expected = byteArrayOf(0x84.toByte(), 0x00, 0x00)
        val actual = ZvtPacket.nack().toBytes()
        assertArrayEquals(expected, actual)
        assertEquals(3, actual.size)
    }

    // --- 3. ack().isAck == true, ack().isNack == false ---
    @Test
    fun ack_isAckTrue_isNackFalse() {
        val packet = ZvtPacket.ack()
        assertTrue("ACK packet should have isAck=true", packet.isAck)
        assertFalse("ACK packet should have isNack=false", packet.isNack)
    }

    // --- 4. nack().isNack == true, nack().isAck == false ---
    @Test
    fun nack_isNackTrue_isAckFalse() {
        val packet = ZvtPacket.nack()
        assertTrue("NACK packet should have isNack=true", packet.isNack)
        assertFalse("NACK packet should have isAck=false", packet.isAck)
    }

    // --- 5. Completion packet ---
    @Test
    fun completion_isCompletionTrue() {
        val packet = ZvtPacket(byteArrayOf(0x06, 0x0F))
        assertTrue("Completion packet should have isCompletion=true", packet.isCompletion)
        assertFalse("Completion packet should not be StatusInfo", packet.isStatusInfo)
        assertFalse("Completion packet should not be Abort", packet.isAbort)
    }

    // --- 6. StatusInfo packet ---
    @Test
    fun statusInfo_isStatusInfoTrue() {
        val packet = ZvtPacket(byteArrayOf(0x04, 0x0F))
        assertTrue("StatusInfo packet should have isStatusInfo=true", packet.isStatusInfo)
        assertFalse("StatusInfo packet should not be Completion", packet.isCompletion)
        assertFalse("StatusInfo packet should not be IntermediateStatus", packet.isIntermediateStatus)
    }

    // --- 7. IntermediateStatus packet ---
    @Test
    fun intermediateStatus_isIntermediateStatusTrue() {
        val packet = ZvtPacket(byteArrayOf(0x04, 0xFF.toByte()))
        assertTrue("IntermediateStatus packet should have isIntermediateStatus=true", packet.isIntermediateStatus)
        assertFalse("IntermediateStatus packet should not be StatusInfo", packet.isStatusInfo)
    }

    // --- 8. Abort packet ---
    @Test
    fun abort_isAbortTrue() {
        val packet = ZvtPacket(byteArrayOf(0x06, 0x1E))
        assertTrue("Abort packet should have isAbort=true", packet.isAbort)
        assertFalse("Abort packet should not be Completion", packet.isCompletion)
    }

    // --- 9. PrintLine packet ---
    @Test
    fun printLine_isPrintLineTrue() {
        val packet = ZvtPacket(byteArrayOf(0x06, 0xD1.toByte()))
        assertTrue("PrintLine packet should have isPrintLine=true", packet.isPrintLine)
        assertFalse("PrintLine packet should not be Completion", packet.isCompletion)
    }

    // --- 10. toBytes with short data (< 0xFF) ---
    @Test
    fun toBytes_shortData_encodesLengthAsSingleByte() {
        val data = byteArrayOf(0x04, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00)
        val packet = ZvtPacket(byteArrayOf(0x06, 0x01), data)
        val bytes = packet.toBytes()

        // Expected: [06, 01, 07, 04, 00, 00, 00, 01, 00, 00]
        assertEquals(0x06.toByte(), bytes[0])
        assertEquals(0x01.toByte(), bytes[1])
        assertEquals(0x07.toByte(), bytes[2]) // length = 7
        assertEquals(2 + 1 + 7, bytes.size)  // cmd(2) + len(1) + data(7)
        // Verify data portion
        assertArrayEquals(data, bytes.copyOfRange(3, bytes.size))
    }

    // --- 11. toBytes/fromBytes round-trip ---
    @Test
    fun toBytes_fromBytes_roundTrip() {
        val original = ZvtPacket(
            byteArrayOf(0x06, 0x01),
            byteArrayOf(0x04, 0x00, 0x00, 0x00, 0x99.toByte(), 0xAB.toByte())
        )
        val bytes = original.toBytes()
        val result = ZvtPacket.fromBytes(bytes)

        assertNotNull("fromBytes should not return null for valid data", result)
        val (parsed, consumed) = result!!
        assertArrayEquals("Command should match after round-trip", original.command, parsed.command)
        assertArrayEquals("Data should match after round-trip", original.data, parsed.data)
        assertEquals("All bytes should be consumed", bytes.size, consumed)
    }

    // --- 12. fromBytes with insufficient data returns null ---
    @Test
    fun fromBytes_insufficientData_returnsNull() {
        // Less than 3 bytes (minimum CMD(2) + LEN(1))
        assertNull("Empty buffer should return null", ZvtPacket.fromBytes(byteArrayOf()))
        assertNull("1-byte buffer should return null", ZvtPacket.fromBytes(byteArrayOf(0x06)))
        assertNull("2-byte buffer should return null", ZvtPacket.fromBytes(byteArrayOf(0x06, 0x01)))
    }

    // --- 13. fromBytes extended length (>= 0xFF) ---
    @Test
    fun fromBytes_extendedLength_parsesCorrectly() {
        // Build a packet with 300 bytes of data (0x012C in LE = 0x2C, 0x01)
        val dataSize = 300
        val data = ByteArray(dataSize) { (it % 256).toByte() }
        val buffer = byteArrayOf(
            0x06, 0x01,                              // Command
            0xFF.toByte(),                           // Extended length marker
            (dataSize and 0xFF).toByte(),            // Low byte: 0x2C (44)
            ((dataSize shr 8) and 0xFF).toByte()     // High byte: 0x01
        ) + data

        val result = ZvtPacket.fromBytes(buffer)
        assertNotNull("fromBytes should parse extended length", result)
        val (parsed, consumed) = result!!
        assertArrayEquals(byteArrayOf(0x06, 0x01), parsed.command)
        assertEquals(dataSize, parsed.dataLength)
        assertArrayEquals(data, parsed.data)
        assertEquals(buffer.size, consumed)
    }

    // --- 14. dataLength property ---
    @Test
    fun dataLength_returnsCorrectSize() {
        val packet = ZvtPacket(byteArrayOf(0x06, 0x00), ByteArray(10))
        assertEquals(10, packet.dataLength)

        val emptyPacket = ZvtPacket(byteArrayOf(0x80.toByte(), 0x00))
        assertEquals(0, emptyPacket.dataLength)
    }

    // --- 15. commandHex property ---
    @Test
    fun commandHex_returnsFormattedHexString() {
        val packet = ZvtPacket(byteArrayOf(0x06, 0x01))
        assertEquals("06 01", packet.commandHex)

        val ackPacket = ZvtPacket.ack()
        assertEquals("80 00", ackPacket.commandHex)
    }

    // --- 16. equals: two packets with same command/data are equal ---
    @Test
    fun equals_sameCommandAndData_areEqual() {
        val packet1 = ZvtPacket(byteArrayOf(0x06, 0x01), byteArrayOf(0x01, 0x02, 0x03))
        val packet2 = ZvtPacket(byteArrayOf(0x06, 0x01), byteArrayOf(0x01, 0x02, 0x03))
        assertEquals("Packets with same command and data should be equal", packet1, packet2)
        assertEquals("Hash codes should match for equal packets", packet1.hashCode(), packet2.hashCode())
    }

    @Test
    fun equals_differentData_areNotEqual() {
        val packet1 = ZvtPacket(byteArrayOf(0x06, 0x01), byteArrayOf(0x01))
        val packet2 = ZvtPacket(byteArrayOf(0x06, 0x01), byteArrayOf(0x02))
        assertNotEquals("Packets with different data should not be equal", packet1, packet2)
    }

    @Test
    fun equals_differentCommand_areNotEqual() {
        val packet1 = ZvtPacket(byteArrayOf(0x06, 0x01))
        val packet2 = ZvtPacket(byteArrayOf(0x06, 0x0F))
        assertNotEquals("Packets with different commands should not be equal", packet1, packet2)
    }

    // --- Extra: toBytes with extended length round-trip ---
    @Test
    fun toBytes_extendedLength_roundTrip() {
        val largeData = ByteArray(300) { (it % 256).toByte() }
        val original = ZvtPacket(byteArrayOf(0x04, 0x0F), largeData)
        val bytes = original.toBytes()

        // Verify extended length encoding: CMD(2) + 0xFF(1) + LL(1) + HH(1) + DATA(300)
        assertEquals(2 + 3 + 300, bytes.size)
        assertEquals(0xFF.toByte(), bytes[2]) // Extended length marker

        val result = ZvtPacket.fromBytes(bytes)
        assertNotNull("fromBytes should parse extended length packet", result)
        val (parsed, consumed) = result!!
        assertArrayEquals(original.command, parsed.command)
        assertArrayEquals(original.data, parsed.data)
        assertEquals(bytes.size, consumed)
    }

    // --- Extra: fromBytes with offset ---
    @Test
    fun fromBytes_withOffset_parsesFromGivenPosition() {
        val prefix = byteArrayOf(0xAA.toByte(), 0xBB.toByte()) // 2 junk bytes
        val packetBytes = ZvtPacket.ack().toBytes()              // 80 00 00
        val buffer = prefix + packetBytes

        val result = ZvtPacket.fromBytes(buffer, offset = 2)
        assertNotNull("fromBytes with offset should parse correctly", result)
        val (parsed, consumed) = result!!
        assertTrue("Parsed packet should be ACK", parsed.isAck)
        assertEquals(3, consumed)
    }

    // --- Extra: fromBytes returns null when data length exceeds buffer ---
    @Test
    fun fromBytes_dataLengthExceedsBuffer_returnsNull() {
        // CMD(2) + LEN=10, but only 3 bytes of data provided
        val buffer = byteArrayOf(0x06, 0x01, 0x0A, 0x01, 0x02, 0x03)
        assertNull("Should return null when buffer has less data than length field indicates",
            ZvtPacket.fromBytes(buffer))
    }

    // --- Extra: fromBytes extended length with insufficient header ---
    @Test
    fun fromBytes_extendedLengthInsufficientHeader_returnsNull() {
        // CMD(2) + 0xFF marker but only 1 byte of length (need 2)
        val buffer = byteArrayOf(0x06, 0x01, 0xFF.toByte(), 0x0A)
        assertNull("Should return null when extended length header is incomplete",
            ZvtPacket.fromBytes(buffer))
    }
}
