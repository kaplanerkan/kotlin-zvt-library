package com.panda.zvt_library.util

import timber.log.Timber

/**
 * TLV (Tag-Length-Value) parser and builder for the ZVT protocol.
 *
 * Some ZVT data fields (e.g. BMP 0x06, BMP 0x3C) use TLV encoding based on
 * ASN.1 BER-TLV rules. This parser reads and writes TLV structures.
 *
 * TLV Format:
 * - **Tag**: 1-3 bytes (if the lower 5 bits of the first byte are `0x1F`, it is a multi-byte tag)
 * - **Length**: 1-3 bytes (`0x00-0x7F` = direct, `0x81` = 1 extra byte, `0x82` = 2 extra bytes)
 * - **Value**: [Length] bytes of payload data
 *
 * Reference: ZVT Protocol Specification v13.13
 *
 * @author Erkan Kaplan
 * @since 2026-02-10
 */
object TlvParser {

    private const val TAG = "ZVT"

    /**
     * Represents a single TLV record.
     *
     * @property tag The TLV tag identifier (1-3 bytes combined into an Int).
     * @property value The raw byte payload.
     */
    data class TlvEntry(
        val tag: Int,
        val value: ByteArray
    ) {
        /** Returns the tag as a hex string, e.g. `"1F10"`. */
        val tagHex: String get() = String.format("%04X", tag)

        /** Returns the value as a hex string. */
        val valueHex: String get() = value.toHexString()

        /** Returns the value decoded as ASCII text. */
        val valueAscii: String get() = value.toSafeAscii()

        /** The byte length of the value. */
        val length: Int get() = value.size

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is TlvEntry) return false
            return tag == other.tag && value.contentEquals(other.value)
        }

        override fun hashCode(): Int = 31 * tag + value.contentHashCode()

        override fun toString(): String = "TLV(tag=$tagHex, len=$length, value=$valueHex)"
    }

    // =====================================================
    // Parse Operations
    // =====================================================

    /**
     * Parses all TLV entries from a byte array.
     *
     * Padding bytes (`0x00` and `0xFF`) are silently skipped.
     * Parsing stops on error or end of data.
     *
     * @param data Byte array containing TLV-encoded data.
     * @return List of parsed [TlvEntry] records.
     */
    fun parse(data: ByteArray): List<TlvEntry> {
        val entries = mutableListOf<TlvEntry>()
        var offset = 0

        while (offset < data.size) {
            // Skip padding bytes (0x00 or 0xFF)
            if (data[offset] == 0x00.toByte() || data[offset] == 0xFF.toByte()) {
                offset++
                continue
            }

            try {
                // Read tag
                val tagResult = readTag(data, offset)
                offset = tagResult.second

                // Read length
                val lengthResult = readLength(data, offset)
                val length = lengthResult.first
                offset = lengthResult.second

                // Read value
                if (offset + length > data.size) break
                val value = data.copyOfRange(offset, offset + length)
                offset += length

                val entry = TlvEntry(tagResult.first, value)
                Timber.tag(TAG).d("[TlvParser] Parsed TLV entry: tag=0x%s, len=%d, value=%s",
                    entry.tagHex, entry.length, entry.valueHex)
                entries.add(entry)
            } catch (e: Exception) {
                Timber.tag(TAG).w("[TlvParser] Parse error at offset %d: %s", offset, e.message)
                break
            }
        }

        return entries
    }

    /**
     * Searches for a specific tag in a TLV data block.
     *
     * @param data TLV-encoded byte array.
     * @param searchTag The tag to search for.
     * @return The matching [TlvEntry], or `null` if not found.
     */
    fun findTag(data: ByteArray, searchTag: Int): TlvEntry? {
        return parse(data).firstOrNull { it.tag == searchTag }
    }

    /**
     * Searches for multiple tags in a TLV data block.
     *
     * @param data TLV-encoded byte array.
     * @param searchTags The tags to search for.
     * @return A map of found tag -> [TlvEntry] pairs.
     */
    fun findTags(data: ByteArray, vararg searchTags: Int): Map<Int, TlvEntry> {
        val tagSet = searchTags.toSet()
        return parse(data)
            .filter { it.tag in tagSet }
            .associateBy { it.tag }
    }

    // =====================================================
    // Build Operations
    // =====================================================

    /**
     * Builds a single TLV record as a byte array.
     *
     * @param tag The TLV tag identifier.
     * @param value The payload bytes.
     * @return Serialized TLV bytes: [TAG][LENGTH][VALUE].
     */
    fun buildTlv(tag: Int, value: ByteArray): ByteArray {
        val tagBytes = encodeTag(tag)
        val lengthBytes = encodeLength(value.size)
        return tagBytes + lengthBytes + value
    }

    /**
     * Builds a concatenated byte array from multiple TLV entries.
     *
     * @param entries List of [TlvEntry] records to serialize.
     * @return Combined TLV byte array.
     */
    fun buildTlvList(entries: List<TlvEntry>): ByteArray {
        return entries.fold(byteArrayOf()) { acc, entry ->
            acc + buildTlv(entry.tag, entry.value)
        }
    }

    /**
     * Convenience method to build a TLV record with an ASCII string value.
     *
     * @param tag The TLV tag identifier.
     * @param text The ASCII text to encode as the value.
     * @return Serialized TLV bytes.
     */
    fun buildTlvAscii(tag: Int, text: String): ByteArray {
        return buildTlv(tag, text.toByteArray(Charsets.US_ASCII))
    }

    // =====================================================
    // Internal Helper Functions
    // =====================================================

    /**
     * Reads a TLV tag from the data at the given offset.
     *
     * If the lower 5 bits of the first byte are `0x1F`, this is a multi-byte tag
     * and subsequent bytes are read until a byte with bit 8 = 0 is encountered.
     *
     * @return Pair(tag value, new offset after reading).
     */
    private fun readTag(data: ByteArray, startOffset: Int): Pair<Int, Int> {
        var offset = startOffset
        val firstByte = data[offset].toUnsignedInt()
        offset++

        // Single-byte tag check
        if ((firstByte and 0x1F) != 0x1F) {
            return Pair(firstByte, offset)
        }

        // Multi-byte tag: continue reading until bit 8 of a byte is 0
        var tag = firstByte
        while (offset < data.size) {
            val nextByte = data[offset].toUnsignedInt()
            offset++
            tag = (tag shl 8) or nextByte

            // Last byte: bit 8 = 0
            if ((nextByte and 0x80) == 0) break
        }

        return Pair(tag, offset)
    }

    /**
     * Reads a BER-TLV length field from the data at the given offset.
     *
     * Encoding:
     * - `0x00-0x7F`: direct length (1 byte)
     * - `0x81`: length in the next 1 byte
     * - `0x82`: length in the next 2 bytes (big-endian)
     *
     * @return Pair(decoded length, new offset after reading).
     */
    private fun readLength(data: ByteArray, startOffset: Int): Pair<Int, Int> {
        var offset = startOffset
        val firstByte = data[offset].toUnsignedInt()
        offset++

        return when {
            firstByte <= 0x7F -> {
                Pair(firstByte, offset)
            }
            firstByte == 0x81 -> {
                val len = data[offset].toUnsignedInt()
                Pair(len, offset + 1)
            }
            firstByte == 0x82 -> {
                val len = (data[offset].toUnsignedInt() shl 8) or data[offset + 1].toUnsignedInt()
                Pair(len, offset + 2)
            }
            else -> {
                // Unknown length format, interpret as 0
                Pair(0, offset)
            }
        }
    }

    /**
     * Encodes a tag value into a byte array.
     *
     * @param tag The tag as an integer (up to 3 bytes wide).
     * @return Byte array representation of the tag.
     */
    private fun encodeTag(tag: Int): ByteArray {
        return when {
            tag <= 0xFF -> byteArrayOf(tag.toByte())
            tag <= 0xFFFF -> byteArrayOf((tag shr 8).toByte(), (tag and 0xFF).toByte())
            else -> byteArrayOf(
                (tag shr 16).toByte(),
                (tag shr 8 and 0xFF).toByte(),
                (tag and 0xFF).toByte()
            )
        }
    }

    /**
     * Encodes a length value into a BER-TLV length byte array.
     *
     * @param length The length to encode.
     * @return Byte array representation of the length.
     */
    private fun encodeLength(length: Int): ByteArray {
        return when {
            length <= 0x7F -> byteArrayOf(length.toByte())
            length <= 0xFF -> byteArrayOf(0x81.toByte(), length.toByte())
            else -> byteArrayOf(
                0x82.toByte(),
                (length shr 8).toByte(),
                (length and 0xFF).toByte()
            )
        }
    }
}
