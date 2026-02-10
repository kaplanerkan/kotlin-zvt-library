package com.panda.zvt_library.protocol

/**
 * ZVT protocol constants.
 *
 * Contains all command codes, BMP field tags, result codes, intermediate status codes,
 * registration bitmask flags, payment types, currency codes, and default connection
 * settings defined by the ZVT Protocol Specification v13.13.
 *
 * Reference: ZVT Protocol Specification v13.13 (PA00P015_13.13_final)
 *
 * @author Erkan Kaplan
 * @since 2026-02-10
 */
object ZvtConstants {

    // =====================================================
    // ECR -> Terminal Commands
    // =====================================================

    /** Registration command (06 00) */
    val CMD_REGISTRATION = byteArrayOf(0x06, 0x00)

    /** Authorization / Payment command (06 01) */
    val CMD_AUTHORIZATION = byteArrayOf(0x06, 0x01)

    /** Log-off command (06 02) */
    val CMD_LOG_OFF = byteArrayOf(0x06, 0x02)

    /** End of Day command (06 50) */
    val CMD_END_OF_DAY = byteArrayOf(0x06, 0x50.toByte())

    /** Reversal command (06 30) */
    val CMD_REVERSAL = byteArrayOf(0x06, 0x30)

    /** Refund command (06 31) */
    val CMD_REFUND = byteArrayOf(0x06, 0x31)

    /** Diagnosis command (06 70) */
    val CMD_DIAGNOSIS = byteArrayOf(0x06, 0x70)

    /** Abort command (06 1E) */
    val CMD_ABORT = byteArrayOf(0x06, 0x1E)

    /** Print Line command (06 D1) */
    val CMD_PRINT_LINE = byteArrayOf(0x06, 0xD1.toByte())

    /** Status Enquiry command (05 01) */
    val CMD_STATUS_ENQUIRY = byteArrayOf(0x05, 0x01)

    /** Repeat Receipt command (06 20) */
    val CMD_REPEAT_RECEIPT = byteArrayOf(0x06, 0x20)

    /** Pre-Authorization command (06 22) */
    val CMD_PRE_AUTHORIZATION = byteArrayOf(0x06, 0x22)

    /** Book Total / Pre-Auth Completion command (06 24) */
    val CMD_BOOK_TOTAL = byteArrayOf(0x06, 0x24)

    /** Partial Reversal command (06 25) */
    val CMD_PARTIAL_REVERSAL = byteArrayOf(0x06, 0x25)

    // =====================================================
    // Terminal -> ECR Responses
    // =====================================================

    /** Completion (06 0F) */
    val RESP_COMPLETION = byteArrayOf(0x06, 0x0F)

    /** Status Information (04 0F) */
    val RESP_STATUS_INFO = byteArrayOf(0x04, 0x0F)

    /** Intermediate Status (04 FF) */
    val RESP_INTERMEDIATE_STATUS = byteArrayOf(0x04, 0xFF.toByte())

    /** Abort by terminal (06 1E) */
    val RESP_ABORT = byteArrayOf(0x06, 0x1E)

    /** Print Line (06 D1) */
    val RESP_PRINT_LINE = byteArrayOf(0x06, 0xD1.toByte())

    /** Print Text Block (06 D3) */
    val RESP_PRINT_TEXT = byteArrayOf(0x06, 0xD3.toByte())

    // =====================================================
    // Acknowledgement Packets
    // =====================================================

    /** Positive acknowledgement (80 00 00) */
    val ACK = byteArrayOf(0x80.toByte(), 0x00, 0x00)

    /** Negative acknowledgement (84 00 00) */
    val NACK = byteArrayOf(0x84.toByte(), 0x00, 0x00)

    // =====================================================
    // BMP (Bitmap) Field Tags
    // =====================================================

    /** Timeout value in seconds */
    const val BMP_TIMEOUT: Byte = 0x01

    /** Protocol version */
    const val BMP_PROTOCOL_VERSION: Byte = 0x02

    /** Service byte / Config byte (Registration) */
    const val BMP_SERVICE_BYTE: Byte = 0x03

