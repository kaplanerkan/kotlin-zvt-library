package com.panda.zvt.simulator.handler

import com.panda.zvt.simulator.protocol.ApduBuilder
import com.panda.zvt.simulator.protocol.ApduParser
import com.panda.zvt.simulator.state.SimulatorState

class LogOffHandler(private val state: SimulatorState) : CommandHandler {

    override suspend fun handle(apdu: ApduParser.ParsedApdu): List<ByteArray> {
        state.setRegistered(false)
        // Log Off only requires ACK, no completion
        return listOf(ApduBuilder.ack())
    }
}
