package com.panda.zvt.simulator.handler

import com.panda.zvt.simulator.protocol.ApduBuilder
import com.panda.zvt.simulator.protocol.ApduParser
import com.panda.zvt.simulator.protocol.ZvtProtocolConstants
import com.panda.zvt.simulator.response.IntermediateStatusBuilder
import com.panda.zvt.simulator.response.PrintLineBuilder
import com.panda.zvt.simulator.response.ReceiptGenerator
import com.panda.zvt.simulator.response.StatusInfoBuilder
import com.panda.zvt.simulator.state.SimulatorState
import com.panda.zvt.simulator.state.TransactionStore

class EndOfDayHandler(
    private val state: SimulatorState,
    private val store: TransactionStore
) : CommandHandler {

    override suspend fun handle(apdu: ApduParser.ParsedApdu): List<ByteArray> {
        val config = state.config
        val responses = mutableListOf<ByteArray>()

        responses.add(ApduBuilder.ack())
        responses.add(IntermediateStatusBuilder.build(ZvtProtocolConstants.IS_PLEASE_WAIT))

        // Print receipt lines
        val receiptLines = ReceiptGenerator.generateEndOfDayReceipt(store, config)
        for (line in receiptLines) {
            responses.add(PrintLineBuilder.build(line))
        }

        // Status info
        responses.add(StatusInfoBuilder.buildEndOfDayStatusInfo(store, config))

        // Clear batch
        store.clearBatch()

        // Completion
        responses.add(ApduBuilder.buildPacket(ZvtProtocolConstants.RESP_COMPLETION))
        return responses
    }
}
