package com.panda.zvt.simulator.handler

import com.panda.zvt.simulator.protocol.ApduBuilder
import com.panda.zvt.simulator.protocol.ApduParser
import com.panda.zvt.simulator.protocol.ZvtProtocolConstants

class AbortHandler : CommandHandler {

    override suspend fun handle(apdu: ApduParser.ParsedApdu): List<ByteArray> {
        return listOf(
            ApduBuilder.ack(),
            ApduBuilder.buildPacket(ZvtProtocolConstants.RESP_COMPLETION)
        )
    }
}