    /** Amount (BCD 6 bytes) */
    const val BMP_AMOUNT: Byte = 0x04

    /** Pump number */
    const val BMP_PUMP_NR: Byte = 0x05

    /** TLV container via BMP 06 */
    const val BMP_TLV_CONTAINER: Byte = 0x06

    /** Trace number (BCD 3 bytes) */
    const val BMP_TRACE_NUMBER: Byte = 0x0B

    /** Time - HHMMSS (BCD 3 bytes) */
    const val BMP_TIME: Byte = 0x0C

    /** Date - MMDD (BCD 2 bytes) */
    const val BMP_DATE: Byte = 0x0D

    /** Card expiry date - YYMM (BCD 2 bytes) */
    const val BMP_EXPIRY_DATE: Byte = 0x0E

    /** Card sequence number (BCD 2 bytes) */
    const val BMP_CARD_SEQUENCE_NR: Byte = 0x17

    /** Payment type */
    const val BMP_PAYMENT_TYPE: Byte = 0x19

    /** PAN / EF_ID (LLVAR BCD) */
    const val BMP_CARD_NUMBER: Byte = 0x22

    /** Result code (1 byte) */
    const val BMP_RESULT_CODE: Byte = 0x27

    /** Terminal ID - TID (BCD 4 bytes) */
    const val BMP_TERMINAL_ID: Byte = 0x29

    /** VU number (15 bytes ASCII, fixed) */
    const val BMP_VU_NUMBER: Byte = 0x2A

    /** Original trace number (BCD 3 bytes, used in reversals) */
    const val BMP_ORIGINAL_TRACE: Byte = 0x37

    /** AID - Application Identifier (8 bytes fixed) */
    const val BMP_AID: Byte = 0x3B

    /** Additional data / TLV container (LLLVAR) */
    const val BMP_ADDITIONAL_DATA: Byte = 0x3C

    /** Currency code (BCD 2 bytes, ISO 4217) */
    const val BMP_CURRENCY_CODE: Byte = 0x49

    /** Blocked goods groups (LLVAR) */
    const val BMP_BLOCKED_GOODS_GROUPS: Byte = 0x4C

    /** Single amounts End-of-Day (LLLVAR) */
    const val BMP_SINGLE_AMOUNTS: Byte = 0x60

    /** Receipt number (BCD 2 bytes) */
    const val BMP_RECEIPT_NR: Byte = 0x87.toByte()

    /** Turnover number (BCD 3 bytes) */
    const val BMP_TURNOVER_NR: Byte = 0x88.toByte()

    /** Card type (1 byte) */
    const val BMP_CARD_TYPE: Byte = 0x8A.toByte()

    /** Card name (LLVAR ASCII null-terminated) */
    const val BMP_CARD_NAME: Byte = 0x8B.toByte()

    /** Card type ID / Network identifier (1 byte) */
    const val BMP_CARD_TYPE_ID: Byte = 0x8C.toByte()

    /** Result code AS (1 byte) */
    const val BMP_RESULT_CODE_AS: Byte = 0xA0.toByte()

    /** AID parameter (5 bytes fixed) */
    const val BMP_AID_PARAMETER: Byte = 0xBA.toByte()

    // =====================================================
    // TLV Tag Definitions
    // =====================================================

    const val TLV_CARD_NAME: Int = 0x1F10
    const val TLV_ADDITIONAL_TEXT: Int = 0x1F07
    const val TLV_RECEIPT_PARAMETER: Int = 0x1F09
    const val TLV_APPLICATION_LABEL: Int = 0x50

    // =====================================================
    // Registration Config Byte Bitmasks (Bit flags)
    // Spec v13.13: Bitmask values, can be OR-combined.
    // =====================================================

