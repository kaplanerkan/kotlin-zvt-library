package com.panda.zvt.simulator.protocol

object LlvarEncoder {

    /**
     * Encodes a 2-byte LLVAR length in FxFy nibble format.
     * E.g., length=10 -> F1 F0, length=3 -> F0 F3
     */
    fun encodeLlvar(length: Int): ByteArray {
        require(length in 0..99) { "LLVAR length must be 0-99: $length" }
        val d1 = length / 10
        val d2 = length % 10
        return byteArrayOf((0xF0 or d1).toByte(), (0xF0 or d2).toByte())
    }

    /**
     * Encodes a 3-byte LLLVAR length in FxFyFz nibble format.
     * E.g., length=15 -> F0 F1 F5, length=123 -> F1 F2 F3
     */
    fun encodeLllvar(length: Int): ByteArray {
        require(length in 0..999) { "LLLVAR length must be 0-999: $length" }
        val d1 = length / 100
        val d2 = (length / 10) % 10
        val d3 = length % 10
        return byteArrayOf(
            (0xF0 or d1).toByte(),
            (0xF0 or d2).toByte(),
            (0xF0 or d3).toByte()
        )
    }
}
