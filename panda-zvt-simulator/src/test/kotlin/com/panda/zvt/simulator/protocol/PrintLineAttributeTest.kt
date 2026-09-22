package com.panda.zvt.simulator.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** ZVT 13.13, chapter 3.5, Table 16 "Definition of <attribute>". */
class PrintLineAttributeTest {

    @Test
    fun lastLine_normalText_avoidsTheReservedBareFlag() {
        // 80h on its own is RFU, so a plain last line must carry formatting.
        val attr = PrintLineAttribute.lastLine()
        assertEquals(0xC0.toByte(), attr)
        assertTrue(PrintLineAttribute.isLastLine(attr))
    }

    @Test
    fun lastLine_keepsExistingFormatting() {
        assertEquals(0x90.toByte(), PrintLineAttribute.lastLine(PrintLineAttribute.DOUBLE_HEIGHT))
        assertEquals(0xA0.toByte(), PrintLineAttribute.lastLine(PrintLineAttribute.DOUBLE_WIDTH))
        assertEquals(0xC0.toByte(), PrintLineAttribute.lastLine(PrintLineAttribute.CENTRED))
    }

    @Test
    fun lastLine_keepsIndent() {
        val attr = PrintLineAttribute.lastLine(PrintLineAttribute.indent(3))
        assertEquals(0x83.toByte(), attr)
        assertTrue(PrintLineAttribute.isLastLine(attr))
    }

    @Test
    fun isLastLine_rejectsRfuAndLinefeed() {
        assertFalse(PrintLineAttribute.isLastLine(0x80.toByte()))
        assertFalse(PrintLineAttribute.isLastLine(PrintLineAttribute.LINEFEED))
        assertFalse(PrintLineAttribute.isLastLine(PrintLineAttribute.NORMAL))
        assertFalse(PrintLineAttribute.isLastLine(PrintLineAttribute.CENTRED))
    }

    @Test
    fun indent_isTheLowNibble() {
        assertEquals(0x00.toByte(), PrintLineAttribute.indent(0))
        assertEquals(0x0F.toByte(), PrintLineAttribute.indent(15))
    }

    @Test(expected = IllegalArgumentException::class)
    fun indent_rejectsOutOfRange() {
        PrintLineAttribute.indent(16)
    }
}
