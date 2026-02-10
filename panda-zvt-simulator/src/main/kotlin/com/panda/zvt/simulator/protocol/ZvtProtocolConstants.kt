package com.panda.zvt.simulator.protocol

object ZvtProtocolConstants {

    // ECR -> Terminal Commands
    val CMD_REGISTRATION = byteArrayOf(0x06, 0x00)
    val CMD_AUTHORIZATION = byteArrayOf(0x06, 0x01)
    val CMD_LOG_OFF = byteArrayOf(0x06, 0x02)
    val CMD_REPEAT_RECEIPT = byteArrayOf(0x06, 0x20)
    val CMD_PRE_AUTHORIZATION = byteArrayOf(0x06, 0x22)
    val CMD_BOOK_TOTAL = byteArrayOf(0x06, 0x24)
    val CMD_PRE_AUTH_REVERSAL = byteArrayOf(0x06, 0x25)
    val CMD_REVERSAL = byteArrayOf(0x06, 0x30)
    val CMD_REFUND = byteArrayOf(0x06, 0x31)
    val CMD_END_OF_DAY = byteArrayOf(0x06, 0x50.toByte())
    val CMD_DIAGNOSIS = byteArrayOf(0x06, 0x70)
    val CMD_ABORT = byteArrayOf(0x06, 0xB0.toByte())
    val CMD_STATUS_ENQUIRY = byteArrayOf(0x05, 0x01)

    // Terminal -> ECR Responses
    val RESP_COMPLETION = byteArrayOf(0x06, 0x0F)
    val RESP_STATUS_INFO = byteArrayOf(0x04, 0x0F)
    val RESP_INTERMEDIATE_STATUS = byteArrayOf(0x04, 0xFF.toByte())
    val RESP_ABORT = byteArrayOf(0x06, 0x1E)
    val RESP_PRINT_LINE = byteArrayOf(0x06, 0xD1.toByte())

    // ACK / NACK
    val ACK = byteArrayOf(0x80.toByte(), 0x00, 0x00)
    val NACK = byteArrayOf(0x84.toByte(), 0x00, 0x00)

    // BMP Field Tags
    const val BMP_TIMEOUT: Byte = 0x01
    const val BMP_SERVICE_BYTE: Byte = 0x03
    const val BMP_AMOUNT: Byte = 0x04
    const val BMP_TLV_CONTAINER: Byte = 0x06
    const val BMP_TRACE_NUMBER: Byte = 0x0B
    const val BMP_TIME: Byte = 0x0C
    const val BMP_DATE: Byte = 0x0D
    const val BMP_EXPIRY_DATE: Byte = 0x0E
    const val BMP_CARD_SEQUENCE_NR: Byte = 0x17
    const val BMP_PAYMENT_TYPE: Byte = 0x19
    const val BMP_CARD_NUMBER: Byte = 0x22
    const val BMP_RESULT_CODE: Byte = 0x27
    const val BMP_TERMINAL_ID: Byte = 0x29
    const val BMP_VU_NUMBER: Byte = 0x2A
    const val BMP_ORIGINAL_TRACE: Byte = 0x37
    const val BMP_AID: Byte = 0x3B
    const val BMP_CURRENCY_CODE: Byte = 0x49
    const val BMP_SINGLE_AMOUNTS: Byte = 0x60
    const val BMP_RECEIPT_NR: Byte = 0x87.toByte()
    const val BMP_TURNOVER_NR: Byte = 0x88.toByte()
    const val BMP_CARD_TYPE: Byte = 0x8A.toByte()
    const val BMP_CARD_NAME: Byte = 0x8B.toByte()
    const val BMP_CARD_TYPE_ID: Byte = 0x8C.toByte()

    // Result Codes
    const val RC_SUCCESS: Byte = 0x00
    const val RC_CARD_NOT_READABLE: Byte = 0x64
    const val RC_PROCESSING_ERROR: Byte = 0x66
    const val RC_ABORT_VIA_TIMEOUT: Byte = 0x6C
    const val RC_CARD_EXPIRED: Byte = 0x78
    const val RC_REVERSAL_NOT_POSSIBLE: Byte = 0xB5.toByte()
    const val RC_SYSTEM_ERROR: Byte = 0xFF.toByte()

    // Intermediate Status Codes
    const val IS_WATCH_PIN_PAD: Byte = 0x01
    const val IS_INSERT_CARD: Byte = 0x0A
    const val IS_PLEASE_REMOVE_CARD: Byte = 0x0B
    const val IS_PLEASE_WAIT: Byte = 0x0E
    const val IS_APPROVED_TAKE_GOODS: Byte = 0x1C
    const val IS_DECLINED: Byte = 0x1D

    fun commandToKey(cmd: ByteArray): Int {
        if (cmd.size < 2) return 0
        return ((cmd[0].toInt() and 0xFF) shl 8) or (cmd[1].toInt() and 0xFF)
    }

    fun getCommandName(command: ByteArray): String {
        if (command.size < 2) return "Unknown"
        val hex = "%02X %02X".format(command[0], command[1])
        return when {
            command.contentEquals(CMD_REGISTRATION) -> "Registration ($hex)"
            command.contentEquals(CMD_AUTHORIZATION) -> "Authorization ($hex)"
            command.contentEquals(CMD_LOG_OFF) -> "Log Off ($hex)"
            command.contentEquals(CMD_REPEAT_RECEIPT) -> "Repeat Receipt ($hex)"
            command.contentEquals(CMD_PRE_AUTHORIZATION) -> "Pre-Authorization ($hex)"
            command.contentEquals(CMD_BOOK_TOTAL) -> "Book Total ($hex)"
            command.contentEquals(CMD_PRE_AUTH_REVERSAL) -> "Pre-Auth Reversal ($hex)"
            command.contentEquals(CMD_REVERSAL) -> "Reversal ($hex)"
            command.contentEquals(CMD_REFUND) -> "Refund ($hex)"
            command.contentEquals(CMD_END_OF_DAY) -> "End of Day ($hex)"
            command.contentEquals(CMD_DIAGNOSIS) -> "Diagnosis ($hex)"
            command.contentEquals(CMD_ABORT) -> "Abort ($hex)"
            command.contentEquals(CMD_STATUS_ENQUIRY) -> "Status Enquiry ($hex)"
            command[0] == 0x80.toByte() -> "ACK"
            command[0] == 0x84.toByte() -> "NACK"
            else -> "Unknown ($hex)"
        }
    }
}
