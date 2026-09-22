package com.panda.zvt.simulator.response

import com.panda.zvt.simulator.protocol.ApduBuilder
import com.panda.zvt.simulator.protocol.PrintLineAttribute
import com.panda.zvt.simulator.protocol.ZvtProtocolConstants

object PrintLineBuilder {

    fun build(text: String, attribute: Byte = PrintLineAttribute.NORMAL): ByteArray {
        val textBytes = text.toByteArray(Charsets.US_ASCII)
        val data = byteArrayOf(attribute) + textBytes
        return ApduBuilder.buildPacket(ZvtProtocolConstants.RESP_PRINT_LINE, data)
    }

    /**
     * Last line of a receipt. Tells the ECR the receipt is complete, so a
     * buffering or cutter-driven printer knows where the slip ends.
     */
    fun buildLastLine(text: String, attribute: Byte = PrintLineAttribute.NORMAL): ByteArray =
        build(text, PrintLineAttribute.lastLine(attribute))

    /**
     * Linefeed-only line: attribute `FF`, text is a single byte holding the
     * number of feeds (ZVT 13.13, chapter 3.5).
     */
    fun buildLinefeed(count: Int): ByteArray {
        require(count in 0..255) { "Linefeed count must be 0..255, was $count" }
        val data = byteArrayOf(PrintLineAttribute.LINEFEED, count.toByte())
        return ApduBuilder.buildPacket(ZvtProtocolConstants.RESP_PRINT_LINE, data)
    }
}
