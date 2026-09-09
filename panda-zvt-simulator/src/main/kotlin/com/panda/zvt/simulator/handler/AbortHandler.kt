package com.panda.zvt.simulator.handler

import com.panda.zvt.simulator.protocol.ApduBuilder
import com.panda.zvt.simulator.protocol.ApduParser
import com.panda.zvt.simulator.protocol.ZvtProtocolConstants

class AbortHandler : CommandHandler {

    override suspend fun handle(apdu: ApduParser.ParsedApdu): List<ByteArray> {
        // ZVT abort: ACK + 06 1E with error code 0x6C ("Vorgang abgebrochen").
        // A completion would end the ECR transaction as a SUCCESSFUL payment.
        return listOf(
            ApduBuilder.ack(),
            ApduBuilder.buildPacket(ZvtProtocolConstants.RESP_ABORT, byteArrayOf(0x6C))
        )
    }
}