    /** Bit 1: ECR prints payment receipts */
    const val REG_ECR_PRINTS_PAYMENT_RECEIPT: Int = 0x02
    /** Bit 2: ECR prints admin receipts */
    const val REG_ECR_PRINTS_ADMIN_RECEIPT: Int = 0x04
    /** Bit 3: ECR requests intermediate status messages */
    const val REG_INTERMEDIATE_STATUS: Byte = 0x08
    /** Bit 4: ECR controls the payment function */
    const val REG_ECR_CONTROLS_PAYMENT: Int = 0x10
    /** Bit 5: ECR controls the admin function */
    const val REG_ECR_CONTROLS_ADMIN: Int = 0x20
    /** Bit 7: Print via Print Line/Text Block (instead of self-compile) */
    const val REG_PRINT_VIA_PRINT_LINE: Int = 0x80

    // =====================================================
    // Payment Types (BMP 0x19)
    // =====================================================

    const val PAY_TYPE_DEFAULT: Byte = 0x00
    const val PAY_TYPE_EC_CASH: Byte = 0x40
    const val PAY_TYPE_ELV: Byte = 0x50.toByte()

    // =====================================================
    // Currency Codes (ISO 4217 - BCD)
    // =====================================================

    const val CURRENCY_EUR: Int = 978
    const val CURRENCY_TRY: Int = 949
    const val CURRENCY_CHF: Int = 756
    const val CURRENCY_USD: Int = 840
    const val CURRENCY_GBP: Int = 826

    // =====================================================
    // Result Codes (BMP 0x27) - ZVT Spec v13.13 pp.200-201
    // =====================================================

