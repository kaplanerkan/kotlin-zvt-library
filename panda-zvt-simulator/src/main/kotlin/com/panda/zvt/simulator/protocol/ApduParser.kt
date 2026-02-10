package com.panda.zvt.simulator.protocol

object ApduParser {

    data class ParsedApdu(
        val command: ByteArray,
        val data: ByteArray,
        val totalLength: Int
    )

    fun isAck(data: ByteArray): Boolean =
        data.size >= 2 && data[0] == 0x80.toByte() && data[1] == 0x00.toByte()

    fun isNack(data: ByteArray): Boolean =
        data.size >= 2 && data[0] == 0x84.toByte()

    /**
     * Extract amount (BMP 0x04) from command data.
     * Scans data for tag 0x04, then reads 6 BCD bytes.
     */
    fun extractAmount(data: ByteArray): Long? {
        val idx = findBmpTag(data, ZvtProtocolConstants.BMP_AMOUNT) ?: return null
        if (idx + 6 > data.size) return null
        return BcdEncoder.bcdToAmount(data.copyOfRange(idx, idx + 6))
    }

    /**
     * Extract receipt number (BMP 0x87) from command data.
     * Scans data for tag 0x87, then reads 2 BCD bytes.
     */
    fun extractReceiptNumber(data: ByteArray): Int? {
        val idx = findBmpTag(data, ZvtProtocolConstants.BMP_RECEIPT_NR) ?: return null
        if (idx + 2 > data.size) return null
        val str = BcdEncoder.bcdToString(data.copyOfRange(idx, idx + 2))
        return str.toIntOrNull()
    }

    /**
     * Extract trace number (BMP 0x0B) from command data.
     */
    fun extractTraceNumber(data: ByteArray): Int? {
        val idx = findBmpTag(data, ZvtProtocolConstants.BMP_TRACE_NUMBER) ?: return null
        if (idx + 3 > data.size) return null
        val str = BcdEncoder.bcdToString(data.copyOfRange(idx, idx + 3))
        return str.toIntOrNull()
    }

    /**
     * Extract AID (BMP 0x3B) from command data.
     */
    fun extractAid(data: ByteArray): ByteArray? {
        val idx = findBmpTag(data, ZvtProtocolConstants.BMP_AID) ?: return null
        if (idx + 8 > data.size) return null
        return data.copyOfRange(idx, idx + 8)
    }

    /**
     * Finds a BMP tag in the data and returns the index of the value (after the tag byte).
     * This is a simple scan — for the simulator's incoming command parsing.
     */
    private fun findBmpTag(data: ByteArray, tag: Byte): Int? {
        for (i in data.indices) {
            if (data[i] == tag) return i + 1
        }
        return null
    }
}
