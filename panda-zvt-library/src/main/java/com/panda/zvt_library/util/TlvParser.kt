package com.panda.zvt_library.util

/**
 * TLV (Tag-Length-Value) Parser
 *
 * ZVT protokolünde bazı veri alanları (BMP 0x3C gibi) TLV formatında kodlanır.
 * Bu parser, TLV yapılarını okur ve yazar.
 *
 * TLV Formatı:
 * - Tag: 1-3 byte (eğer ilk byte'ın alt 5 biti 11111 ise multi-byte tag)
 * - Length: 1-3 byte (eğer ilk byte 0x81 ise 2 byte, 0x82 ise 3 byte uzunluk)
 * - Value: Length kadar byte
 */
object TlvParser {

    /**
     * Tek bir TLV kaydı
     */
    data class TlvEntry(
        val tag: Int,
        val value: ByteArray
    ) {
        /** Tag'i hex string olarak döner */
        val tagHex: String get() = String.format("%04X", tag)

        /** Value'yu hex string olarak döner */
        val valueHex: String get() = value.toHexString()

        /** Value'yu ASCII string olarak döner */
        val valueAscii: String get() = value.toSafeAscii()

        /** Value uzunluğu */
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
    // Parse İşlemleri
    // =====================================================

    /**
     * ByteArray'den tüm TLV kayıtlarını okur
     * @param data TLV verisini içeren byte dizisi
     * @return TLV kayıt listesi
     */
    fun parse(data: ByteArray): List<TlvEntry> {
        val entries = mutableListOf<TlvEntry>()
        var offset = 0

        while (offset < data.size) {
            // Padding byte'ları atla (0x00 veya 0xFF)
            if (data[offset] == 0x00.toByte() || data[offset] == 0xFF.toByte()) {
                offset++
                continue
            }

            try {
                // Tag oku
                val tagResult = readTag(data, offset)
                offset = tagResult.second

                // Length oku
                val lengthResult = readLength(data, offset)
                val length = lengthResult.first
                offset = lengthResult.second

                // Value oku
                if (offset + length > data.size) break
                val value = data.copyOfRange(offset, offset + length)
                offset += length

                entries.add(TlvEntry(tagResult.first, value))
            } catch (e: Exception) {
                // Parse hatası, kalan veriyi atla
                break
            }
        }

        return entries
    }

    /**
     * Belirli bir tag'i arar
     * @param data TLV verisi
     * @param searchTag Aranan tag
     * @return Bulunan TlvEntry veya null
     */
    fun findTag(data: ByteArray, searchTag: Int): TlvEntry? {
        return parse(data).firstOrNull { it.tag == searchTag }
    }

    /**
     * Birden fazla tag'i arar
     */
    fun findTags(data: ByteArray, vararg searchTags: Int): Map<Int, TlvEntry> {
        val tagSet = searchTags.toSet()
        return parse(data)
            .filter { it.tag in tagSet }
            .associateBy { it.tag }
    }

    // =====================================================
    // Build İşlemleri
    // =====================================================

    /**
     * Tek bir TLV kaydı oluşturur
     */
    fun buildTlv(tag: Int, value: ByteArray): ByteArray {
        val tagBytes = encodeTag(tag)
        val lengthBytes = encodeLength(value.size)
        return tagBytes + lengthBytes + value
    }

    /**
     * Birden fazla TLV kaydından byte dizisi oluşturur
     */
    fun buildTlvList(entries: List<TlvEntry>): ByteArray {
        return entries.fold(byteArrayOf()) { acc, entry ->
            acc + buildTlv(entry.tag, entry.value)
        }
    }

    /**
     * Kolay TLV oluşturma - ASCII değerli
     */
    fun buildTlvAscii(tag: Int, text: String): ByteArray {
        return buildTlv(tag, text.toByteArray(Charsets.US_ASCII))
    }

    // =====================================================
    // İç Yardımcı Fonksiyonlar
    // =====================================================

    /**
     * Tag okur ve (tag, yeniOffset) döner
     * - Eğer ilk byte'ın alt 5 biti 0x1F (11111) ise → multi-byte tag
     */
    private fun readTag(data: ByteArray, startOffset: Int): Pair<Int, Int> {
        var offset = startOffset
        val firstByte = data[offset].toUnsignedInt()
        offset++

        // Single byte tag kontrolü
        if ((firstByte and 0x1F) != 0x1F) {
            return Pair(firstByte, offset)
        }

        // Multi-byte tag
        var tag = firstByte
        while (offset < data.size) {
            val nextByte = data[offset].toUnsignedInt()
            offset++
            tag = (tag shl 8) or nextByte

            // Son byte: bit 8 = 0
            if ((nextByte and 0x80) == 0) break
        }

        return Pair(tag, offset)
    }

    /**
     * Length okur ve (uzunluk, yeniOffset) döner
     * - 0x00-0x7F: direkt uzunluk (1 byte)
     * - 0x81: sonraki 1 byte uzunluk
     * - 0x82: sonraki 2 byte uzunluk
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
                // Bilinmeyen format, 0 olarak yorumla
                Pair(0, offset)
            }
        }
    }

    /**
     * Tag değerini byte dizisine kodlar
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
     * Uzunluk değerini byte dizisine kodlar
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