    const val RC_SUCCESS: Byte = 0x00
    // 0x01-0x63: Errors from network operator / authorization system
    const val RC_CARD_NOT_READABLE: Byte = 0x64
    const val RC_CARD_DATA_NOT_PRESENT: Byte = 0x65
    const val RC_PROCESSING_ERROR: Byte = 0x66
    const val RC_FUNCTION_NOT_PERMITTED_EC: Byte = 0x67
    const val RC_FUNCTION_NOT_PERMITTED_CREDIT: Byte = 0x68
    const val RC_TURNOVER_FILE_FULL: Byte = 0x6A
    const val RC_FUNCTION_DEACTIVATED: Byte = 0x6B
    const val RC_ABORT_VIA_TIMEOUT: Byte = 0x6C
    const val RC_CARD_IN_BLOCKED_LIST: Byte = 0x6E
    const val RC_WRONG_CURRENCY: Byte = 0x6F
    const val RC_CREDIT_NOT_SUFFICIENT: Byte = 0x71
    const val RC_CHIP_ERROR: Byte = 0x72
    const val RC_CARD_DATA_INCORRECT: Byte = 0x73
    const val RC_DUKPT_EXHAUSTED: Byte = 0x74
    const val RC_TEXT_NOT_AUTHENTIC: Byte = 0x75
    const val RC_PAN_NOT_IN_WHITE_LIST: Byte = 0x76
    const val RC_END_OF_DAY_NOT_POSSIBLE: Byte = 0x77
    const val RC_CARD_EXPIRED: Byte = 0x78
    const val RC_CARD_NOT_YET_VALID: Byte = 0x79
    const val RC_CARD_UNKNOWN: Byte = 0x7A
    const val RC_FALLBACK_NOT_POSSIBLE_GIROCARD: Byte = 0x7B
    const val RC_FALLBACK_NOT_POSSIBLE_OTHER: Byte = 0x7C
    const val RC_COMMUNICATION_ERROR: Byte = 0x7D
    const val RC_FALLBACK_NOT_POSSIBLE_DEBIT: Byte = 0x7E
    const val RC_FUNCTION_NOT_POSSIBLE: Byte = 0x83.toByte()
    const val RC_KEY_MISSING: Byte = 0x85.toByte()
    const val RC_PIN_PAD_DEFECTIVE: Byte = 0x89.toByte()
    const val RC_ZVT_PROTOCOL_ERROR: Byte = 0x9A.toByte()
    const val RC_ERROR_FROM_DIALUP: Byte = 0x9B.toByte()
    const val RC_PLEASE_WAIT: Byte = 0x9C.toByte()
    const val RC_RECEIVER_NOT_READY: Byte = 0xA0.toByte()
    const val RC_REMOTE_STATION_NO_RESPONSE: Byte = 0xA1.toByte()
    const val RC_NO_CONNECTION: Byte = 0xA3.toByte()
    const val RC_SUBMISSION_GELDKARTE_NOT_POSSIBLE: Byte = 0xA4.toByte()
    const val RC_FUNCTION_NOT_ALLOWED_PCI: Byte = 0xA5.toByte()
    const val RC_MEMORY_FULL: Byte = 0xB1.toByte()
    const val RC_MERCHANT_JOURNAL_FULL: Byte = 0xB2.toByte()
    const val RC_ALREADY_REVERSED: Byte = 0xB4.toByte()
    const val RC_REVERSAL_NOT_POSSIBLE: Byte = 0xB5.toByte()
    const val RC_PRE_AUTH_INCORRECT: Byte = 0xB7.toByte()
    const val RC_ERROR_PRE_AUTHORIZATION: Byte = 0xB8.toByte()
    const val RC_VOLTAGE_TOO_LOW: Byte = 0xBF.toByte()
    const val RC_CARD_LOCKING_DEFECTIVE: Byte = 0xC0.toByte()
    const val RC_MERCHANT_CARD_LOCKED: Byte = 0xC1.toByte()
    const val RC_DIAGNOSIS_REQUIRED: Byte = 0xC2.toByte()
    const val RC_MAX_AMOUNT_EXCEEDED: Byte = 0xC3.toByte()
    const val RC_CARD_PROFILE_INVALID: Byte = 0xC4.toByte()
    const val RC_PAYMENT_METHOD_NOT_SUPPORTED: Byte = 0xC5.toByte()
    const val RC_CURRENCY_NOT_APPLICABLE: Byte = 0xC6.toByte()
    const val RC_AMOUNT_TOO_SMALL: Byte = 0xC8.toByte()
    const val RC_MAX_TRANSACTION_AMOUNT_TOO_SMALL: Byte = 0xC9.toByte()
    const val RC_FUNCTION_ONLY_IN_EURO: Byte = 0xCB.toByte()
    const val RC_PRINTER_NOT_READY: Byte = 0xCC.toByte()
    const val RC_CASHBACK_NOT_POSSIBLE: Byte = 0xCD.toByte()
    const val RC_FUNCTION_NOT_PERMITTED_SERVICE: Byte = 0xD2.toByte()
    const val RC_CARD_INSERTED: Byte = 0xDC.toByte()
    const val RC_ERROR_CARD_EJECT: Byte = 0xDD.toByte()
    const val RC_ERROR_CARD_INSERTION: Byte = 0xDE.toByte()
    const val RC_REMOTE_MAINTENANCE_ACTIVATED: Byte = 0xE0.toByte()
    const val RC_CARD_READER_DEFECTIVE: Byte = 0xE2.toByte()
    const val RC_SHUTTER_CLOSED: Byte = 0xE3.toByte()
    const val RC_TERMINAL_ACTIVATION_REQUIRED: Byte = 0xE4.toByte()
    const val RC_GOODS_GROUP_NOT_FOUND: Byte = 0xE7.toByte()
    const val RC_NO_GOODS_GROUPS_TABLE: Byte = 0xE8.toByte()
    const val RC_RESTRICTION_CODE_NOT_PERMITTED: Byte = 0xE9.toByte()
    const val RC_CARD_CODE_NOT_PERMITTED: Byte = 0xEA.toByte()
    const val RC_PIN_ALGO_UNKNOWN: Byte = 0xEB.toByte()
    const val RC_PIN_PROCESSING_NOT_POSSIBLE: Byte = 0xEC.toByte()
    const val RC_PIN_PAD_DEFECTIVE_2: Byte = 0xED.toByte()
    const val RC_OPEN_END_OF_DAY_BATCH: Byte = 0xF0.toByte()
    const val RC_EC_CASH_OFFLINE_ERROR: Byte = 0xF1.toByte()
    const val RC_OPT_ERROR: Byte = 0xF5.toByte()
    const val RC_OPT_DATA_NOT_AVAILABLE: Byte = 0xF6.toByte()
    const val RC_ERROR_OFFLINE_TRANSACTIONS: Byte = 0xFA.toByte()
    const val RC_TURNOVER_DATA_DEFECTIVE: Byte = 0xFB.toByte()
    const val RC_DEVICE_NOT_PRESENT: Byte = 0xFC.toByte()
    const val RC_BAUDRATE_NOT_SUPPORTED: Byte = 0xFD.toByte()
    const val RC_REGISTER_UNKNOWN: Byte = 0xFE.toByte()
    const val RC_SYSTEM_ERROR: Byte = 0xFF.toByte()

