package com.panda.zvt.simulator.response

/**
 * Formats amounts for human-readable output (print lines, receipts).
 *
 * The currency is taken from the configured ISO 4217 numeric code instead of
 * being hardcoded, so a simulator configured for e.g. CHF does not print EUR.
 */
object AmountFormatter {

    private val ISO_4217 = mapOf(
        978 to "EUR",
        840 to "USD",
        826 to "GBP",
        756 to "CHF",
        949 to "TRY",
        208 to "DKK",
        752 to "SEK",
        578 to "NOK",
        985 to "PLN",
        203 to "CZK",
        348 to "HUF"
    )

    /** ISO 4217 alpha code for [code], or the numeric code itself when unknown. */
    fun currencyName(code: Int): String = ISO_4217[code] ?: code.toString()

    /** Formats minor units, e.g. `format(1250, 978)` -> `"12,50 EUR"`. */
    fun format(cents: Long, currencyCode: Int): String =
        "%d,%02d %s".format(cents / 100, cents % 100, currencyName(currencyCode))
}
