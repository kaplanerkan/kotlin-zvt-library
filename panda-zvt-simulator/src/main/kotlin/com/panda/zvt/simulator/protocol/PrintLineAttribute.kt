package com.panda.zvt.simulator.protocol

/**
 * `<attribute>` byte of the ZVT Print Line command (06 D1).
 *
 * ZVT protocol 13.13, chapter 3.5, Table 16 "Definition of <attribute>":
 *
 * ```
 * 1000 0000                     RFU
 * 1xxx xxxx (not equal to 80h)  this is the last line
 * 1111 1111                     linefeed, count of feeds follows in <text>
 * 01xx nnnn                     centred
 * 0x1x nnnn                     double width
 * 0xx1 nnnn                     double height
 * 0000 nnnn                     normal text
 * ```
 *
 * `nnnn` is the number of characters to indent from the left (0-15).
 *
 * Besides marking the end of a receipt, the last-line flag also tells the ECR
 * that a switch between customer- and merchant-receipt takes place. ECRs that
 * buffer lines for a page printer, or drive a cutter, depend on it.
 */
object PrintLineAttribute {

    const val NORMAL: Byte = 0x00
    const val DOUBLE_HEIGHT: Byte = 0x10
    const val DOUBLE_WIDTH: Byte = 0x20
    const val CENTRED: Byte = 0x40

    /** `FF`: the text field carries a single byte with the number of linefeeds. */
    const val LINEFEED: Byte = 0xFF.toByte()

    /** Bit 7 — "this is the last line". */
    const val LAST_LINE_FLAG = 0x80

    /** Reserved by the spec, so bit 7 on its own is not a usable last-line marker. */
    const val RFU = 0x80

    /** Indent in characters from the left, 0-15. */
    fun indent(chars: Int): Byte {
        require(chars in 0..15) { "Indent must be 0..15, was $chars" }
        return chars.toByte()
    }

    /**
     * Marks [base] as the last line of a receipt.
     *
     * Bit 7 alone is `80h`, which the spec reserves, so plain normal text with
     * no indent cannot be flagged as the last line. In that case the line is
     * centred as well — receipts end on a centred line anyway, and it keeps the
     * marker spec-conformant instead of emitting the RFU value.
     */
    fun lastLine(base: Byte = NORMAL): Byte {
        val marked = (base.toInt() and 0x7F) or LAST_LINE_FLAG
        return if (marked == RFU) (LAST_LINE_FLAG or CENTRED.toInt()).toByte() else marked.toByte()
    }

    /** True if [attribute] carries the last-line marker (bit 7 set, but not `80h` or `FFh`). */
    fun isLastLine(attribute: Byte): Boolean {
        val v = attribute.toInt() and 0xFF
        return v and LAST_LINE_FLAG != 0 && v != RFU && v != 0xFF
    }
}