    // =====================================================
    // Intermediate Status Codes (04 FF) - ZVT Spec v13.13
    // =====================================================

    const val IS_WAIT_FOR_AMOUNT_CONFIRMATION: Byte = 0x00
    const val IS_WATCH_PIN_PAD_1: Byte = 0x01
    const val IS_WATCH_PIN_PAD_2: Byte = 0x02
    const val IS_NOT_ACCEPTED: Byte = 0x03
    const val IS_WAITING_FOR_FEP: Byte = 0x04
    const val IS_SENDING_AUTO_REVERSAL: Byte = 0x05
    const val IS_SENDING_POST_BOOKINGS: Byte = 0x06
    const val IS_CARD_NOT_ADMITTED: Byte = 0x07
    const val IS_CARD_UNKNOWN: Byte = 0x08
    const val IS_EXPIRED_CARD: Byte = 0x09
    const val IS_INSERT_CARD: Byte = 0x0A
    const val IS_PLEASE_REMOVE_CARD: Byte = 0x0B
    const val IS_CARD_NOT_READABLE: Byte = 0x0C
    const val IS_PROCESSING_ERROR: Byte = 0x0D
    const val IS_PLEASE_WAIT: Byte = 0x0E
    const val IS_AUTOMATIC_END_OF_DAY: Byte = 0x0F
    const val IS_INVALID_CARD: Byte = 0x10
    const val IS_CONFIRM_AMOUNT_PINPAD: Byte = 0x11
    const val IS_CONFIRM_AMOUNT_ECR: Byte = 0x12
    const val IS_NO_PAPER: Byte = 0x13
    const val IS_CREDIT_NOT_SUFFICIENT: Byte = 0x14
    const val IS_INCORRECT_PIN: Byte = 0x15
    const val IS_CARD_WRONG_DIRECTION: Byte = 0x16
    const val IS_PLEASE_WAIT_2: Byte = 0x17
    const val IS_PIN_TRY_LIMIT_EXCEEDED: Byte = 0x18
    const val IS_CHIP_ERROR: Byte = 0x19
    const val IS_MERCHANT_JOURNAL_FULL: Byte = 0x1A
    const val IS_OPERATION_NOT_POSSIBLE: Byte = 0x1B
    const val IS_APPROVED_TAKE_GOODS: Byte = 0x1C
    const val IS_DECLINED: Byte = 0x1D
    const val IS_CONTACTLESS_FINISHED: Byte = 0x5E
    const val IS_NO_MATCHING_ZVT_CODE: Byte = 0xFF.toByte()

    // =====================================================
    // Connection Default Settings
    // =====================================================

    const val DEFAULT_PORT: Int = 20007
    const val DEFAULT_TIMEOUT_MS: Int = 90_000
    const val CONNECT_TIMEOUT_MS: Int = 10_000
    const val ACK_TIMEOUT_MS: Int = 5_000
    const val READ_BUFFER_SIZE: Int = 4096
    const val MAX_PACKET_SIZE: Int = 65535

