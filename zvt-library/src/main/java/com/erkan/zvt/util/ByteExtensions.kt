package com.erkan.zvt.util

/**
 * ByteArray ve Byte için ZVT protokolüne özel extension fonksiyonları
 */

/** ByteArray'i hex string'e çevirir: [0x06, 0x01] → "06 01" */
fun ByteArray.toHexString(separator: String = " "): String =
    joinToString(separator) { String.format("%02X", it) }

/** Hex string'i ByteArray'e çevirir: "06 01" → [0x06, 0x01] */
fun String.hexToByteArray(): ByteArray {
    val clean = replace(" ", "").replace(":", "").replace("-", "")
    require(clean.length % 2 == 0) { "Hex string çift uzunlukta olmalı" }
    return ByteArray(clean.length / 2) { i ->
        clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
}

/** Byte'ı unsigned Int'e çevirir (0-255) */
fun Byte.toUnsignedInt(): Int = toInt() and 0xFF

/** İki byte'ı birleştirip unsigned Short değeri döner */
fun bytesToUnsignedShort(high: Byte, low: Byte): Int =
    (high.toUnsignedInt() shl 8) or low.toUnsignedInt()

/** Int değerini 2 byte'a ayırır [high, low] */
fun Int.toTwoBytes(): ByteArray = byteArrayOf(
    (this shr 8 and 0xFF).toByte(),
    (this and 0xFF).toByte()
)

/** ByteArray'den belirli pozisyondan itibaren n byte okur */
fun ByteArray.readBytes(offset: Int, length: Int): ByteArray {
    require(offset >= 0 && offset + length <= size) {
        "Geçersiz okuma: offset=$offset, length=$length, size=$size"
    }
    return copyOfRange(offset, offset + length)
}

/** İki ByteArray'in komut kısmını karşılaştırır (ilk 2 byte) */
fun ByteArray.commandEquals(other: ByteArray): Boolean {
    if (size < 2 || other.size < 2) return false
    return this[0] == other[0] && this[1] == other[1]
}

/** ByteArray'i güvenli şekilde ASCII string'e çevirir */
fun ByteArray.toSafeAscii(): String =
    String(this, Charsets.US_ASCII).replace(Regex("[^\\x20-\\x7E]"), ".")

/** Log için kısa hex özeti */
fun ByteArray.toLogString(maxBytes: Int = 32): String {
    val hex = take(maxBytes).toByteArray().toHexString()
    return if (size > maxBytes) "$hex... (${size} bytes)" else hex
}
