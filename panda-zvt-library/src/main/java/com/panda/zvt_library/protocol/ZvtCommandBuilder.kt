package com.panda.zvt_library.protocol

import com.panda.zvt_library.util.BcdHelper
import com.panda.zvt_library.util.TlvParser
import com.panda.zvt_library.util.toHexString
import timber.log.Timber

/**
 * ZVT command builder for ECR -> Terminal direction.
 *
 * Constructs APDU command packets for all supported ZVT operations.
 * Each method returns a [ZvtPacket] ready to be sent to the payment terminal.
 *
 * Supported commands:
 * - Registration (06 00)
 * - Authorization (06 01)
 * - Log Off (06 02)
 * - Repeat Receipt (06 20)
 * - Pre-Authorization (06 22)
 * - Book Total (06 24)
 * - Partial Reversal (06 25)
 * - Reversal (06 30)
 * - Refund (06 31)
 * - End of Day (06 50)
 * - Diagnosis (06 70)
 * - Abort (06 1E)
 * - Status Enquiry (05 01)
 *
 * Reference: ZVT Protocol Specification v13.13
 *
 * @author Erkan Kaplan
 * @since 2026-02-10
 */
object ZvtCommandBuilder {

    private const val TAG = "ZVT"

    // =====================================================
    // Registration (06 00)
    // =====================================================

    /** TLV tag 26: List of permitted ZVT commands (strongly recommended) */
    private const val TLV_TAG_PERMITTED_COMMANDS = 0x26

    /**
     * Builds a Registration command (06 00).
     *
     * This command must be sent as the first communication with the terminal.
     * It registers the ECR with the terminal and configures the session.
     *
     * The command includes a TLV container (BMP 06) with the list of permitted
     * PT->ECR commands (tag 26), as strongly recommended by the ZVT spec.
     *
     * @param password Terminal password (usually "000000", encoded as 3-byte BCD).
     * @param configByte Configuration bitmask (controls receipt printing, intermediate status, etc.).
     * @param currencyCode ISO 4217 currency code (default EUR: 978).
     * @return [ZvtPacket] containing the Registration command.
     */
    fun buildRegistration(
        password: String = "000000",
        configByte: Byte = ZvtConstants.REG_INTERMEDIATE_STATUS,
        currencyCode: Int = ZvtConstants.CURRENCY_EUR,
        serviceByteValue: Byte = 0x00
    ): ZvtPacket {
        val data = mutableListOf<Byte>()

        // Password (3 byte BCD)
        data.addAll(BcdHelper.stringToBcd(password.padStart(6, '0')).toList())

        // Config byte
        data.add(configByte)

        // Currency code CC (2 byte BCD) — positional field, NO BMP tag!
        // In Registration (06 00), CC is placed directly after config byte
        // without a 0x49 prefix. This is different from other commands where
        // CC uses BMP tag 0x49.
        data.addAll(BcdHelper.currencyToBcd(currencyCode).toList())

        // Service byte (BMP 0x03)
        data.add(ZvtConstants.BMP_SERVICE_BYTE)
        data.add(serviceByteValue)

        val packet = ZvtPacket(
            command = ZvtConstants.CMD_REGISTRATION,
            data = data.toByteArray()
        )

        Timber.tag(TAG).d("[CommandBuilder] Built Registration: password=%s, configByte=0x%02X, currency=%d, data=%s",
            password, configByte, currencyCode, data.toByteArray().toHexString())

        return packet
    }

    /**
     * Builds the list of permitted PT->ECR commands for tag 26.
     *
     * Each command is encoded as 2 bytes: [CLASS][INSTR].
     * This list tells the terminal which response types the ECR supports.
     *
     * @return Byte array containing the permitted command codes.
     */
    private fun buildPermittedCommandsList(): ByteArray {
        val commands = mutableListOf<Byte>()
        // Status Info (04 0F)
        commands.add(0x04); commands.add(0x0F)
        // Intermediate Status (04 FF)
        commands.add(0x04); commands.add(0xFF.toByte())
        // Completion (06 0F)
        commands.add(0x06); commands.add(0x0F)
        // Abort (06 1E)
        commands.add(0x06); commands.add(0x1E)
        // Print Line (06 D1)
        commands.add(0x06); commands.add(0xD1.toByte())
        // Print Text-Block (06 D3)
        commands.add(0x06); commands.add(0xD3.toByte())
        return commands.toByteArray()
    }

    // =====================================================
    // Authorization / Payment (06 01)
    // =====================================================