    /**
     * Converts a result code (BMP 0x27) to a human-readable message.
     *
     * Based on ZVT Spec v13.13 pp.200-201.
     *
     * @param code The result code byte.
     * @return Human-readable error/success description.
     */
    fun getResultMessage(code: Byte): String = when (code) {
        RC_SUCCESS -> "Successful"
        RC_CARD_NOT_READABLE -> "Card not readable (LRC/parity error)"
        RC_CARD_DATA_NOT_PRESENT -> "Card data not present"
        RC_PROCESSING_ERROR -> "Processing error"
        RC_FUNCTION_NOT_PERMITTED_EC -> "Function not permitted for ec/Maestro"
        RC_FUNCTION_NOT_PERMITTED_CREDIT -> "Function not permitted for credit/tank cards"
        RC_TURNOVER_FILE_FULL -> "Turnover file full"
        RC_FUNCTION_DEACTIVATED -> "Function deactivated (PT not registered)"
        RC_ABORT_VIA_TIMEOUT -> "Abort via timeout or abort key"
        RC_CARD_IN_BLOCKED_LIST -> "Card in blocked list"
        RC_WRONG_CURRENCY -> "Wrong currency"
        RC_CREDIT_NOT_SUFFICIENT -> "Credit not sufficient (chip)"
        RC_CHIP_ERROR -> "Chip error"
        RC_CARD_DATA_INCORRECT -> "Card data incorrect"
        RC_DUKPT_EXHAUSTED -> "DUKPT engine exhausted"
        RC_TEXT_NOT_AUTHENTIC -> "Text not authentic"
        RC_PAN_NOT_IN_WHITE_LIST -> "PAN not in white list"
        RC_END_OF_DAY_NOT_POSSIBLE -> "End-of-day batch not possible"
        RC_CARD_EXPIRED -> "Card expired"
        RC_CARD_NOT_YET_VALID -> "Card not yet valid"
        RC_CARD_UNKNOWN -> "Card unknown"
        RC_FALLBACK_NOT_POSSIBLE_GIROCARD -> "Fallback to mag stripe not possible (girocard)"
        RC_FALLBACK_NOT_POSSIBLE_OTHER -> "Fallback to mag stripe not possible"
        RC_COMMUNICATION_ERROR -> "Communication error"
        RC_FALLBACK_NOT_POSSIBLE_DEBIT -> "Fallback not possible, debit advice possible"
        RC_FUNCTION_NOT_POSSIBLE -> "Function not possible"
        RC_KEY_MISSING -> "Key missing"
        RC_PIN_PAD_DEFECTIVE -> "PIN-pad defective"
        RC_ZVT_PROTOCOL_ERROR -> "ZVT protocol error"
        RC_ERROR_FROM_DIALUP -> "Error from dial-up/communication"
        RC_PLEASE_WAIT -> "Please wait"
        RC_RECEIVER_NOT_READY -> "Receiver not ready"
        RC_REMOTE_STATION_NO_RESPONSE -> "Remote station does not respond"
        RC_NO_CONNECTION -> "No connection"
        RC_SUBMISSION_GELDKARTE_NOT_POSSIBLE -> "Submission of Geldkarte not possible"
        RC_FUNCTION_NOT_ALLOWED_PCI -> "Function not allowed (PCI-DSS/P2PE)"
        RC_MEMORY_FULL -> "Memory full"
        RC_MERCHANT_JOURNAL_FULL -> "Merchant journal full"
        RC_ALREADY_REVERSED -> "Already reversed"
        RC_REVERSAL_NOT_POSSIBLE -> "Reversal not possible"
        RC_PRE_AUTH_INCORRECT -> "Pre-auth incorrect (amount too high)"
        RC_ERROR_PRE_AUTHORIZATION -> "Error pre-authorization"
        RC_VOLTAGE_TOO_LOW -> "Voltage supply too low"
        RC_CARD_LOCKING_DEFECTIVE -> "Card locking mechanism defective"
        RC_MERCHANT_CARD_LOCKED -> "Merchant card locked"
        RC_DIAGNOSIS_REQUIRED -> "Diagnosis required"
        RC_MAX_AMOUNT_EXCEEDED -> "Maximum amount exceeded"
        RC_CARD_PROFILE_INVALID -> "Card profile invalid"
        RC_PAYMENT_METHOD_NOT_SUPPORTED -> "Payment method not supported"
        RC_CURRENCY_NOT_APPLICABLE -> "Currency not applicable"
        RC_AMOUNT_TOO_SMALL -> "Amount too small"
        RC_MAX_TRANSACTION_AMOUNT_TOO_SMALL -> "Max transaction amount too small"
        RC_FUNCTION_ONLY_IN_EURO -> "Function only allowed in EUR"
        RC_PRINTER_NOT_READY -> "Printer not ready"
        RC_CASHBACK_NOT_POSSIBLE -> "Cashback not possible"
        RC_FUNCTION_NOT_PERMITTED_SERVICE -> "Function not permitted for service cards"
        RC_CARD_INSERTED -> "Card inserted"
        RC_ERROR_CARD_EJECT -> "Error during card eject"
        RC_ERROR_CARD_INSERTION -> "Error during card insertion"
        RC_REMOTE_MAINTENANCE_ACTIVATED -> "Remote maintenance activated"
        RC_CARD_READER_DEFECTIVE -> "Card reader does not answer / defective"
        RC_SHUTTER_CLOSED -> "Shutter closed"
        RC_TERMINAL_ACTIVATION_REQUIRED -> "Terminal activation required"
        RC_GOODS_GROUP_NOT_FOUND -> "Min one goods group not found"
        RC_NO_GOODS_GROUPS_TABLE -> "No goods groups table loaded"
        RC_RESTRICTION_CODE_NOT_PERMITTED -> "Restriction code not permitted"
        RC_CARD_CODE_NOT_PERMITTED -> "Card code not permitted"
        RC_PIN_ALGO_UNKNOWN -> "Function not executable (PIN algo unknown)"
        RC_PIN_PROCESSING_NOT_POSSIBLE -> "PIN processing not possible"
        RC_PIN_PAD_DEFECTIVE_2 -> "PIN-pad defective"
        RC_OPEN_END_OF_DAY_BATCH -> "Open end-of-day batch present"
        RC_EC_CASH_OFFLINE_ERROR -> "ec-cash/Maestro offline error"
        RC_OPT_ERROR -> "OPT error"
        RC_OPT_DATA_NOT_AVAILABLE -> "OPT data not available"
        RC_ERROR_OFFLINE_TRANSACTIONS -> "Error transmitting offline transactions"
        RC_TURNOVER_DATA_DEFECTIVE -> "Turnover data set defective"
        RC_DEVICE_NOT_PRESENT -> "Necessary device not present/defective"
        RC_BAUDRATE_NOT_SUPPORTED -> "Baudrate not supported"
        RC_REGISTER_UNKNOWN -> "Register unknown"
        RC_SYSTEM_ERROR -> "System error (see TLV tags 1F16, 1F17)"
        else -> {
            val unsigned = code.toInt() and 0xFF
            if (unsigned in 0x01..0x63) {
                "Network/authorization error (0x${String.format("%02X", unsigned)})"
            } else {
                "Unknown error (0x${String.format("%02X", unsigned)})"
            }
        }
    }

