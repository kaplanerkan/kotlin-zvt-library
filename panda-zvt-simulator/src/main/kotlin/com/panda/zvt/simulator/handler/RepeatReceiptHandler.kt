package com.panda.zvt.simulator.handler

import com.panda.zvt.simulator.protocol.ApduBuilder
import com.panda.zvt.simulator.protocol.ApduParser
import com.panda.zvt.simulator.protocol.ZvtProtocolConstants
import com.panda.zvt.simulator.response.StatusInfoBuilder
import com.panda.zvt.simulator.state.SimulatorState
import com.panda.zvt.simulator.state.TransactionStore

class RepeatReceiptHandler(
    private val state: SimulatorState,
    private val store: TransactionStore
) : CommandHandler {

    override suspend fun handle(apdu: ApduParser.ParsedApdu): List<ByteArray> {
        return listOf(
            ApduBuilder.ack(),
            StatusInfoBuilder.buildRepeatReceiptStatusInfo(store, state.config),
            ApduBuilder.buildPacket(ZvtProtocolConstants.RESP_COMPLETION)
        )
    }
}