    /**
     * Builds an Authorization command (06 01) for a payment transaction.
     *
     * @param amountInCents Amount in cents, e.g. 1250 = 12.50 EUR.
     * @param paymentType Payment type byte (default: automatic detection).
     * @param currencyCode ISO 4217 currency code.
     * @return [ZvtPacket] containing the Authorization command.
     * @throws IllegalArgumentException if the amount is not positive.
     */
    fun buildAuthorization(
        amountInCents: Long,
        paymentType: Byte = ZvtConstants.PAY_TYPE_DEFAULT,
        currencyCode: Int = ZvtConstants.CURRENCY_EUR
    ): ZvtPacket {
        require(amountInCents > 0) { "Amount must be greater than 0" }

        val data = mutableListOf<Byte>()

        // Amount (BMP 0x04, 6 byte BCD)
        data.add(ZvtConstants.BMP_AMOUNT)
        data.addAll(BcdHelper.amountToBcd(amountInCents).toList())

        // Payment type (BMP 0x19) — only if non-default
        if (paymentType != ZvtConstants.PAY_TYPE_DEFAULT) {
            data.add(ZvtConstants.BMP_PAYMENT_TYPE)
            data.add(paymentType)
        }

        // Currency code (BMP 0x49) — optional, terminal uses Registration currency
        // Some terminals reject if CC is included. Only send if explicitly specified.
        if (currencyCode > 0) {
            data.add(ZvtConstants.BMP_CURRENCY_CODE)
            data.addAll(BcdHelper.currencyToBcd(currencyCode).toList())
        }

        val packet = ZvtPacket(
            command = ZvtConstants.CMD_AUTHORIZATION,
            data = data.toByteArray()
        )

        Timber.tag(TAG).d("[CommandBuilder] Built Authorization: amount=%d cents (%.2f EUR), paymentType=0x%02X",
            amountInCents, amountInCents / 100.0, paymentType)

        return packet
    }

    /**
     * Builds an Authorization command using a Euro amount (convenience overload).
     *
     * @param amountEuro Amount in Euro, e.g. 12.50.
     * @param paymentType Payment type byte.
     * @param currencyCode ISO 4217 currency code.
     * @return [ZvtPacket] containing the Authorization command.
     */
    fun buildAuthorization(
        amountEuro: Double,
        paymentType: Byte = ZvtConstants.PAY_TYPE_DEFAULT,
        currencyCode: Int = ZvtConstants.CURRENCY_EUR
    ): ZvtPacket {
        val cents = (amountEuro * 100).toLong()
        return buildAuthorization(cents, paymentType, currencyCode)
    }

    // =====================================================
    // Reversal (06 30)
    // =====================================================

    /**
     * Builds a Reversal command (06 30) to cancel a previous transaction.
     *
     * @param receiptNumber Optional receipt number of the transaction to reverse (BCD 2 bytes).
     * @return [ZvtPacket] containing the Reversal command.
     */
    fun buildReversal(receiptNumber: Int? = null): ZvtPacket {
        val data = mutableListOf<Byte>()

        // Password (3 byte BCD - "000000")
        data.addAll(BcdHelper.stringToBcd("000000").toList())

        // Receipt number (optional)
        if (receiptNumber != null) {
            data.add(ZvtConstants.BMP_RECEIPT_NR)
            data.addAll(BcdHelper.stringToBcd(receiptNumber.toString().padStart(4, '0')).toList())
        }

        val packet = ZvtPacket(
            command = ZvtConstants.CMD_REVERSAL,
            data = data.toByteArray()
        )

        Timber.tag(TAG).d("[CommandBuilder] Built Reversal: receiptNumber=%s",
            receiptNumber?.toString() ?: "none")

        return packet
    }

    // =====================================================
    // Refund (06 31)
    // =====================================================

    /**
     * Builds a Refund command (06 31).
     *
     * @param amountInCents Refund amount in cents.
     * @param currencyCode ISO 4217 currency code.
     * @return [ZvtPacket] containing the Refund command.
     * @throws IllegalArgumentException if the amount is not positive.
     */
    fun buildRefund(
        amountInCents: Long,
        currencyCode: Int = ZvtConstants.CURRENCY_EUR
    ): ZvtPacket {
        require(amountInCents > 0) { "Refund amount must be greater than 0" }

        val data = mutableListOf<Byte>()

        // Password (3 byte BCD)
        data.addAll(BcdHelper.stringToBcd("000000").toList())

        // Amount (BMP 0x04, 6 byte BCD)
        data.add(ZvtConstants.BMP_AMOUNT)
        data.addAll(BcdHelper.amountToBcd(amountInCents).toList())

        // Currency code (BMP 0x49)
        data.add(ZvtConstants.BMP_CURRENCY_CODE)
        data.addAll(BcdHelper.currencyToBcd(currencyCode).toList())

        val packet = ZvtPacket(
            command = ZvtConstants.CMD_REFUND,
            data = data.toByteArray()
        )

        Timber.tag(TAG).d("[CommandBuilder] Built Refund: amount=%d cents (%.2f EUR), currency=%d",
            amountInCents, amountInCents / 100.0, currencyCode)

        return packet
    }