    /**
     * Converts an intermediate status code to a human-readable message.
     *
     * Based on ZVT Spec v13.13.
     *
     * @param code The intermediate status code byte.
     * @return Human-readable status description.
     */
    fun getIntermediateStatusMessage(code: Byte): String = when (code) {
        IS_WAIT_FOR_AMOUNT_CONFIRMATION -> "Waiting for amount confirmation"
        IS_WATCH_PIN_PAD_1 -> "Please watch PIN-pad"
        IS_WATCH_PIN_PAD_2 -> "Please watch PIN-pad"
        IS_NOT_ACCEPTED -> "Not accepted"
        IS_WAITING_FOR_FEP -> "Waiting for FEP response"
        IS_SENDING_AUTO_REVERSAL -> "Sending auto-reversal"
        IS_SENDING_POST_BOOKINGS -> "Sending post-bookings"
        IS_CARD_NOT_ADMITTED -> "Card not admitted"
        IS_CARD_UNKNOWN -> "Card unknown / undefined"
        IS_EXPIRED_CARD -> "Expired card"
        IS_INSERT_CARD -> "Insert card"
        IS_PLEASE_REMOVE_CARD -> "Please remove card"
        IS_CARD_NOT_READABLE -> "Card not readable"
        IS_PROCESSING_ERROR -> "Processing error"
        IS_PLEASE_WAIT -> "Please wait"
        IS_AUTOMATIC_END_OF_DAY -> "Automatic end-of-day"
        IS_INVALID_CARD -> "Invalid card"
        IS_CONFIRM_AMOUNT_PINPAD -> "Please confirm amount on PIN-pad"
        IS_CONFIRM_AMOUNT_ECR -> "Please confirm amount on ECR"
        IS_NO_PAPER -> "Not enough paper in printer"
        IS_CREDIT_NOT_SUFFICIENT -> "Credit not sufficient"
        IS_INCORRECT_PIN -> "Incorrect PIN"
        IS_CARD_WRONG_DIRECTION -> "Card inserted in wrong direction"
        IS_PLEASE_WAIT_2 -> "Please wait"
        IS_PIN_TRY_LIMIT_EXCEEDED -> "PIN try limit exceeded"
        IS_CHIP_ERROR -> "Chip error"
        IS_MERCHANT_JOURNAL_FULL -> "Merchant journal full"
        IS_OPERATION_NOT_POSSIBLE -> "Operation not possible"
        IS_APPROVED_TAKE_GOODS -> "Approved, please take goods"
        IS_DECLINED -> "Declined"
        IS_CONTACTLESS_FINISHED -> "Contactless card access finished"
        IS_NO_MATCHING_ZVT_CODE -> "No matching ZVT status code (check TLV tags 24, 07)"
        else -> "Status: 0x${String.format("%02X", code.toInt() and 0xFF)}"
    }

