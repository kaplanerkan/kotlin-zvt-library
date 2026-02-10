package com.panda.zvt_library

import com.panda.zvt_library.util.TlvParser
import com.panda.zvt_library.util.TlvParser.TlvEntry
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for [TlvParser] — TLV encoding/decoding used in the ZVT protocol.
 *
 * Covers parsing (single-byte tag, multi-byte tag, padding, long-form length),
 * searching (findTag, findTags), building (buildTlv, buildTlvList, buildTlvAscii),
 * round-trip integrity, and TlvEntry equality/toString behaviour.
 */
class TlvParserTest {

    // =========================================================================
    // 1. parse: single 1-byte tag
    // =========================================================================
    @Test
    fun parseSingleOneByteTag() {
        // Tag 0x50, length 3, value "ABC"
        val data = byteArrayOf(0x50, 0x03, 0x41, 0x42, 0x43)
        val entries = TlvParser.parse(data)

        assertEquals(1, entries.size)
        assertEquals(0x50, entries[0].tag)
        assertEquals(3, entries[0].length)
        assertArrayEquals(byteArrayOf(0x41, 0x42, 0x43), entries[0].value)
        assertEquals("ABC", String(entries[0].value, Charsets.US_ASCII))
    }

    // =========================================================================
    // 2. parse: multiple tags concatenated
    // =========================================================================
    @Test
    fun parseMultipleTags() {
        // Tag 0x50, len 2, value "AB" | Tag 0x26, len 1, value 0x07
        val data = byteArrayOf(
            0x50, 0x02, 0x41, 0x42,
            0x26, 0x01, 0x07
        )
        val entries = TlvParser.parse(data)

        assertEquals(2, entries.size)

        assertEquals(0x50, entries[0].tag)
        assertArrayEquals(byteArrayOf(0x41, 0x42), entries[0].value)

        assertEquals(0x26, entries[1].tag)
        assertArrayEquals(byteArrayOf(0x07), entries[1].value)
    }

    // =========================================================================
    // 3. parse: 2-byte tag (lower 5 bits of first byte = 0x1F)
    // =========================================================================
    @Test
    fun parseTwoByteTag() {
        // Tag 0x1F10 (two bytes: 0x1F, 0x10), length 2, value 0xAA 0xBB
        val data = byteArrayOf(0x1F, 0x10, 0x02, 0xAA.toByte(), 0xBB.toByte())
        val entries = TlvParser.parse(data)

        assertEquals(1, entries.size)
        assertEquals(0x1F10, entries[0].tag)
        assertEquals(2, entries[0].length)
        assertArrayEquals(byteArrayOf(0xAA.toByte(), 0xBB.toByte()), entries[0].value)
    }

    // =========================================================================
    // 4. parse: skips 0x00 and 0xFF padding bytes
    // =========================================================================
    @Test
    fun parseSkipsPadding() {
        // Padding (0x00, 0xFF) before and between two TLV records
        val data = byteArrayOf(
            0x00, 0xFF.toByte(), // leading padding
            0x50, 0x01, 0x41,   // Tag 0x50, len 1, value "A"
            0x00,                // mid-padding
            0xFF.toByte(),       // mid-padding
            0x26, 0x01, 0x05    // Tag 0x26, len 1, value 0x05
        )
        val entries = TlvParser.parse(data)

        assertEquals(2, entries.size)
        assertEquals(0x50, entries[0].tag)
        assertEquals(0x26, entries[1].tag)
    }

    // =========================================================================
    // 5. parse: empty data returns empty list
    // =========================================================================
    @Test
    fun parseEmptyDataReturnsEmptyList() {
        val entries = TlvParser.parse(byteArrayOf())
        assertTrue(entries.isEmpty())
    }

    // =========================================================================
    // 6. parse: long-form length (0x81 prefix, 128-byte value)
    // =========================================================================
    @Test
    fun parseLongFormLength() {
        // Tag 0x50, length 0x81 0x80 (= 128), value = 128 bytes of 0x42
        val valueBytes = ByteArray(128) { 0x42 }
        val data = byteArrayOf(0x50, 0x81.toByte(), 0x80.toByte()) + valueBytes
        val entries = TlvParser.parse(data)

        assertEquals(1, entries.size)
        assertEquals(0x50, entries[0].tag)
        assertEquals(128, entries[0].length)
        assertArrayEquals(valueBytes, entries[0].value)
    }

    // =========================================================================
    // 7. findTag: tag present
    // =========================================================================
    @Test
    fun findTagReturnsEntryWhenPresent() {
        val data = byteArrayOf(
            0x50, 0x03, 0x41, 0x42, 0x43, // Tag 0x50, "ABC"
            0x26, 0x01, 0x07               // Tag 0x26, 0x07
        )
        val entry = TlvParser.findTag(data, 0x50)

        assertNotNull(entry)
        assertEquals(0x50, entry!!.tag)
        assertArrayEquals(byteArrayOf(0x41, 0x42, 0x43), entry.value)
    }

    // =========================================================================
    // 8. findTag: tag not present returns null
    // =========================================================================
    @Test
    fun findTagReturnsNullWhenNotPresent() {
        val data = byteArrayOf(0x50, 0x01, 0x41) // Only tag 0x50
        val entry = TlvParser.findTag(data, 0x99)

        assertNull(entry)
    }

