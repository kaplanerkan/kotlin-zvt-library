package com.panda.zvt_library.protocol

import com.panda.zvt_library.util.toHexString
import com.panda.zvt_library.util.toUnsignedInt

/**
 * ZVT APDU Paketi
 *
 * Her ZVT paketi şu yapıdadır:
 * ┌──────────┬──────────┬──────────┐
 * │ Command  │ Length   │ Data     │
 * │ (2 byte) │ (1-3 b) │ (N byte) │
 * └──────────┴──────────┴──────────┘
 *
 * Length kodlaması:
 * - 0x00-0xFE: direkt uzunluk (1 byte)
 * - 0xFF + 2 byte: uzun format (LL HH, little-endian)
 */
data class ZvtPacket(
    /** Komut kodu (2 byte) - ör: [0x06, 0x01] */
    val command: ByteArray,
    /** Veri alanı (0-N byte) */
    val data: ByteArray = byteArrayOf()
) {
    /** Komut kodunu hex string olarak döner: "06 01" */
    val commandHex: String get() = command.toHexString()

    /** Veri uzunluğu */
    val dataLength: Int get() = data.size

    /** Paketin ACK olup olmadığını kontrol eder */
    val isAck: Boolean
        get() = command.size >= 1 && command[0] == 0x80.toByte()

    /** Paketin NACK olup olmadığını kontrol eder */
    val isNack: Boolean
        get() = command.size >= 1 && command[0] == 0x84.toByte()

    /** Paketin Completion (06 0F) olup olmadığını kontrol eder */
    val isCompletion: Boolean
        get() = command.contentEquals(ZvtConstants.RESP_COMPLETION)

    /** Paketin Status Info (04 0F) olup olmadığını kontrol eder */
    val isStatusInfo: Boolean
        get() = command.contentEquals(ZvtConstants.RESP_STATUS_INFO)

    /** Paketin Intermediate Status (04 FF) olup olmadığını kontrol eder */
    val isIntermediateStatus: Boolean
        get() = command.contentEquals(ZvtConstants.RESP_INTERMEDIATE_STATUS)

    /** Paketin Abort (06 1E) olup olmadığını kontrol eder */
    val isAbort: Boolean
        get() = command.contentEquals(ZvtConstants.RESP_ABORT)

    /** Paketin Print Line (06 D1) olup olmadığını kontrol eder */
    val isPrintLine: Boolean
        get() = command.contentEquals(ZvtConstants.RESP_PRINT_LINE)

    /**
     * Paketi wire format'a (gönderilecek byte dizisi) serialize eder
     *
     * Format: CMD1 CMD2 LEN DATA
     */
    fun toBytes(): ByteArray {
        val lengthBytes = encodeLength(data.size)
        return command + lengthBytes + data
    }

    /**
     * Uzunluk bilgisini kodlar
     * - 0-254: 1 byte
     * - 255+: 0xFF + 2 byte (little-endian)
     */
    private fun encodeLength(length: Int): ByteArray {
        return if (length < 0xFF) {
            byteArrayOf(length.toByte())
        } else {
            byteArrayOf(
                0xFF.toByte(),
                (length and 0xFF).toByte(),         // Low byte
                ((length shr 8) and 0xFF).toByte()   // High byte
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ZvtPacket) return false
        return command.contentEquals(other.command) && data.contentEquals(other.data)
    }

    override fun hashCode(): Int = 31 * command.contentHashCode() + data.contentHashCode()

    override fun toString(): String =
        "ZvtPacket(cmd=${commandHex}, dataLen=${dataLength}, data=${data.toHexString()})"

    companion object {
        /**
         * Wire format byte dizisinden ZvtPacket oluşturur
         * @return Pair(packet, okunan byte sayısı) veya null
         */
        fun fromBytes(buffer: ByteArray, offset: Int = 0): Pair<ZvtPacket, Int>? {
            if (buffer.size - offset < 3) return null // En az CMD(2) + LEN(1)

            val command = buffer.copyOfRange(offset, offset + 2)
            var pos = offset + 2

            // Length oku
            val firstLenByte = buffer[pos].toUnsignedInt()
            pos++

            val dataLength: Int
            if (firstLenByte == 0xFF) {
                // Uzun format: sonraki 2 byte (little-endian)
                if (buffer.size - pos < 2) return null
                dataLength = buffer[pos].toUnsignedInt() or (buffer[pos + 1].toUnsignedInt() shl 8)
                pos += 2
            } else {
                dataLength = firstLenByte
            }

            // Data oku
            if (buffer.size - pos < dataLength) return null
            val data = if (dataLength > 0) {
                buffer.copyOfRange(pos, pos + dataLength)
            } else {
                byteArrayOf()
            }
            pos += dataLength

            return Pair(ZvtPacket(command, data), pos - offset)
        }

        /** ACK paketi oluşturur */
        fun ack(): ZvtPacket = ZvtPacket(ZvtConstants.ACK)

        /** NACK paketi oluşturur */
        fun nack(): ZvtPacket = ZvtPacket(ZvtConstants.NACK)
    }
}
