# Panda ZVT Library for Android

Ödeme terminalleri ile TCP/IP üzerinden ZVT protokolü iletişimi sağlayan Android kütüphanesi.

## Özellikler

- **Tam ZVT Protokol Desteği**: Registration, Authorization, Reversal, Refund, End of Day, Diagnosis, Status Enquiry, Abort
- **Coroutine Tabanlı**: Tüm I/O işlemleri suspend fonksiyonlarla
- **Callback Mekanizması**: Intermediate status, print line, connection state dinleme
- **StateFlow**: Reactive bağlantı durumu takibi
- **BCD / TLV Yardımcıları**: Protokol veri yapıları için hazır util sınıfları
- **Thread-Safe**: Synchronized send, IO dispatcher üzerinde çalışma
- **Minimum Bağımlılık**: Sadece `kotlinx-coroutines`

## Kurulum

```kotlin
// settings.gradle.kts
include(":panda-zvt-library")

// app/build.gradle.kts
dependencies {
    implementation(project(":panda-zvt-library"))
}
```

## Hızlı Başlangıç

```kotlin
// 1. Config oluştur
val config = ZvtConfig(
    host = "192.168.1.100",
    port = 20007,
    currencyCode = 978,  // EUR
    debugMode = true
)

// 2. Client oluştur
val client = ZvtClient(config)

// 3. Callback ayarla (opsiyonel)
client.setCallback(object : ZvtCallback {
    override fun onIntermediateStatus(status: IntermediateStatus) {
        println("Terminal: ${status.message}")
    }
    override fun onPrintLine(line: String) {
        println("Fiş: $line")
    }
})

// 4. Bağlan ve kayıt ol
client.connect()
client.register()

// 5. Ödeme yap (12.50 EUR)
val result = client.authorize(1250)
if (result.success) {
    println("Ödeme başarılı: ${result.amountFormatted}")
    println("Kart: ${result.cardData?.cardType}")
}

// 6. Bağlantıyı kapat
client.disconnect()
```

## Proje Yapısı

```
panda-zvt-library/
├── src/main/java/com/panda/zvt_library/
│   ├── ZvtClient.kt           ← Ana TCP client
│   ├── ZvtCallback.kt         ← Olay dinleyici interface
│   ├── model/
│   │   └── Models.kt          ← Tüm veri sınıfları
│   ├── protocol/
│   │   ├── ZvtConstants.kt    ← Protokol sabitleri
│   │   ├── ZvtPacket.kt       ← APDU paket yapısı
│   │   ├── ZvtCommandBuilder.kt ← Komut oluşturucu
│   │   └── ZvtResponseParser.kt ← Yanıt ayrıştırıcı
│   └── util/
│       ├── BcdHelper.kt       ← BCD encode/decode
│       ├── TlvParser.kt       ← TLV parse/build
│       └── ByteExtensions.kt  ← Extension fonksiyonlar
```

## Lisans

MIT License
