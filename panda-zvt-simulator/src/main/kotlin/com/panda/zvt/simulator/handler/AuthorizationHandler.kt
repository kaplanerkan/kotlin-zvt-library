package com.panda.zvt.simulator.handler

import com.panda.zvt.simulator.protocol.ApduBuilder
import com.panda.zvt.simulator.protocol.ApduParser
import com.panda.zvt.simulator.protocol.ZvtProtocolConstants
import com.panda.zvt.simulator.response.AmountFormatter
import com.panda.zvt.simulator.response.IntermediateStatusBuilder
import com.panda.zvt.simulator.response.PrintLineBuilder
import com.panda.zvt.simulator.response.StatusInfoBuilder
import com.panda.zvt.simulator.state.SimulatorState
import com.panda.zvt.simulator.state.StoredTransaction
import com.panda.zvt.simulator.state.TransactionStore
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AuthorizationHandler(
    private val state: SimulatorState,
    private val store: TransactionStore
) : CommandHandler {

    override suspend fun handle(apdu: ApduParser.ParsedApdu): List<ByteArray> {
        val config = state.config
        val responses = mutableListOf<ByteArray>()

        // 1. ACK
        responses.add(ApduBuilder.ack())

        // Check error simulation
        if (config.errorSimulation.shouldError()) {
            responses.add(StatusInfoBuilder.buildErrorStatusInfo(config.errorSimulation.getErrorCode(), config))
            responses.add(ApduBuilder.buildPacket(ZvtProtocolConstants.RESP_COMPLETION))
            return responses
        }

        // 2. Intermediate statuses
        responses.add(IntermediateStatusBuilder.build(ZvtProtocolConstants.IS_INSERT_CARD))
        responses.add(IntermediateStatusBuilder.build(ZvtProtocolConstants.IS_WATCH_PIN_PAD))
        responses.add(IntermediateStatusBuilder.build(ZvtProtocolConstants.IS_PLEASE_WAIT))

        // 3. Build status info
        val amount = ApduParser.extractAmount(apdu.data) ?: 0L
        val trace = state.nextTraceNumber()
        val receipt = state.nextReceiptNumber()
        val turnover = state.nextTurnoverNumber()
        val now = LocalDateTime.now()

        responses.add(StatusInfoBuilder.buildPaymentStatusInfo(amount, trace, receipt, turnover, now, config))

        // Terminal without its own printer: send the card receipt to the
        // ECR as print lines (06 D1) before the completion. The closing line
        // carries the last-line attribute so the ECR knows the slip is done.
        if (config.paymentPrintLines) {
            val amountText = AmountFormatter.format(amount, config.currencyCode)
            responses.add(PrintLineBuilder.build(" "))
            responses.add(PrintLineBuilder.build("Terminal-Beleg"))
            responses.add(PrintLineBuilder.build("Kartenzahlung"))
            responses.add(PrintLineBuilder.build("Karte: %s".format(config.cardData.cardName)))
            responses.add(PrintLineBuilder.build("Betrag     %s".format(amountText)))
            responses.add(PrintLineBuilder.build(now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))))
            responses.add(PrintLineBuilder.build("Beleg-Nr: %d  Trace: %d".format(receipt, trace)))
            responses.add(PrintLineBuilder.buildLastLine("Terminal-ID: %s".format(config.terminalId)))
        }

        // 4. Store transaction
        store.recordTransaction(StoredTransaction(
            type = "Payment",
            amount = amount,
            trace = trace,
            receipt = receipt,
            turnover = turnover,
            timestamp = now,
            cardData = config.cardData
        ))

        // 5. Completion
        responses.add(ApduBuilder.buildPacket(ZvtProtocolConstants.RESP_COMPLETION))

        return responses
    }
}
