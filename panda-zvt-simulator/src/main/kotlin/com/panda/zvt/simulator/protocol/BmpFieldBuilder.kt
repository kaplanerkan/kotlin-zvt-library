package com.panda.zvt.simulator.protocol

object BmpFieldBuilder {

    fun resultCode(code: Byte): ByteArray =
        byteArrayOf(ZvtProtocolConstants.BMP_RESULT_CODE, code)

    fun amount(cents: Long): ByteArray =
        byteArrayOf(ZvtProtocolConstants.BMP_AMOUNT) + BcdEncoder.amountToBcd(cents)

    fun traceNumber(trace: Int): ByteArray =
        byteArrayOf(ZvtProtocolConstants.BMP_TRACE_NUMBER) + BcdEncoder.traceNumberToBcd(trace)

    fun originalTrace(trace: Int): ByteArray =
        byteArrayOf(ZvtProtocolConstants.BMP_ORIGINAL_TRACE) + BcdEncoder.traceNumberToBcd(trace)

    fun time(hour: Int, minute: Int, second: Int): ByteArray =
        byteArrayOf(ZvtProtocolConstants.BMP_TIME) + BcdEncoder.timeToBcd(hour, minute, second)

    fun date(month: Int, day: Int): ByteArray =
        byteArrayOf(ZvtProtocolConstants.BMP_DATE) + BcdEncoder.dateToBcd(month, day)

    fun expiryDate(yymm: String): ByteArray =
        byteArrayOf(ZvtProtocolConstants.BMP_EXPIRY_DATE) + BcdEncoder.expiryDateToBcd(yymm)

    fun cardSequenceNr(nr: Int): ByteArray =
        byteArrayOf(ZvtProtocolConstants.BMP_CARD_SEQUENCE_NR) + BcdEncoder.cardSequenceNrToBcd(nr)

    fun terminalId(tid: String): ByteArray =
        byteArrayOf(ZvtProtocolConstants.BMP_TERMINAL_ID) + BcdEncoder.terminalIdToBcd(tid)

    fun vuNumber(vu: String): ByteArray {
        val padded = vu.padEnd(15, ' ').take(15)
        return byteArrayOf(ZvtProtocolConstants.BMP_VU_NUMBER) + padded.toByteArray(Charsets.US_ASCII)
    }

    fun currencyCode(code: Int): ByteArray =
        byteArrayOf(ZvtProtocolConstants.BMP_CURRENCY_CODE) + BcdEncoder.currencyToBcd(code)

    fun receiptNumber(nr: Int): ByteArray =
        byteArrayOf(ZvtProtocolConstants.BMP_RECEIPT_NR) + BcdEncoder.receiptNumberToBcd(nr)

    fun turnoverNumber(nr: Int): ByteArray =
        byteArrayOf(ZvtProtocolConstants.BMP_TURNOVER_NR) + BcdEncoder.turnoverNumberToBcd(nr)

    fun cardType(type: Byte): ByteArray =
        byteArrayOf(ZvtProtocolConstants.BMP_CARD_TYPE, type)

    /**
     * PAN as LLVAR BCD with FxFy length prefix and E-nibble masking.
     */
    fun pan(panStr: String): ByteArray {
        val bcdData = BcdEncoder.panToBcd(panStr)
        val llvarLen = LlvarEncoder.encodeLlvar(bcdData.size)
        return byteArrayOf(ZvtProtocolConstants.BMP_CARD_NUMBER) + llvarLen + bcdData
    }

    /**
     * Card name as LLVAR ASCII with FxFy length prefix, null-terminated.
     */
    fun cardName(name: String): ByteArray {
        val nameBytes = name.toByteArray(Charsets.US_ASCII) + byteArrayOf(0x00)
        val llvarLen = LlvarEncoder.encodeLlvar(nameBytes.size)
        return byteArrayOf(ZvtProtocolConstants.BMP_CARD_NAME) + llvarLen + nameBytes
    }

    /**
     * AID - exactly 8 bytes fixed, hex string input (16 hex chars).
     */
    fun aid(aidHex: String): ByteArray {
        val hex = aidHex.replace(" ", "").padEnd(16, '0').take(16)
        val aidBytes = ByteArray(8) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return byteArrayOf(ZvtProtocolConstants.BMP_AID) + aidBytes
    }

    fun paymentType(type: Byte): ByteArray =
        byteArrayOf(ZvtProtocolConstants.BMP_PAYMENT_TYPE, type)
}
