package com.panda.zvt.simulator.protocol

import org.junit.Test
import org.junit.Assert.*

class LlvarEncoderTest {

    // --- encodeLlvar ---

    @Test
    fun encodeLlvar_zero_returnsF0F0() {
        val result = LlvarEncoder.encodeLlvar(0)
        assertEquals(2, result.size)
        assertArrayEquals(byteArrayOf(0xF0.toByte(), 0xF0.toByte()), result)
    }

    @Test
    fun encodeLlvar_3_returnsF0F3() {
        val result = LlvarEncoder.encodeLlvar(3)
        assertEquals(2, result.size)
        assertArrayEquals(byteArrayOf(0xF0.toByte(), 0xF3.toByte()), result)
    }

    @Test
    fun encodeLlvar_99_returnsF9F9() {
        val result = LlvarEncoder.encodeLlvar(99)
        assertEquals(2, result.size)
        assertArrayEquals(byteArrayOf(0xF9.toByte(), 0xF9.toByte()), result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun encodeLlvar_100_throwsException() {
        LlvarEncoder.encodeLlvar(100)
    }

    // --- encodeLllvar ---

    @Test
    fun encodeLllvar_zero_returnsF0F0F0() {
        val result = LlvarEncoder.encodeLllvar(0)
        assertEquals(3, result.size)
        assertArrayEquals(
            byteArrayOf(0xF0.toByte(), 0xF0.toByte(), 0xF0.toByte()),
            result
        )
    }

    @Test
    fun encodeLllvar_15_returnsF0F1F5() {
        val result = LlvarEncoder.encodeLllvar(15)
        assertEquals(3, result.size)
        assertArrayEquals(
            byteArrayOf(0xF0.toByte(), 0xF1.toByte(), 0xF5.toByte()),
            result
        )
    }

    @Test
    fun encodeLllvar_999_returnsF9F9F9() {
        val result = LlvarEncoder.encodeLllvar(999)
        assertEquals(3, result.size)
        assertArrayEquals(
            byteArrayOf(0xF9.toByte(), 0xF9.toByte(), 0xF9.toByte()),
            result
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun encodeLllvar_1000_throwsException() {
        LlvarEncoder.encodeLllvar(1000)
    }
}