    // =========================================================================
    // 9. findTags: multiple search tags, some present some not
    // =========================================================================
    @Test
    fun findTagsReturnsOnlyPresentTags() {
        val data = byteArrayOf(
            0x50, 0x02, 0x41, 0x42, // Tag 0x50, "AB"
            0x26, 0x01, 0x07        // Tag 0x26, 0x07
        )
        val result = TlvParser.findTags(data, 0x50, 0x26, 0x99)

        assertEquals(2, result.size)
        assertTrue(result.containsKey(0x50))
        assertTrue(result.containsKey(0x26))
        assertFalse(result.containsKey(0x99))

        assertArrayEquals(byteArrayOf(0x41, 0x42), result[0x50]!!.value)
        assertArrayEquals(byteArrayOf(0x07), result[0x26]!!.value)
    }

    // =========================================================================
    // 10. buildTlv: single tag structure
    // =========================================================================
    @Test
    fun buildTlvSingleTag() {
        val result = TlvParser.buildTlv(0x26, byteArrayOf(0x01, 0x02))

        // Expected: [0x26, 0x02, 0x01, 0x02]
        assertEquals(4, result.size)
        assertEquals(0x26.toByte(), result[0]) // tag
        assertEquals(0x02.toByte(), result[1]) // length
        assertEquals(0x01.toByte(), result[2]) // value byte 1
        assertEquals(0x02.toByte(), result[3]) // value byte 2
    }

    // =========================================================================
    // 11. buildTlv + parse round-trip
    // =========================================================================
    @Test
    fun buildTlvAndParseRoundTrip() {
        val originalTag = 0x50
        val originalValue = byteArrayOf(0x10, 0x20, 0x30, 0x40)

        val built = TlvParser.buildTlv(originalTag, originalValue)
        val parsed = TlvParser.parse(built)

        assertEquals(1, parsed.size)
        assertEquals(originalTag, parsed[0].tag)
        assertArrayEquals(originalValue, parsed[0].value)
    }

    // =========================================================================
    // 12. buildTlvList: multiple entries round-trip
    // =========================================================================
    @Test
    fun buildTlvListRoundTrip() {
        val entries = listOf(
            TlvEntry(0x50, byteArrayOf(0x41, 0x42, 0x43)),      // "ABC"
            TlvEntry(0x26, byteArrayOf(0x01)),                    // 0x01
            TlvEntry(0x1F10, byteArrayOf(0xDE.toByte(), 0xAD.toByte())) // 2-byte tag
        )

        val built = TlvParser.buildTlvList(entries)
        val parsed = TlvParser.parse(built)

        assertEquals(3, parsed.size)

        assertEquals(entries[0].tag, parsed[0].tag)
        assertArrayEquals(entries[0].value, parsed[0].value)

        assertEquals(entries[1].tag, parsed[1].tag)
        assertArrayEquals(entries[1].value, parsed[1].value)

        assertEquals(entries[2].tag, parsed[2].tag)
        assertArrayEquals(entries[2].value, parsed[2].value)
    }

    // =========================================================================
    // 13. buildTlvAscii: ASCII text round-trip
    // =========================================================================
    @Test
    fun buildTlvAsciiRoundTrip() {
        val built = TlvParser.buildTlvAscii(0x50, "VISA")
        val parsed = TlvParser.parse(built)

        assertEquals(1, parsed.size)
        assertEquals(0x50, parsed[0].tag)
        assertEquals(4, parsed[0].length)
        assertEquals("VISA", String(parsed[0].value, Charsets.US_ASCII))
        assertEquals("VISA", parsed[0].valueAscii)
    }

    // =========================================================================
    // 14. TlvEntry equals and hashCode
    // =========================================================================
    @Test
    fun tlvEntryEqualsAndHashCode() {
        val entry1 = TlvEntry(0x50, byteArrayOf(0x01, 0x02, 0x03))
        val entry2 = TlvEntry(0x50, byteArrayOf(0x01, 0x02, 0x03))
        val entry3 = TlvEntry(0x50, byteArrayOf(0x01, 0x02, 0x04)) // different value
        val entry4 = TlvEntry(0x26, byteArrayOf(0x01, 0x02, 0x03)) // different tag

        // Same tag and same value content -> equal
        assertEquals(entry1, entry2)
        assertEquals(entry1.hashCode(), entry2.hashCode())

        // Different value -> not equal
        assertNotEquals(entry1, entry3)

        // Different tag -> not equal
        assertNotEquals(entry1, entry4)
    }

    // =========================================================================
    // 15. TlvEntry toString contains tag hex
    // =========================================================================
    @Test
    fun tlvEntryToStringContainsTagHex() {
        val entry = TlvEntry(0x1F10, byteArrayOf(0xAA.toByte(), 0xBB.toByte()))
        val str = entry.toString()

        // toString format is "TLV(tag=XXYY, len=N, value=HH HH)"
        assertTrue("toString should contain tag hex '1F10'", str.contains("1F10"))
        assertTrue("toString should start with 'TLV('", str.startsWith("TLV("))
        assertTrue("toString should contain 'len=2'", str.contains("len=2"))
    }
}
