package com.panda.zvt.simulator.response

import org.junit.Assert.assertEquals
import org.junit.Test

class AmountFormatterTest {

    @Test
    fun currencyName_knownCode_returnsAlphaCode() {
        assertEquals("EUR", AmountFormatter.currencyName(978))
        assertEquals("CHF", AmountFormatter.currencyName(756))
        assertEquals("TRY", AmountFormatter.currencyName(949))
    }

    @Test
    fun currencyName_unknownCode_fallsBackToNumeric() {
        assertEquals("999", AmountFormatter.currencyName(999))
    }

    @Test
    fun format_usesGermanDecimalCommaAndCurrency() {
        assertEquals("12,50 EUR", AmountFormatter.format(1250, 978))
        assertEquals("0,05 CHF", AmountFormatter.format(5, 756))
        assertEquals("100,00 EUR", AmountFormatter.format(10000, 978))
    }

    @Test
    fun format_zero() {
        assertEquals("0,00 EUR", AmountFormatter.format(0, 978))
    }
}
