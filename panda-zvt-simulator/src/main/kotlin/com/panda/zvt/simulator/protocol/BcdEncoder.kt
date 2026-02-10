package com.panda.zvt.simulator.protocol

object BcdEncoder {

    fun amountToBcd(amountInCents: Long): ByteArray {
        require(amountInCents >= 0) { "Amount must not be negative" }
        return stringToBcd(amountInCents.toString().padStart(12, '0'))
    }

    fun traceNumberToBcd(trace: Int): ByteArray {
        require(trace in 0..999999) { "Invalid trace number: $trace" }
        return stringToBcd(trace.toString().padStart(6, '0'))
    }

    fun receiptNumberToBcd(receipt: Int): ByteArray {
        require(receipt in 0..9999) { "Invalid receipt number: $receipt" }
        return stringToBcd(receipt.toString().padStart(4, '0'))
    }

    fun turnoverNumberToBcd(turnover: Int): ByteArray {
        require(turnover in 0..999999) { "Invalid turnover number: $turnover" }
        return stringToBcd(turnover.toString().padStart(6, '0'))
    }

    fun timeToBcd(hour: Int, minute: Int, second: Int): ByteArray {
        return stringToBcd("%02d%02d%02d".format(hour, minute, second))
    }

    fun dateToBcd(month: Int, day: Int): ByteArray {
        return stringToBcd("%02d%02d".format(month, day))
    }

    fun expiryDateToBcd(yymm: String): ByteArray {
        val padded = yymm.padStart(4, '0')
        return stringToBcd(padded)
    }

    fun currencyToBcd(code: Int): ByteArray {
        return stringToBcd(code.toString().padStart(4, '0'))
    }

    fun cardSequenceNrToBcd(nr: Int): ByteArray {
        return stringToBcd(nr.toString().padStart(4, '0'))
    }

    fun terminalIdToBcd(tid: String): ByteArray {
        val padded = tid.padStart(8, '0').take(8)
        return stringToBcd(padded)
    }

    /**
     * Encodes a PAN as BCD with E-masking.
     * First 6 and last 4 digits are visible, middle digits are masked with nibble 0xE.
     * Padded with 0xF nibble if odd length.
     */
    fun panToBcd(pan: String): ByteArray {
        val digits = pan.filter { it.isDigit() }
        val masked = StringBuilder()
        for (i in digits.indices) {
            if (i < 6 || i >= digits.length - 4) {
                masked.append(digits[i])
            } else {
                masked.append('E')
            }
        }
        // Pad with F if odd length
        if (masked.length % 2 != 0) masked.append('F')

        return ByteArray(masked.length / 2) { i ->
            val highChar = masked[i * 2]
            val lowChar = masked[i * 2 + 1]
            val high = if (highChar == 'E') 0x0E else if (highChar == 'F') 0x0F else highChar.digitToInt()
            val low = if (lowChar == 'E') 0x0E else if (lowChar == 'F') 0x0F else lowChar.digitToInt()
            ((high shl 4) or low).toByte()
        }
    }

    fun stringToBcd(numStr: String): ByteArray {
        val padded = if (numStr.length % 2 != 0) "0$numStr" else numStr
        return ByteArray(padded.length / 2) { i ->
            val high = padded[i * 2].digitToInt()
            val low = padded[i * 2 + 1].digitToInt()
            ((high shl 4) or low).toByte()
        }
    }

    fun bcdToAmount(bcd: ByteArray): Long {
        val str = bcdToString(bcd)
        return str.toLongOrNull() ?: 0L
    }

    fun bcdToString(bcd: ByteArray): String {
        val sb = StringBuilder(bcd.size * 2)
        for (b in bcd) {
            val unsigned = b.toInt() and 0xFF
            sb.append(unsigned shr 4)
            sb.append(unsigned and 0x0F)
        }
        return sb.toString()
    }
}
