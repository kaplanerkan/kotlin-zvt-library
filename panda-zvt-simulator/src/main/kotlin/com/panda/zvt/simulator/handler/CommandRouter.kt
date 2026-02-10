package com.panda.zvt.simulator.handler

import com.panda.zvt.simulator.protocol.ApduBuilder
import com.panda.zvt.simulator.protocol.ApduParser
import com.panda.zvt.simulator.protocol.ZvtProtocolConstants
import com.panda.zvt.simulator.state.SimulatorState
import com.panda.zvt.simulator.state.TransactionStore
import org.slf4j.LoggerFactory

class CommandRouter(
    private val state: SimulatorState,
    private val store: TransactionStore
) {
    private val logger = LoggerFactory.getLogger(CommandRouter::class.java)

    private val handlers: Map<Int, CommandHandler> = mapOf(
        key(0x06, 0x00) to RegistrationHandler(state),
        key(0x06, 0x01) to AuthorizationHandler(state, store),
        key(0x06, 0x02) to LogOffHandler(state),
        key(0x06, 0x20) to RepeatReceiptHandler(state, store),
        key(0x06, 0x22) to PreAuthHandler(state, store),
        key(0x06, 0x24) to BookTotalHandler(state, store),
        key(0x06, 0x25) to PreAuthReversalHandler(state, store),
        key(0x06, 0x30) to ReversalHandler(state, store),
        key(0x06, 0x31) to RefundHandler(state, store),
        key(0x06, 0x50) to EndOfDayHandler(state, store),
        key(0x06, 0x70) to DiagnosisHandler(state),
        key(0x05, 0x01) to StatusEnquiryHandler(state),
        key(0x06, 0xB0) to AbortHandler()
    )

    suspend fun route(apdu: ApduParser.ParsedApdu): List<ByteArray> {
        // ACK from ECR — just consume, no response
        if (ApduParser.isAck(apdu.command)) return emptyList()

        val cmdKey = ZvtProtocolConstants.commandToKey(apdu.command)
        val cmdName = ZvtProtocolConstants.getCommandName(apdu.command)

        val handler = handlers[cmdKey]
        if (handler == null) {
            logger.warn("Unknown command: $cmdName — sending NACK")
            return listOf(ApduBuilder.nack())
        }

        logger.info("Handling: $cmdName")
        return handler.handle(apdu)
    }

    private fun key(cls: Int, instr: Int): Int = (cls shl 8) or instr
}
