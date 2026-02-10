package com.panda.zvt_library.util

/**
 * BCD (Binary Coded Decimal) yardımcı sınıfı
 *
 * ZVT protokolünde tutarlar, tarihler ve bazı numaralar BCD formatında kodlanır.
 * Örnek: 12.50 EUR → cent: 1250 → BCD bytes: 00 00 00 00 12 50
 *
 * Her byte iki ondalık basamak taşır:
 * - Üst nibble (4 bit): İlk basamak
 * - Alt nibble (4 bit): İkinci basamak
 */
object BcdHelper {

    // =====================================================
    // Tutar Dönüşümleri
    // =====================================================

    /**
     * Cent cinsinden tutarı 6 byte BCD'ye çevirir
     * @param amountInCents Tutar (cent), ör: 1250 = 12.50 EUR
     * @return 6 byte BCD dizisi
     *
     * Örnek: 1250 → [0x00, 0x00, 0x00, 0x00, 0x12, 0x50]
     */
    fun amountToBcd(amountInCents: Long): ByteArray {
        require(amountInCents >= 0) { "Tutar negatif olamaz: $amountInCents" }
        require(amountInCents <= 999999999999L) { "Tutar çok büyük: $amountInCents" }
        return stringToBcd(amountInCents.toString().padStart(12, '0'))
    }

    /**
     * 6 byte BCD'yi cent cinsinden tutara çevirir
     * @param bcd 6 byte BCD dizisi
     * @return Tutar (cent)
     *
     * Örnek: [0x00, 0x00, 0x00, 0x00, 0x12, 0x50] → 1250
     */
    fun bcdToAmount(bcd: ByteArray): Long {
        return bcdToString(bcd).toLongOrNull() ?: 0L
    }

    /**
     * Euro cinsinden tutarı BCD'ye çevirir (Double → BCD)
     * @param amount Tutar (Euro), ör: 12.50
     */
    fun euroToBcd(amount: Double): ByteArray {
        val cents = (amount * 100).toLong()
        return amountToBcd(cents)
    }

    /**
     * BCD'yi Euro tutarına çevirir (BCD → Double)
     */
    fun bcdToEuro(bcd: ByteArray): Double {
        return bcdToAmount(bcd) / 100.0
    }

    // =====================================================
    // Tarih/Saat Dönüşümleri
    // =====================================================

    /**
     * Saat bilgisini BCD'ye çevirir
     * @param hour Saat (0-23)
     * @param minute Dakika (0-59)
     * @param second Saniye (0-59)
     * @return 3 byte BCD: HH MM SS
     */
    fun timeToBcd(hour: Int, minute: Int, second: Int): ByteArray {
        require(hour in 0..23) { "Geçersiz saat: $hour" }
        require(minute in 0..59) { "Geçersiz dakika: $minute" }
        require(second in 0..59) { "Geçersiz saniye: $second" }
        val str = String.format("%02d%02d%02d", hour, minute, second)
        return stringToBcd(str)
    }

    /**
     * BCD'den saat bilgisini okur
     * @param bcd 3 byte BCD
     * @return Triple(hour, minute, second)
     */
    fun bcdToTime(bcd: ByteArray): Triple<Int, Int, Int> {
        require(bcd.size >= 3) { "Saat BCD en az 3 byte olmalı" }
        val str = bcdToString(bcd.copyOfRange(0, 3))
        return Triple(
            str.substring(0, 2).toInt(),
            str.substring(2, 4).toInt(),
            str.substring(4, 6).toInt()
        )
    }

    /**
     * Tarih bilgisini BCD'ye çevirir
     * @param month Ay (1-12)
     * @param day Gün (1-31)
     * @return 2 byte BCD: MM DD
     */
    fun dateToBcd(month: Int, day: Int): ByteArray {
        require(month in 1..12) { "Geçersiz ay: $month" }
        require(day in 1..31) { "Geçersiz gün: $day" }
        val str = String.format("%02d%02d", month, day)
        return stringToBcd(str)
    }

    /**
     * BCD'den tarih bilgisini okur
     * @param bcd 2 byte BCD
     * @return Pair(month, day)
     */
    fun bcdToDate(bcd: ByteArray): Pair<Int, Int> {
        require(bcd.size >= 2) { "Tarih BCD en az 2 byte olmalı" }
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
     * Trace numarasını 3 byte BCD'ye çevirir
     * @param traceNumber 0..999999 arası numara
     */
    fun traceNumberToBcd(traceNumber: Int): ByteArray {
        require(traceNumber in 0..999999) { "Geçersiz trace numarası: $traceNumber" }
        return stringToBcd(traceNumber.toString().padStart(6, '0'))
    }

    /**
     * 3 byte BCD'den trace numarasını okur
     */
    fun bcdToTraceNumber(bcd: ByteArray): Int {
        return bcdToString(bcd.copyOfRange(0, minOf(3, bcd.size))).toIntOrNull() ?: 0
    }

    // =====================================================
    // Para Birimi Kodu
    // =====================================================

    /**
     * ISO 4217 para birimi kodunu 2 byte BCD'ye çevirir
     * Örnek: 978 (EUR) → [0x09, 0x78]
     */
    fun currencyToBcd(currencyCode: Int): ByteArray {
        return stringToBcd(currencyCode.toString().padStart(4, '0'))
    }

    /**
     * 2 byte BCD'den para birimi kodunu okur
     */
    fun bcdToCurrency(bcd: ByteArray): Int {
        return bcdToString(bcd.copyOfRange(0, minOf(2, bcd.size))).toIntOrNull() ?: 0
    }

    // =====================================================
    // Genel BCD Dönüşüm Fonksiyonları
    // =====================================================

    /**
     * Sayısal string'i BCD byte dizisine çevirir
     * String uzunluğu çift olmalıdır.
     *
     * Örnek: "001250" → [0x00, 0x12, 0x50]
     */
    fun stringToBcd(numStr: String): ByteArray {
        val padded = if (numStr.length % 2 != 0) "0$numStr" else numStr
        require(padded.all { it.isDigit() }) { "BCD string sadece rakam içermeli: $numStr" }

        return ByteArray(padded.length / 2) { i ->
            val high = padded[i * 2].digitToInt()
            val low = padded[i * 2 + 1].digitToInt()
            ((high shl 4) or low).toByte()
        }
    }

    /**
     * BCD byte dizisini sayısal string'e çevirir
     * Her byte'ın üst ve alt nibble'ı birer basamak olur.
     *
     * Örnek: [0x00, 0x12, 0x50] → "001250"
     */
    fun bcdToString(bcd: ByteArray): String {
        val sb = StringBuilder(bcd.size * 2)
        for (b in bcd) {
            val unsigned = b.toInt() and 0xFF
            sb.append(unsigned shr 4)     // Üst nibble
            sb.append(unsigned and 0x0F)  // Alt nibble
        }
        return sb.toString()
    }

    /**
     * Tutarı okunabilir formatta döner
     * Örnek: 1250 → "12,50"
     */
    fun formatAmount(amountInCents: Long): String {
        val euros = amountInCents / 100
        val cents = amountInCents % 100
        return String.format("%d,%02d", euros, cents)
    }
}
