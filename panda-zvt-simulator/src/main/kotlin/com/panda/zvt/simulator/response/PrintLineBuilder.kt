package com.panda.zvt.simulator.response

import com.panda.zvt.simulator.protocol.ApduBuilder
import com.panda.zvt.simulator.protocol.ZvtProtocolConstants

object PrintLineBuilder {

    fun build(text: String, attribute: Byte = 0x00): ByteArray {
        val textBytes = text.toByteArray(Charsets.US_ASCII)
        val data = byteArrayOf(attribute) + textBytes
        return ApduBuilder.buildPacket(ZvtProtocolConstants.RESP_PRINT_LINE, data)
    }
}
