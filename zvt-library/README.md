# ZVT Library for Android

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
include(":zvt-library")

// app/build.gradle.kts
dependencies {
    implementation(project(":zvt-library"))
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

## API Referansı

### ZvtClient

| Metod | Açıklama |
|-------|----------|
| `connect()` | TCP bağlantısı açar |
| `register()` | Terminal'e kayıt olur (06 00) |
| `authorize(amountInCents)` | Ödeme yapar (06 01) |
| `authorizeEuro(amount)` | Euro ile ödeme (kolay kullanım) |
| `reversal(receiptNumber?)` | Son işlemi iptal eder (06 30) |
| `refund(amountInCents)` | İade yapar (06 31) |
| `endOfDay()` | Gün sonu kapanışı (06 50) |
| `diagnosis()` | Terminal tanılaması (06 70) |
| `statusEnquiry()` | Durum sorgulama (05 01) |
| `abort()` | Devam eden işlemi iptal (06 1E) |
| `logOff()` | Terminal bağlantısını sonlandır (06 02) |
| `disconnect()` | TCP bağlantısını kapat |

### ZvtConfig

```kotlin
ZvtConfig(
    host = "192.168.1.100",     // Terminal IP
    port = 20007,                // Terminal port
    connectTimeoutMs = 10_000,   // Bağlantı timeout
    readTimeoutMs = 90_000,      // Okuma timeout
    password = "000000",         // Terminal şifresi
    currencyCode = 978,          // ISO 4217 para birimi
    autoAck = true,              // Otomatik ACK
    debugMode = false            // Debug log
)
```

### Modeller

- `TransactionResult` - İşlem sonucu (tutar, kart, fiş no, trace no...)
- `CardData` - Kart bilgileri (maskeli PAN, tip, AID...)
- `TerminalStatus` - Terminal durumu
- `DiagnosisResult` - Tanılama sonucu
- `EndOfDayResult` - Gün sonu sonucu
- `IntermediateStatus` - Ara durum bilgisi
- `ZvtError` - Hata tipleri (ConnectionError, TimeoutError, ProtocolError, TerminalError)

### Yardımcı Sınıflar

```kotlin
// BCD Helper
BcdHelper.amountToBcd(1250)        // → [00 00 00 00 12 50]
BcdHelper.bcdToEuro(bytes)          // → 12.50
BcdHelper.timeToBcd(14, 30, 0)     // → [14 30 00]
BcdHelper.currencyToBcd(978)        // → [09 78]

// TLV Parser
val entries = TlvParser.parse(data)
val cardName = TlvParser.findTag(data, 0x1F10)
val tlvBytes = TlvParser.buildTlv(0x1F10, "VISA".toByteArray())

// Byte Extensions
byteArray.toHexString()             // → "06 01 00"
"06 01".hexToByteArray()            // → [0x06, 0x01]
```

## Proje Yapısı

```
zvt-library/
├── src/main/java/com/erkan/zvt/
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

## Desteklenen Terminaller

ZVT TCP/IP protokolünü destekleyen tüm terminaller:
- CCV (Android & Linux terminaller)
- Ingenico
- Verifone
- Worldline

## Lisans

MIT License
