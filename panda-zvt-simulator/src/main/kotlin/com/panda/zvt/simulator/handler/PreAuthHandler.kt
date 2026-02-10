package com.panda.zvt.simulator.handler

import com.panda.zvt.simulator.protocol.ApduBuilder
import com.panda.zvt.simulator.protocol.ApduParser
import com.panda.zvt.simulator.protocol.ZvtProtocolConstants
import com.panda.zvt.simulator.response.IntermediateStatusBuilder
import com.panda.zvt.simulator.response.StatusInfoBuilder
import com.panda.zvt.simulator.state.SimulatorState
import com.panda.zvt.simulator.state.StoredTransaction
import com.panda.zvt.simulator.state.TransactionStore
import java.time.LocalDateTime

class PreAuthHandler(
    private val state: SimulatorState,
    private val store: TransactionStore
) : CommandHandler {

    override suspend fun handle(apdu: ApduParser.ParsedApdu): List<ByteArray> {
        val config = state.config
        val responses = mutableListOf<ByteArray>()

        responses.add(ApduBuilder.ack())

        if (config.errorSimulation.shouldError()) {
            responses.add(StatusInfoBuilder.buildErrorStatusInfo(config.errorSimulation.getErrorCode(), config))
            responses.add(ApduBuilder.buildPacket(ZvtProtocolConstants.RESP_COMPLETION))
            return responses
        }

        responses.add(IntermediateStatusBuilder.build(ZvtProtocolConstants.IS_INSERT_CARD))
        responses.add(IntermediateStatusBuilder.build(ZvtProtocolConstants.IS_WATCH_PIN_PAD))
        responses.add(IntermediateStatusBuilder.build(ZvtProtocolConstants.IS_PLEASE_WAIT))

        val amount = ApduParser.extractAmount(apdu.data) ?: 0L
        val trace = state.nextTraceNumber()
        val receipt = state.nextReceiptNumber()
        val turnover = state.nextTurnoverNumber()
        val now = LocalDateTime.now()

        responses.add(StatusInfoBuilder.buildPaymentStatusInfo(amount, trace, receipt, turnover, now, config))

        store.recordTransaction(StoredTransaction(
            type = "Pre-Authorization", amount = amount, trace = trace, receipt = receipt,
            turnover = turnover, timestamp = now, cardData = config.cardData
        ))

        responses.add(ApduBuilder.buildPacket(ZvtProtocolConstants.RESP_COMPLETION))
        return responses
    }
}