    // =====================================================
    // End of Day (06 50)
    // =====================================================

    /**
     * Builds an End of Day command (06 50) to close the daily batch.
     *
     * @return [ZvtPacket] containing the End of Day command.
     */
    fun buildEndOfDay(): ZvtPacket {
        val data = mutableListOf<Byte>()

        // Password (3 byte BCD)
        data.addAll(BcdHelper.stringToBcd("000000").toList())

        val packet = ZvtPacket(
            command = ZvtConstants.CMD_END_OF_DAY,
            data = data.toByteArray()
        )

        Timber.tag(TAG).d("[CommandBuilder] Built End of Day")

        return packet
    }

    // =====================================================
    // Diagnosis (06 70)
    // =====================================================

    /**
     * Builds a Diagnosis command (06 70) to query the terminal status.
     *
     * @return [ZvtPacket] containing the Diagnosis command.
     */
    fun buildDiagnosis(): ZvtPacket {
        Timber.tag(TAG).d("[CommandBuilder] Built Diagnosis")
        return ZvtPacket(
            command = ZvtConstants.CMD_DIAGNOSIS,
            data = byteArrayOf()
        )
    }

    // =====================================================
    // Status Enquiry (05 01)
    // =====================================================

    /**
     * Builds a Status Enquiry command (05 01) to check the terminal state.
     *
     * @return [ZvtPacket] containing the Status Enquiry command.
     */
    fun buildStatusEnquiry(): ZvtPacket {
        Timber.tag(TAG).d("[CommandBuilder] Built Status Enquiry")
        return ZvtPacket(
            command = ZvtConstants.CMD_STATUS_ENQUIRY,
            data = byteArrayOf()
        )
    }

    // =====================================================
    // Abort (06 1E)
    // =====================================================

    /**
     * Builds an Abort command (06 1E) to cancel an ongoing operation.
     *
     * @return [ZvtPacket] containing the Abort command.
     */
    fun buildAbort(): ZvtPacket {
        Timber.tag(TAG).d("[CommandBuilder] Built Abort")
        return ZvtPacket(
            command = ZvtConstants.CMD_ABORT,
            data = byteArrayOf()
        )
    }

    // =====================================================
    // Log Off (06 02)
    // =====================================================

    /**
     * Builds a Log Off command (06 02) to disconnect from the terminal.
     *
     * @return [ZvtPacket] containing the Log Off command.
     */
    fun buildLogOff(): ZvtPacket {
        Timber.tag(TAG).d("[CommandBuilder] Built Log Off")
        return ZvtPacket(
            command = ZvtConstants.CMD_LOG_OFF,
            data = byteArrayOf()
        )
    }

    // =====================================================
    // Pre-Authorization (06 22)
    // =====================================================

    /**
     * Builds a Pre-Authorization command (06 22) for reserving an amount.
     *
     * Used in hotel, car rental, and similar scenarios where the final
     * amount is not yet known.
     *
     * @param amountInCents Amount to reserve in cents.
     * @param currencyCode ISO 4217 currency code.
     * @return [ZvtPacket] containing the Pre-Authorization command.
     * @throws IllegalArgumentException if the amount is not positive.
     */
    fun buildPreAuthorization(
        amountInCents: Long,
        currencyCode: Int = ZvtConstants.CURRENCY_EUR
    ): ZvtPacket {
        require(amountInCents > 0) { "Amount must be greater than 0" }

        val data = mutableListOf<Byte>()

        // Amount (BMP 0x04, 6 byte BCD)
        data.add(ZvtConstants.BMP_AMOUNT)
        data.addAll(BcdHelper.amountToBcd(amountInCents).toList())

        // Currency (BMP 0x49)
        data.add(ZvtConstants.BMP_CURRENCY_CODE)
        data.addAll(BcdHelper.currencyToBcd(currencyCode).toList())

        val packet = ZvtPacket(
            command = ZvtConstants.CMD_PRE_AUTHORIZATION,
            data = data.toByteArray()
        )

        Timber.tag(TAG).d("[CommandBuilder] Built Pre-Authorization: amount=%d cents (%.2f EUR), currency=%d",
            amountInCents, amountInCents / 100.0, currencyCode)

        return packet
    }

    // =====================================================
    // Book Total / Pre-Auth Completion (06 24)
    // =====================================================

