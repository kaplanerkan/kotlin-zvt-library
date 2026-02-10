package com.panda.zvt.simulator.response

import com.panda.zvt.simulator.protocol.ApduBuilder
import com.panda.zvt.simulator.protocol.ZvtProtocolConstants

object IntermediateStatusBuilder {

    fun build(statusCode: Byte): ByteArray {
        return ApduBuilder.buildPacket(
            ZvtProtocolConstants.RESP_INTERMEDIATE_STATUS,
            byteArrayOf(statusCode)
        )
    }
}
