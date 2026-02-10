package com.panda.zvt_library.util

/**
 * BCD (Binary Coded Decimal) encoding and decoding utility.
 *
 * In the ZVT protocol, amounts, dates, trace numbers, and currency codes
 * are encoded in BCD format. Each byte carries two decimal digits:
 * - Upper nibble (4 bits): first digit
 * - Lower nibble (4 bits): second digit
 *
 * Example: 12.50 EUR -> cents: 1250 -> BCD bytes: `00 00 00 00 12 50`
 *
 * Reference: ZVT Protocol Specification v13.13
 *
 * @author Erkan Kaplan
 * @since 2026-02-10
 */
object BcdHelper {

    // =====================================================
    // Amount Conversions
    // =====================================================

    /**
     * Converts an amount in cents to a 6-byte BCD representation.
     *
     * @param amountInCents Amount in cents, e.g. 1250 = 12.50 EUR.
     * @return 6-byte BCD array.
     * @throws IllegalArgumentException if the amount is negative or exceeds the maximum.
     *
     * Example: `1250` -> `[0x00, 0x00, 0x00, 0x00, 0x12, 0x50]`
     */
    fun amountToBcd(amountInCents: Long): ByteArray {
        require(amountInCents >= 0) { "Amount must not be negative: $amountInCents" }
        require(amountInCents <= 999999999999L) { "Amount too large: $amountInCents" }
        return stringToBcd(amountInCents.toString().padStart(12, '0'))
    }

    /**
     * Converts a 6-byte BCD amount to cents.
     *
     * @param bcd 6-byte BCD array.
     * @return Amount in cents.
     *
     * Example: `[0x00, 0x00, 0x00, 0x00, 0x12, 0x50]` -> `1250`
     */
    fun bcdToAmount(bcd: ByteArray): Long {
        return bcdToString(bcd).toLongOrNull() ?: 0L
    }

    /**
     * Converts a Euro amount (Double) to a 6-byte BCD representation.
     *
     * @param amount Amount in Euro, e.g. 12.50.
     * @return 6-byte BCD array.
     */
    fun euroToBcd(amount: Double): ByteArray {
        val cents = (amount * 100).toLong()
        return amountToBcd(cents)
    }

    /**
     * Converts a 6-byte BCD amount to Euro (Double).
     *
     * @param bcd 6-byte BCD array.
     * @return Amount in Euro.
     */
    fun bcdToEuro(bcd: ByteArray): Double {
        return bcdToAmount(bcd) / 100.0
    }

    // =====================================================
    // Date/Time Conversions
    // =====================================================

    /**
     * Converts time components to a 3-byte BCD representation (HHMMSS).
     *
     * @param hour Hour (0-23).
     * @param minute Minute (0-59).
     * @param second Second (0-59).
     * @return 3-byte BCD array: HH MM SS.
     * @throws IllegalArgumentException if any component is out of range.
     */
    fun timeToBcd(hour: Int, minute: Int, second: Int): ByteArray {
        require(hour in 0..23) { "Invalid hour: $hour" }
        require(minute in 0..59) { "Invalid minute: $minute" }
        require(second in 0..59) { "Invalid second: $second" }
        val str = String.format("%02d%02d%02d", hour, minute, second)
        return stringToBcd(str)
    }

    /**
     * Extracts time components from a 3-byte BCD representation.
     *
     * @param bcd 3-byte BCD array (HHMMSS).
     * @return Triple(hour, minute, second).
     * @throws IllegalArgumentException if the array is too short.
     */
    fun bcdToTime(bcd: ByteArray): Triple<Int, Int, Int> {
        require(bcd.size >= 3) { "Time BCD must be at least 3 bytes" }
        val str = bcdToString(bcd.copyOfRange(0, 3))
        return Triple(
            str.substring(0, 2).toInt(),
            str.substring(2, 4).toInt(),
            str.substring(4, 6).toInt()
        )
    }

    /**
     * Converts date components to a 2-byte BCD representation (MMDD).
     *
     * @param month Month (1-12).
     * @param day Day (1-31).
     * @return 2-byte BCD array: MM DD.
     * @throws IllegalArgumentException if any component is out of range.
     */
    fun dateToBcd(month: Int, day: Int): ByteArray {
        require(month in 1..12) { "Invalid month: $month" }
        require(day in 1..31) { "Invalid day: $day" }
        val str = String.format("%02d%02d", month, day)
        return stringToBcd(str)
    }

    /**
     * Extracts date components from a 2-byte BCD representation.
     *
     * @param bcd 2-byte BCD array (MMDD).
     * @return Pair(month, day).
     * @throws IllegalArgumentException if the array is too short.
     */
    fun bcdToDate(bcd: ByteArray): Pair<Int, Int> {
        require(bcd.size >= 2) { "Date BCD must be at least 2 bytes" }
        val str = bcdToString(bcd.copyOfRange(0, 2))
        return Pair(
            str.substring(0, 2).toInt(),
            str.substring(2, 4).toInt()
        )
    }

