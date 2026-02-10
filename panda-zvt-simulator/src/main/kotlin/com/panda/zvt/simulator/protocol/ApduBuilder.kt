package com.panda.zvt.simulator.protocol

object ApduBuilder {

    fun ack(): ByteArray = ZvtProtocolConstants.ACK.copyOf()

    fun nack(): ByteArray = ZvtProtocolConstants.NACK.copyOf()

    fun buildPacket(command: ByteArray, data: ByteArray = byteArrayOf()): ByteArray {
        val lengthBytes = encodeLength(data.size)
        return command + lengthBytes + data
    }

    private fun encodeLength(length: Int): ByteArray {
        return if (length <= 0xFE) {
            byteArrayOf(length.toByte())
        } else {
            // Extended: 0xFF + 2 bytes little-endian
            byteArrayOf(
                0xFF.toByte(),
                (length and 0xFF).toByte(),
                ((length shr 8) and 0xFF).toByte()
            )
        }
    }
}
