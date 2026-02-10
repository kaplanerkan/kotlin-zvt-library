package com.erkan.zvt

import com.erkan.zvt.model.*

/**
 * ZVT olay dinleyici arayüzü
 *
 * Terminal'den gelen olayları (durum değişiklikleri, ara durumlar, yazdırma satırları vs.)
 * dinlemek için kullanılır.
 *
 * Kullanım:
 * ```kotlin
 * zvtClient.setCallback(object : ZvtCallback {
 *     override fun onConnectionStateChanged(state: ConnectionState) { ... }
 *     override fun onIntermediateStatus(status: IntermediateStatus) { ... }
 *     override fun onPrintLine(line: String) { ... }
 * })
 * ```
 */
interface ZvtCallback {

    /**
     * Bağlantı durumu değiştiğinde çağrılır
     * @param state Yeni bağlantı durumu
     */
    fun onConnectionStateChanged(state: ConnectionState) {}

    /**
     * Ara durum bilgisi geldiğinde çağrılır (04 FF)
     * Örn: "Kart bekleniyor...", "PIN girişi bekleniyor..."
     * @param status Ara durum bilgisi
     */
    fun onIntermediateStatus(status: IntermediateStatus) {}

    /**
     * Terminal'den yazdırma satırı geldiğinde çağrılır (06 D1)
     * @param line Yazdırılacak metin satırı
     */
    fun onPrintLine(line: String) {}

    /**
     * Debug log mesajı
     * @param tag Log etiketi
     * @param message Log mesajı
     */
    fun onDebugLog(tag: String, message: String) {}

    /**
     * Hata oluştuğunda çağrılır
     * @param error Hata detayı
     */
    fun onError(error: ZvtError) {}
}
