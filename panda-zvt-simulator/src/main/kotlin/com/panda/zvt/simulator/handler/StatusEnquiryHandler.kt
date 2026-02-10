package com.panda.zvt.simulator.handler

import com.panda.zvt.simulator.protocol.ApduBuilder
import com.panda.zvt.simulator.protocol.ApduParser
import com.panda.zvt.simulator.protocol.ZvtProtocolConstants
import com.panda.zvt.simulator.response.StatusInfoBuilder
import com.panda.zvt.simulator.state.SimulatorState

class StatusEnquiryHandler(private val state: SimulatorState) : CommandHandler {

    override suspend fun handle(apdu: ApduParser.ParsedApdu): List<ByteArray> {
        return listOf(
            ApduBuilder.ack(),
            StatusInfoBuilder.buildStatusEnquiryInfo(state.config),
            ApduBuilder.buildPacket(ZvtProtocolConstants.RESP_COMPLETION)
        )
    }
}
