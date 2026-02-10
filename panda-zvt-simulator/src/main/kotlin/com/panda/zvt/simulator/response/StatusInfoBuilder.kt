package com.panda.zvt.simulator.response

import com.panda.zvt.simulator.config.SimulatorConfig
import com.panda.zvt.simulator.protocol.ApduBuilder
import com.panda.zvt.simulator.protocol.BmpFieldBuilder
import com.panda.zvt.simulator.protocol.ZvtProtocolConstants
import com.panda.zvt.simulator.state.TransactionStore
import java.time.LocalDateTime

object StatusInfoBuilder {

    fun buildPaymentStatusInfo(
        amount: Long,
        trace: Int,
        receipt: Int,
        turnover: Int,
        dateTime: LocalDateTime,
        config: SimulatorConfig
    ): ByteArray {
        val data = buildList {
            addAll(BmpFieldBuilder.resultCode(ZvtProtocolConstants.RC_SUCCESS).toList())
            addAll(BmpFieldBuilder.amount(amount).toList())
            addAll(BmpFieldBuilder.traceNumber(trace).toList())
            addAll(BmpFieldBuilder.time(dateTime.hour, dateTime.minute, dateTime.second).toList())
            addAll(BmpFieldBuilder.date(dateTime.monthValue, dateTime.dayOfMonth).toList())
            addAll(BmpFieldBuilder.expiryDate(config.cardData.expiryDate).toList())
            addAll(BmpFieldBuilder.cardSequenceNr(config.cardData.sequenceNumber).toList())
            addAll(BmpFieldBuilder.terminalId(config.terminalId).toList())
            addAll(BmpFieldBuilder.vuNumber(config.vuNumber).toList())
            addAll(BmpFieldBuilder.aid(config.cardData.aid).toList())
            addAll(BmpFieldBuilder.currencyCode(config.currencyCode).toList())
            addAll(BmpFieldBuilder.receiptNumber(receipt).toList())
            addAll(BmpFieldBuilder.turnoverNumber(turnover).toList())
            addAll(BmpFieldBuilder.cardType(config.cardData.cardType.toByte()).toList())
            addAll(BmpFieldBuilder.cardName(config.cardData.cardName).toList())
            addAll(BmpFieldBuilder.pan(config.cardData.pan).toList())
        }.toByteArray()

        return ApduBuilder.buildPacket(ZvtProtocolConstants.RESP_STATUS_INFO, data)
    }

    fun buildRegistrationStatusInfo(config: SimulatorConfig): ByteArray {
        val data = buildList {
            addAll(BmpFieldBuilder.resultCode(ZvtProtocolConstants.RC_SUCCESS).toList())
            addAll(BmpFieldBuilder.terminalId(config.terminalId).toList())
            addAll(BmpFieldBuilder.currencyCode(config.currencyCode).toList())
        }.toByteArray()

        return ApduBuilder.buildPacket(ZvtProtocolConstants.RESP_STATUS_INFO, data)
    }

    fun buildDiagnosisStatusInfo(config: SimulatorConfig): ByteArray {
        val data = buildList {
            addAll(BmpFieldBuilder.resultCode(ZvtProtocolConstants.RC_SUCCESS).toList())
            addAll(BmpFieldBuilder.terminalId(config.terminalId).toList())
        }.toByteArray()

        return ApduBuilder.buildPacket(ZvtProtocolConstants.RESP_STATUS_INFO, data)
    }

    fun buildStatusEnquiryInfo(config: SimulatorConfig): ByteArray {
        val data = buildList {
            addAll(BmpFieldBuilder.resultCode(ZvtProtocolConstants.RC_SUCCESS).toList())
            addAll(BmpFieldBuilder.terminalId(config.terminalId).toList())
        }.toByteArray()

        return ApduBuilder.buildPacket(ZvtProtocolConstants.RESP_STATUS_INFO, data)
    }

    fun buildEndOfDayStatusInfo(store: TransactionStore, config: SimulatorConfig): ByteArray {
        val totalAmount = store.getTotalAmount()
        val now = LocalDateTime.now()
        val data = buildList {
            addAll(BmpFieldBuilder.resultCode(ZvtProtocolConstants.RC_SUCCESS).toList())
            if (totalAmount > 0) addAll(BmpFieldBuilder.amount(totalAmount).toList())
            addAll(BmpFieldBuilder.time(now.hour, now.minute, now.second).toList())
            addAll(BmpFieldBuilder.date(now.monthValue, now.dayOfMonth).toList())
            addAll(BmpFieldBuilder.terminalId(config.terminalId).toList())
        }.toByteArray()

        return ApduBuilder.buildPacket(ZvtProtocolConstants.RESP_STATUS_INFO, data)
    }

    fun buildErrorStatusInfo(errorCode: Byte, config: SimulatorConfig): ByteArray {
        val data = buildList {
            addAll(BmpFieldBuilder.resultCode(errorCode).toList())
            addAll(BmpFieldBuilder.terminalId(config.terminalId).toList())
        }.toByteArray()

        return ApduBuilder.buildPacket(ZvtProtocolConstants.RESP_STATUS_INFO, data)
    }

    fun buildRepeatReceiptStatusInfo(store: TransactionStore, config: SimulatorConfig): ByteArray {
        val lastTxn = store.getLastTransaction()
        if (lastTxn == null) {
            return buildErrorStatusInfo(0x6D.toByte(), config) // No transaction
        }
        return buildPaymentStatusInfo(
            amount = lastTxn.amount,
            trace = lastTxn.trace,
            receipt = lastTxn.receipt,
            turnover = lastTxn.turnover,
            dateTime = lastTxn.timestamp,
            config = config
        )
    }
}