    /**
     * Builds a Book Total command (06 24) to complete a pre-authorization.
     *
     * After a successful pre-authorization, this command books the final amount.
     * The receipt number from the original pre-authorization must be provided.
     *
     * @param amountInCents Final amount to book in cents.
     * @param receiptNumber Receipt number of the original pre-authorization (BCD 2 bytes).
     * @param currencyCode ISO 4217 currency code.
     * @return [ZvtPacket] containing the Book Total command.
     * @throws IllegalArgumentException if the amount is not positive.
     */
    fun buildBookTotal(
        amountInCents: Long,
        receiptNumber: Int,
        currencyCode: Int = ZvtConstants.CURRENCY_EUR
    ): ZvtPacket {
        require(amountInCents > 0) { "Amount must be greater than 0" }

        val data = mutableListOf<Byte>()

        // Password (3 byte BCD)
        data.addAll(BcdHelper.stringToBcd("000000").toList())

        // Amount (BMP 0x04, 6 byte BCD)
        data.add(ZvtConstants.BMP_AMOUNT)
        data.addAll(BcdHelper.amountToBcd(amountInCents).toList())

        // Currency (BMP 0x49)
        data.add(ZvtConstants.BMP_CURRENCY_CODE)
        data.addAll(BcdHelper.currencyToBcd(currencyCode).toList())

        // Receipt number (BMP 0x87, BCD 2 bytes)
        data.add(ZvtConstants.BMP_RECEIPT_NR)
        data.addAll(BcdHelper.stringToBcd(receiptNumber.toString().padStart(4, '0')).toList())

        val packet = ZvtPacket(
            command = ZvtConstants.CMD_BOOK_TOTAL,
            data = data.toByteArray()
        )

        Timber.tag(TAG).d("[CommandBuilder] Built Book Total: amount=%d cents (%.2f EUR), receiptNr=%d",
            amountInCents, amountInCents / 100.0, receiptNumber)

        return packet
    }

    // =====================================================
    // Partial Reversal (06 25)
    // =====================================================

    /**
     * Builds a Partial Reversal command (06 25) to partially reverse a transaction.
     *
     * @param amountInCents Amount to partially reverse in cents.
     * @param receiptNumber Receipt number of the original transaction (BCD 2 bytes).
     * @param currencyCode ISO 4217 currency code.
     * @return [ZvtPacket] containing the Partial Reversal command.
     * @throws IllegalArgumentException if the amount is not positive.
     */
    fun buildPartialReversal(
        amountInCents: Long,
        receiptNumber: Int,
        currencyCode: Int = ZvtConstants.CURRENCY_EUR
    ): ZvtPacket {
        require(amountInCents > 0) { "Amount must be greater than 0" }

        val data = mutableListOf<Byte>()

        // Password (3 byte BCD)
        data.addAll(BcdHelper.stringToBcd("000000").toList())

        // Amount (BMP 0x04, 6 byte BCD)
        data.add(ZvtConstants.BMP_AMOUNT)
        data.addAll(BcdHelper.amountToBcd(amountInCents).toList())

        // Currency (BMP 0x49)
        data.add(ZvtConstants.BMP_CURRENCY_CODE)
        data.addAll(BcdHelper.currencyToBcd(currencyCode).toList())

        // Receipt number (BMP 0x87, BCD 2 bytes)
        data.add(ZvtConstants.BMP_RECEIPT_NR)
        data.addAll(BcdHelper.stringToBcd(receiptNumber.toString().padStart(4, '0')).toList())

        val packet = ZvtPacket(
            command = ZvtConstants.CMD_PARTIAL_REVERSAL,
            data = data.toByteArray()
        )

        Timber.tag(TAG).d("[CommandBuilder] Built Partial Reversal: amount=%d cents (%.2f EUR), receiptNr=%d",
            amountInCents, amountInCents / 100.0, receiptNumber)

        return packet
    }

    // =====================================================
    // Repeat Receipt (06 20)
    // =====================================================

    /**
     * Builds a Repeat Receipt command (06 20) to re-print the last receipt.
     *
     * @return [ZvtPacket] containing the Repeat Receipt command.
     */
    fun buildRepeatReceipt(): ZvtPacket {
        val data = mutableListOf<Byte>()

        // Password (3 byte BCD)
        data.addAll(BcdHelper.stringToBcd("000000").toList())

        val packet = ZvtPacket(
            command = ZvtConstants.CMD_REPEAT_RECEIPT,
            data = data.toByteArray()
        )

        Timber.tag(TAG).d("[CommandBuilder] Built Repeat Receipt")

        return packet
    }

    // =====================================================
    // Print Line ACK (06 D1 response)
    // =====================================================

    /**
     * Builds an ACK response for a received Print Line message.
     *
     * @return ACK [ZvtPacket].
     */
    fun buildPrintLineAck(): ZvtPacket {
        return ZvtPacket.ack()
    }
}
