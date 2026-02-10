package com.panda.zvt_library.protocol

import com.panda.zvt_library.util.toHexString
import com.panda.zvt_library.util.toUnsignedInt

/**
 * ZVT APDU (Application Protocol Data Unit) packet.
 *
 * Every ZVT packet has the following wire format:
 * ```
 * +----------+----------+----------+
 * | Command  | Length   | Data     |
 * | (2 byte) | (1-3 b) | (N byte) |
 * +----------+----------+----------+
 * ```
 *
 * Length encoding:
 * - `0x00-0xFE`: direct length in 1 byte
 * - `0xFF` + 2 bytes: extended format (LL HH, little-endian)
 *
 * Reference: ZVT Protocol Specification v13.13
 *
 * @property command Command code (2 bytes), e.g. `[0x06, 0x01]` for Authorization.
 * @property data Data payload (0-N bytes).
 *
 * @author Erkan Kaplan
 * @since 2026-02-10
 */
data class ZvtPacket(
    val command: ByteArray,
    val data: ByteArray = byteArrayOf()
) {
    /** Returns the command code as a hex string, e.g. `"06 01"`. */
    val commandHex: String get() = command.toHexString()

    /** Returns the length of the data payload. */
    val dataLength: Int get() = data.size

    /** Checks whether this packet is an ACK (positive acknowledgement, first byte = 0x80). */
    val isAck: Boolean
        get() = command.size >= 1 && command[0] == 0x80.toByte()

    /** Checks whether this packet is a NACK (negative acknowledgement, first byte = 0x84). */
    val isNack: Boolean
        get() = command.size >= 1 && command[0] == 0x84.toByte()

    /** Checks whether this packet is a Completion response (06 0F). */
    val isCompletion: Boolean
        get() = command.contentEquals(ZvtConstants.RESP_COMPLETION)

    /** Checks whether this packet is a Status Information response (04 0F). */
    val isStatusInfo: Boolean
        get() = command.contentEquals(ZvtConstants.RESP_STATUS_INFO)

    /** Checks whether this packet is an Intermediate Status response (04 FF). */
    val isIntermediateStatus: Boolean
        get() = command.contentEquals(ZvtConstants.RESP_INTERMEDIATE_STATUS)

    /** Checks whether this packet is an Abort response (06 1E). */
    val isAbort: Boolean
        get() = command.contentEquals(ZvtConstants.RESP_ABORT)

    /** Checks whether this packet is a Print Line response (06 D1). */
    val isPrintLine: Boolean
        get() = command.contentEquals(ZvtConstants.RESP_PRINT_LINE)

    /**
     * Serializes this packet to the wire format (byte array for transmission).
     *
     * Format: `CMD1 CMD2 LEN [DATA...]`
     *
     * @return Byte array ready to be sent over TCP.
     */
    fun toBytes(): ByteArray {
        val lengthBytes = encodeLength(data.size)
        return command + lengthBytes + data
    }

    /**
     * Encodes the data length into the ZVT length format.
     *
     * - 0-254: single byte
     * - 255+: `0xFF` followed by 2 bytes (little-endian)
     *
     * @param length The data length to encode.
     * @return Encoded length bytes.
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
         * Deserializes a [ZvtPacket] from a wire format byte array.
         *
         * @param buffer The byte buffer containing the packet data.
         * @param offset Starting position within the buffer.
         * @return A Pair of (parsed packet, bytes consumed), or `null` if insufficient data.
         */
        fun fromBytes(buffer: ByteArray, offset: Int = 0): Pair<ZvtPacket, Int>? {
            if (buffer.size - offset < 3) return null // Minimum: CMD(2) + LEN(1)

            val command = buffer.copyOfRange(offset, offset + 2)
            var pos = offset + 2

            // Read length
            val firstLenByte = buffer[pos].toUnsignedInt()
            pos++

            val dataLength: Int
            if (firstLenByte == 0xFF) {
                // Extended format: next 2 bytes (little-endian)
                if (buffer.size - pos < 2) return null
                dataLength = buffer[pos].toUnsignedInt() or (buffer[pos + 1].toUnsignedInt() shl 8)
                pos += 2
            } else {
                dataLength = firstLenByte
            }

            // Read data
            if (buffer.size - pos < dataLength) return null
            val data = if (dataLength > 0) {
                buffer.copyOfRange(pos, pos + dataLength)
            } else {
                byteArrayOf()
            }
            pos += dataLength

            return Pair(ZvtPacket(command, data), pos - offset)
        }

        /** Creates an ACK (positive acknowledgement) packet: `80 00 00`. */
        fun ack(): ZvtPacket = ZvtPacket(ZvtConstants.ACK)

        /** Creates a NACK (negative acknowledgement) packet: `84 00 00`. */
        fun nack(): ZvtPacket = ZvtPacket(ZvtConstants.NACK)
    }
}