    /**
     * Returns a human-readable name for a ZVT command code.
     *
     * Used for logging purposes to identify packet types in the protocol trace.
     *
     * @param command The 2-byte command code.
     * @return Command name string, e.g. "Registration (06 00)".
     */
    fun getCommandName(command: ByteArray): String {
        if (command.size < 2) return "Unknown"
        val hex = String.format("%02X %02X", command[0], command[1])
        return when {
            command.contentEquals(CMD_REGISTRATION) -> "Registration ($hex)"
            command.contentEquals(CMD_AUTHORIZATION) -> "Authorization ($hex)"
            command.contentEquals(CMD_LOG_OFF) -> "Log Off ($hex)"
            command.contentEquals(CMD_END_OF_DAY) -> "End of Day ($hex)"
            command.contentEquals(CMD_REVERSAL) -> "Reversal ($hex)"
            command.contentEquals(CMD_REFUND) -> "Refund ($hex)"
            command.contentEquals(CMD_DIAGNOSIS) -> "Diagnosis ($hex)"
            command.contentEquals(CMD_ABORT) -> "Abort ($hex)"
            command.contentEquals(CMD_PRINT_LINE) -> "Print Line ($hex)"
            command.contentEquals(CMD_STATUS_ENQUIRY) -> "Status Enquiry ($hex)"
            command.contentEquals(CMD_REPEAT_RECEIPT) -> "Repeat Receipt ($hex)"
            command.contentEquals(CMD_PRE_AUTHORIZATION) -> "Pre-Authorization ($hex)"
            command.contentEquals(CMD_BOOK_TOTAL) -> "Book Total ($hex)"
            command.contentEquals(CMD_PARTIAL_REVERSAL) -> "Partial Reversal ($hex)"
            command.contentEquals(RESP_COMPLETION) -> "Completion ($hex)"
            command.contentEquals(RESP_STATUS_INFO) -> "Status Info ($hex)"
            command.contentEquals(RESP_INTERMEDIATE_STATUS) -> "Intermediate Status ($hex)"
            command.contentEquals(RESP_ABORT) -> "Abort ($hex)"
            command.contentEquals(RESP_PRINT_LINE) -> "Print Line ($hex)"
            command.contentEquals(RESP_PRINT_TEXT) -> "Print Text Block ($hex)"
            command[0] == 0x80.toByte() -> "ACK ($hex)"
            command[0] == 0x84.toByte() -> "NACK ($hex)"
            else -> "Unknown ($hex)"
        }
    }
}
