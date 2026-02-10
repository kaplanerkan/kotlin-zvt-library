package com.panda.zvt.simulator.handler

import com.panda.zvt.simulator.protocol.ApduBuilder
import com.panda.zvt.simulator.protocol.ApduParser
import com.panda.zvt.simulator.protocol.ZvtProtocolConstants
import com.panda.zvt.simulator.response.StatusInfoBuilder
import com.panda.zvt.simulator.state.SimulatorState
import com.panda.zvt.simulator.state.StoredTransaction
import com.panda.zvt.simulator.state.TransactionStore
import java.time.LocalDateTime

class ReversalHandler(
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

        val receipt = ApduParser.extractReceiptNumber(apdu.data) ?: 0
        val trace = state.nextTraceNumber()
        val turnover = state.nextTurnoverNumber()
        val now = LocalDateTime.now()

        // Reversal returns the original amount (use last transaction or 0)
        val lastTxn = store.getLastTransaction()
        val amount = lastTxn?.amount ?: 0L

        responses.add(StatusInfoBuilder.buildPaymentStatusInfo(amount, trace, receipt, turnover, now, config))

        store.recordTransaction(StoredTransaction(
            type = "Reversal", amount = amount, trace = trace, receipt = receipt,
            turnover = turnover, timestamp = now, cardData = config.cardData
        ))

        responses.add(ApduBuilder.buildPacket(ZvtProtocolConstants.RESP_COMPLETION))
        return responses
    }
}
