package com.panda.zvt.simulator.protocol

import org.junit.Test
import org.junit.Assert.*

class ApduBuilderTest {

    // --- ack ---

    @Test
    fun ack_returnsCorrectBytes() {
        val result = ApduBuilder.ack()
        assertArrayEquals(byteArrayOf(0x80.toByte(), 0x00, 0x00), result)
    }

    // --- nack ---

    @Test
    fun nack_returnsCorrectBytes() {
        val result = ApduBuilder.nack()
        assertArrayEquals(byteArrayOf(0x84.toByte(), 0x00, 0x00), result)
    }

    // --- buildPacket ---

    @Test
    fun buildPacket_noData_returnsCommandWithZeroLength() {
        val command = byteArrayOf(0x06, 0x0F)
        val result = ApduBuilder.buildPacket(command)
        assertEquals(3, result.size)
        assertEquals(0x06.toByte(), result[0])
        assertEquals(0x0F.toByte(), result[1])
        assertEquals(0x00.toByte(), result[2])
    }

    @Test
    fun buildPacket_with3BytesData_returnsCorrectPacket() {
        val command = byteArrayOf(0x06, 0x0F)
        val data = byteArrayOf(0x27, 0x00, 0x0B)
        val result = ApduBuilder.buildPacket(command, data)
        assertEquals(6, result.size)
        assertEquals(0x06.toByte(), result[0])
        assertEquals(0x0F.toByte(), result[1])
        assertEquals(0x03.toByte(), result[2])  // length
        assertEquals(0x27.toByte(), result[3])
        assertEquals(0x00.toByte(), result[4])
        assertEquals(0x0B.toByte(), result[5])
    }

    @Test
    fun buildPacket_extendedLength_uses0xFFPrefix() {
        val command = byteArrayOf(0x06, 0x0F)
        val data = ByteArray(300) { 0xAA.toByte() }
        val result = ApduBuilder.buildPacket(command, data)
        // Total: 2 (cmd) + 3 (0xFF + 2 LE bytes) + 300 (data) = 305
        assertEquals(305, result.size)
        assertEquals(0x06.toByte(), result[0])
        assertEquals(0x0F.toByte(), result[1])
        assertEquals(0xFF.toByte(), result[2])
        // 300 = 0x012C in little-endian: low=0x2C, high=0x01
        assertEquals(0x2C.toByte(), result[3])
        assertEquals(0x01.toByte(), result[4])
    }

    // --- ack returns independent copy ---

    @Test
    fun ack_returnsIndependentCopy() {
        val ack1 = ApduBuilder.ack()
        ack1[0] = 0x00  // modify the first copy
        val ack2 = ApduBuilder.ack()
        // Second call should still return the original ACK bytes
        assertEquals(0x80.toByte(), ack2[0])
    }

    // --- nack returns independent copy ---

    @Test
    fun nack_returnsIndependentCopy() {
        val nack1 = ApduBuilder.nack()
        nack1[0] = 0x00  // modify the first copy
        val nack2 = ApduBuilder.nack()
        // Second call should still return the original NACK bytes
        assertEquals(0x84.toByte(), nack2[0])
    }

    // --- buildPacket boundary: exactly 254 bytes uses single-byte length ---

    @Test
    fun buildPacket_254bytesData_usesSingleByteLength() {
        val command = byteArrayOf(0x04, 0x0F)
        val data = ByteArray(254) { 0x01 }
        val result = ApduBuilder.buildPacket(command, data)
        // Total: 2 (cmd) + 1 (length byte) + 254 (data) = 257
        assertEquals(257, result.size)
        assertEquals(0xFE.toByte(), result[2])  // 254 = 0xFE, still single byte
    }
}