    // =====================================================
    // Trace Number
    // =====================================================

    /**
     * Converts a trace number to a 3-byte BCD representation.
     *
     * @param traceNumber Value in range 0..999999.
     * @return 3-byte BCD array.
     * @throws IllegalArgumentException if the number is out of range.
     */
    fun traceNumberToBcd(traceNumber: Int): ByteArray {
        require(traceNumber in 0..999999) { "Invalid trace number: $traceNumber" }
        return stringToBcd(traceNumber.toString().padStart(6, '0'))
    }

    /**
     * Extracts a trace number from a 3-byte BCD representation.
     *
     * @param bcd BCD byte array (at least 3 bytes).
     * @return Trace number as integer.
     */
    fun bcdToTraceNumber(bcd: ByteArray): Int {
        return bcdToString(bcd.copyOfRange(0, minOf(3, bcd.size))).toIntOrNull() ?: 0
    }

    // =====================================================
    // Currency Code
    // =====================================================

    /**
     * Converts an ISO 4217 currency code to a 2-byte BCD representation.
     *
     * Example: `978` (EUR) -> `[0x09, 0x78]`
     *
     * @param currencyCode ISO 4217 numeric currency code.
     * @return 2-byte BCD array.
     */
    fun currencyToBcd(currencyCode: Int): ByteArray {
        return stringToBcd(currencyCode.toString().padStart(4, '0'))
    }

    /**
     * Extracts an ISO 4217 currency code from a 2-byte BCD representation.
     *
     * @param bcd 2-byte BCD array.
     * @return ISO 4217 numeric currency code.
     */
    fun bcdToCurrency(bcd: ByteArray): Int {
        return bcdToString(bcd.copyOfRange(0, minOf(2, bcd.size))).toIntOrNull() ?: 0
    }

    // =====================================================
    // General BCD Conversion Functions
    // =====================================================

    /**
     * Converts a numeric string to a BCD byte array.
     *
     * The string is left-padded with '0' if its length is odd.
     *
     * Example: `"001250"` -> `[0x00, 0x12, 0x50]`
     *
     * @param numStr A string containing only digit characters.
     * @return BCD-encoded byte array.
     * @throws IllegalArgumentException if the string contains non-digit characters.
     */
    fun stringToBcd(numStr: String): ByteArray {
        val padded = if (numStr.length % 2 != 0) "0$numStr" else numStr
        require(padded.all { it.isDigit() }) { "BCD string must contain only digits: $numStr" }

        return ByteArray(padded.length / 2) { i ->
            val high = padded[i * 2].digitToInt()
            val low = padded[i * 2 + 1].digitToInt()
            ((high shl 4) or low).toByte()
        }
    }

    /**
     * Converts a BCD byte array to a numeric string.
     *
     * Each byte's upper and lower nibbles become two decimal digits.
     *
     * Example: `[0x00, 0x12, 0x50]` -> `"001250"`
     *
     * @param bcd BCD-encoded byte array.
     * @return Numeric string representation.
     */
    fun bcdToString(bcd: ByteArray): String {
        val sb = StringBuilder(bcd.size * 2)
        for (b in bcd) {
            val unsigned = b.toInt() and 0xFF
            sb.append(unsigned shr 4)     // Upper nibble
            sb.append(unsigned and 0x0F)  // Lower nibble
        }
        return sb.toString()
    }

    /**
     * Converts a BCD-encoded PAN to a masked string.
     *
     * In the ZVT protocol, masked PAN digits use nibble `E` (14) as placeholder
     * and nibble `F` (15) as trailing padding. Valid BCD digits (0-9) are kept as-is.
     *
     * Example: `[0x68, 0x05, 0x40, 0xEE, 0xEE, 0xEE, 0xEE, 0xE3, 0x18, 0x6F]`
     *          -> `"680540********3186"`
     *
     * @param bcd BCD-encoded PAN byte array.
     * @return Masked PAN string with `*` for hidden digits.
     */
    fun bcdToPan(bcd: ByteArray): String {
        val sb = StringBuilder(bcd.size * 2)
        for (b in bcd) {
            val unsigned = b.toInt() and 0xFF
            val high = unsigned shr 4
            val low = unsigned and 0x0F
            when {
                high in 0..9 -> sb.append(high)
                high == 0x0E -> sb.append('*')
                // 0x0F = padding, skip
            }
            when {
                low in 0..9 -> sb.append(low)
                low == 0x0E -> sb.append('*')
                // 0x0F = padding, skip
            }
        }
        return sb.toString()
    }

    /**
     * Formats a cent amount into a human-readable Euro string.
     *
     * Example: `1250` -> `"12,50"`
     *
     * @param amountInCents Amount in cents.
     * @return Formatted string with comma as decimal separator.
     */
    fun formatAmount(amountInCents: Long): String {
        val euros = amountInCents / 100
        val cents = amountInCents % 100
        return String.format("%d,%02d", euros, cents)
    }
}
