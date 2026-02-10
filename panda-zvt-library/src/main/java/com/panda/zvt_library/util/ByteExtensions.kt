package com.panda.zvt_library.util

/**
 * Byte array and Byte extension functions for the ZVT protocol.
 *
 * Provides hex conversion, unsigned operations, and safe ASCII conversion
 * utilities used throughout the ZVT library.
 *
 * @author Erkan Kaplan
 * @since 2026-02-10
 */

/**
 * Converts a byte array to a hex string representation.
 *
 * Example: `[0x06, 0x01]` -> `"06 01"`
 *
 * @param separator Character(s) placed between each byte. Default is a single space.
 * @return Hex string representation of the byte array.
 */
fun ByteArray.toHexString(separator: String = " "): String =
    joinToString(separator) { String.format("%02X", it) }

/**
 * Converts a hex string to a byte array.
 *
 * Supports hex strings with spaces, colons, or dashes as separators.
 *
 * Example: `"06 01"` -> `[0x06, 0x01]`
 *
 * @return Byte array parsed from the hex string.
 * @throws IllegalArgumentException if the cleaned hex string has odd length.
 */
fun String.hexToByteArray(): ByteArray {
    val clean = replace(" ", "").replace(":", "").replace("-", "")
    require(clean.length % 2 == 0) { "Hex string must have even length" }
    return ByteArray(clean.length / 2) { i ->
        clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
}

/**
 * Converts a signed Byte to an unsigned Int value (0-255).
 *
 * @return Unsigned integer value of the byte.
 */
fun Byte.toUnsignedInt(): Int = toInt() and 0xFF

/**
 * Combines two bytes into an unsigned 16-bit value (big-endian).
 *
 * @param high The most significant byte.
 * @param low The least significant byte.
 * @return Combined unsigned 16-bit integer value.
 */
fun bytesToUnsignedShort(high: Byte, low: Byte): Int =
    (high.toUnsignedInt() shl 8) or low.toUnsignedInt()

/**
 * Splits an Int value into 2 bytes (big-endian): [high, low].
 *
 * @return Byte array of length 2: [high byte, low byte].
 */
fun Int.toTwoBytes(): ByteArray = byteArrayOf(
    (this shr 8 and 0xFF).toByte(),
    (this and 0xFF).toByte()
)

/**
 * Reads [length] bytes from this byte array starting at [offset].
 *
 * @param offset Starting position in the array.
 * @param length Number of bytes to read.
 * @return Sub-array of the specified range.
 * @throws IllegalArgumentException if the range is out of bounds.
 */
fun ByteArray.readBytes(offset: Int, length: Int): ByteArray {
    require(offset >= 0 && offset + length <= size) {
        "Invalid read: offset=$offset, length=$length, size=$size"
    }
    return copyOfRange(offset, offset + length)
}

/**
 * Compares the command portion (first 2 bytes) of two byte arrays.
 *
 * @param other The other byte array to compare with.
 * @return `true` if both arrays have at least 2 bytes and the first 2 bytes match.
 */
fun ByteArray.commandEquals(other: ByteArray): Boolean {
    if (size < 2 || other.size < 2) return false
    return this[0] == other[0] && this[1] == other[1]
}

/**
 * Converts a byte array to a printable ASCII string.
 *
 * Non-printable characters (outside 0x20-0x7E range) are replaced with '.'.
 *
 * @return Safe ASCII representation of the byte array.
 */
fun ByteArray.toSafeAscii(): String =
    String(this, Charsets.US_ASCII).replace(Regex("[^\\x20-\\x7E]"), ".")

/**
 * Creates a short hex summary for logging purposes.
 *
 * If the array exceeds [maxBytes], the output is truncated with `"... (N bytes)"`.
 *
 * @param maxBytes Maximum number of bytes to include in the hex output. Default is 64.
 * @return Hex string, potentially truncated, for log display.
 */
fun ByteArray.toLogString(maxBytes: Int = 64): String {
    val hex = take(maxBytes).toByteArray().toHexString()
    return if (size > maxBytes) "$hex... (${size} bytes)" else hex
}
