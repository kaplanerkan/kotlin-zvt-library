package com.panda.zvt_library

import com.panda.zvt_library.util.*
import org.junit.Test
import org.junit.Assert.*

class ByteExtensionsTest {

    // --- toHexString ---

    @Test
    fun toHexString_defaultSeparator() {
        val result = byteArrayOf(0x06, 0x01).toHexString()
        assertEquals("06 01", result)
    }

    @Test
    fun toHexString_customSeparator() {
        val result = byteArrayOf(0x06, 0x01).toHexString("")
        assertEquals("0601", result)
    }

    @Test
    fun toHexString_emptyArray() {
        val result = byteArrayOf().toHexString()
        assertEquals("", result)
    }

    // --- hexToByteArray ---

    @Test
    fun hexToByteArray_withSpaces() {
        val result = "06 01".hexToByteArray()
        assertArrayEquals(byteArrayOf(0x06, 0x01), result)
    }

    @Test
    fun hexToByteArray_withColons() {
        val result = "06:01".hexToByteArray()
        assertArrayEquals(byteArrayOf(0x06, 0x01), result)
    }

    @Test
    fun hexToByteArray_withDashes() {
        val result = "06-01".hexToByteArray()
        assertArrayEquals(byteArrayOf(0x06, 0x01), result)
    }

    @Test
    fun hexToByteArray_noSeparator() {
        val result = "0601".hexToByteArray()
        assertArrayEquals(byteArrayOf(0x06, 0x01), result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun hexToByteArray_oddLength_throwsException() {
        "061".hexToByteArray()
    }

    // --- toUnsignedInt ---

    @Test
    fun toUnsignedInt_variousValues() {
        assertEquals(0, 0x00.toByte().toUnsignedInt())
        assertEquals(127, 0x7F.toByte().toUnsignedInt())
        assertEquals(255, 0xFF.toByte().toUnsignedInt())
    }

    // --- bytesToUnsignedShort + toTwoBytes round-trip ---

    @Test
    fun bytesToUnsignedShort_toTwoBytes_roundTrip() {
        val original = 0x1234
        val bytes = original.toTwoBytes()
        val restored = bytesToUnsignedShort(bytes[0], bytes[1])
        assertEquals(original, restored)
    }

    // --- readBytes ---

    @Test
    fun readBytes_validRange() {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        val result = data.readBytes(1, 3)
        assertArrayEquals(byteArrayOf(2, 3, 4), result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun readBytes_outOfBounds_throwsException() {
        val data = byteArrayOf(1, 2, 3)
        data.readBytes(2, 5)
    }

    // --- commandEquals ---

    @Test
    fun commandEquals_matching() {
        val a = byteArrayOf(0x06, 0x01, 0x00)
        val b = byteArrayOf(0x06, 0x01)
        assertTrue(a.commandEquals(b))
    }

    @Test
    fun commandEquals_notMatching() {
        val a = byteArrayOf(0x06, 0x01)
        val b = byteArrayOf(0x06, 0x0D)
        assertFalse(a.commandEquals(b))
    }

    @Test
    fun commandEquals_shortArray() {
        val a = byteArrayOf(0x06)
        val b = byteArrayOf(0x06, 0x01)
        assertFalse(a.commandEquals(b))
    }

    // --- toSafeAscii ---

    @Test
    fun toSafeAscii_replacesNonPrintable() {
        // 0x48='H', 0x69='i', 0x01=non-printable -> replaced with '.'
        val result = byteArrayOf(0x48, 0x69, 0x01).toSafeAscii()
        assertEquals("Hi.", result)
    }

    // --- toLogString ---

    @Test
    fun toLogString_shortArray_fullHex() {
        val data = byteArrayOf(0x06, 0x01, 0x00)
        val result = data.toLogString()
        assertEquals("06 01 00", result)
    }

    @Test
    fun toLogString_longArray_truncated() {
        // Create an array of 100 bytes (exceeds default maxBytes=64)
        val data = ByteArray(100) { it.toByte() }
        val result = data.toLogString()

        // Should end with "... (100 bytes)"
        assertTrue(result.endsWith("... (100 bytes)"))

        // The hex portion should contain exactly 64 bytes worth of hex
        val hexPart = result.substringBefore("...")
        val byteCount = hexPart.trim().split(" ").size
        assertEquals(64, byteCount)
    }
}
