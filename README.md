# ZVT Client Library for Android (Kotlin)

A Kotlin/Android library implementing the **ZVT Protocol (v13.13)** for communication between an Electronic Cash Register (ECR) and Payment Terminals (PT) over TCP/IP.

## What is ZVT?

ZVT (Zahlungsverkehrstechnik) is the German standard protocol for communication between point-of-sale systems and payment terminals. It is widely used in Germany, Austria, and Switzerland for card payment processing.

- **Spec**: ZVT Protocol Specification v13.13 (PA00P015_13.13_final)
- **Transport**: TCP/IP, default port **20007**
- **Encoding**: Binary APDU (Application Protocol Data Unit)

## Project Structure

```
zvt-project/
├── app/                          # Demo/test Android application
├── zvt-library/                  # ZVT protocol library (reusable)
│   └── src/main/java/com/erkan/zvt/
│       ├── ZvtClient.kt          # Main client facade (TCP connection, command execution)
│       ├── ZvtCallback.kt        # Event listener interface
│       ├── model/
│       │   └── Models.kt         # Data models (TransactionResult, CardData, etc.)
│       ├── protocol/
│       │   ├── ZvtConstants.kt   # All protocol constants (commands, BMPs, error codes)
│       │   ├── ZvtPacket.kt      # APDU packet serialization/deserialization
│       │   ├── ZvtCommandBuilder.kt  # Command builders (Registration, Authorization, etc.)
│       │   └── ZvtResponseParser.kt  # Response parser (BMP fields, TLV containers)
│       └── util/
│           ├── TlvParser.kt      # TLV (Tag-Length-Value) parser/builder
│           ├── BcdHelper.kt      # BCD encoding/decoding utilities
│           └── ByteExtensions.kt # Byte array extension functions
└── gradle/
    └── libs.versions.toml        # Centralized dependency management
```

## Supported Commands

| Command | Code | Description |
|---------|------|-------------|
| Registration | `06 00` | Register ECR with terminal |
| Authorization | `06 01` | Payment transaction |
| Log Off | `06 02` | Disconnect from terminal |
| Pre-Authorization | `06 22` | Pre-authorize amount (hotel, car rental) |
| Reversal | `06 30` | Cancel a previous transaction |
| Refund | `06 31` | Refund a transaction |
| End of Day | `06 50` | Close daily batch |
| Diagnosis | `06 70` | Query terminal status |
| Status Enquiry | `05 01` | Check terminal state |

## Usage

```kotlin
val config = ZvtConfig(
    host = "192.168.1.100",
    port = 20007,
    password = "000000",
    currencyCode = 978, // EUR
    debugMode = true
)

val client = ZvtClient(config)

// Connect and register
client.connect()
client.register(configByte = ZvtConstants.REG_INTERMEDIATE_STATUS)

// Make a payment (12.50 EUR)
val result = client.authorize(amountInCents = 1250)
if (result.success) {
    println("Payment successful! Trace: ${result.traceNumber}")
    println("Card: ${result.cardData?.cardType}")
} else {
    println("Payment failed: ${result.resultMessage}")
}

// End of day
val eod = client.endOfDay()

// Disconnect
client.disconnect()
```

## Protocol Flow

```
ECR → PT:  Command APDU (e.g. 06 01 for Authorization)
PT  → ECR: ACK (80 00 00)
PT  → ECR: Intermediate Status (04 FF) [repeated] - "Insert card", "Enter PIN"...
ECR → PT:  ACK (80 00 00) [for each]
PT  → ECR: Status Information (04 0F) - Transaction result with BMP fields
ECR → PT:  ACK (80 00 00)
PT  → ECR: Print Line (06 D1) [repeated] - Receipt lines
ECR → PT:  ACK (80 00 00) [for each]
PT  → ECR: Completion (06 0F) or Abort (06 1E)
ECR → PT:  ACK (80 00 00)
```

## BMP Fields Parsed

The library parses the following BMP (Bitmap) data fields from Status Information responses:

| BMP | Name | Format |
|-----|------|--------|
| `04` | Amount | 6 byte BCD |
| `06` | TLV Container | BER-TLV length |
| `0B` | Trace Number | 3 byte BCD |
| `0C` | Time (HHMMSS) | 3 byte BCD |
| `0D` | Date (MMDD) | 2 byte BCD |
| `0E` | Expiry Date (YYMM) | 2 byte BCD |
| `17` | Card Sequence Number | 2 byte BCD |
| `19` | Payment Type | 1 byte |
| `22` | PAN/EF_ID | LLVAR BCD |
| `27` | Result Code | 1 byte |
| `29` | Terminal ID | 4 byte BCD |
| `2A` | VU Number | 15 byte ASCII |
| `37` | Original Trace | 3 byte BCD |
| `3B` | AID | 8 byte fixed |
| `3C` | Additional Data/TLV | LLLVAR |
| `49` | Currency Code | 2 byte BCD |
| `87` | Receipt Number | 2 byte BCD |
| `88` | Turnover Number | 3 byte BCD |
| `8A` | Card Type | 1 byte |
| `8B` | Card Name | LLVAR ASCII |
| `8C` | Card Type ID Network | 1 byte |
| `A0` | Result Code AS | 1 byte |
| `BA` | AID Parameter | 5 byte fixed |

## Build

```bash
./gradlew :zvt-library:assembleDebug
```

## Requirements

- Android SDK 36
- Kotlin 1.9.22
- Min SDK 24

---

# ZVT Client Library - Android (Kotlin) [TR]

Android icin **ZVT Protokolu (v13.13)** uygulayan bir Kotlin kutuphanesi. Yazar Kasa (ECR) ile Odeme Terminalleri (PT) arasinda TCP/IP uzerinden iletisim saglar.

## ZVT Nedir?

ZVT (Zahlungsverkehrstechnik), satis noktasi (POS) sistemleri ile odeme terminalleri arasindaki iletisimi saglayan Alman standart protokoludur. Almanya, Avusturya ve Isvicre'de kartli odeme islemlerinde yaygin olarak kullanilir.

- **Spesifikasyon**: ZVT Protocol Specification v13.13 (PA00P015_13.13_final)
- **Iletisim**: TCP/IP, varsayilan port **20007**
- **Kodlama**: Binary APDU (Application Protocol Data Unit)

## Proje Yapisi

```
zvt-project/
├── app/                          # Demo/test Android uygulamasi
├── zvt-library/                  # ZVT protokol kutuphanesi (yeniden kullanilabilir)
│   └── src/main/java/com/erkan/zvt/
│       ├── ZvtClient.kt          # Ana istemci (TCP baglanti, komut yurutme)
│       ├── ZvtCallback.kt        # Olay dinleyici arayuzu
│       ├── model/
│       │   └── Models.kt         # Veri modelleri (TransactionResult, CardData, vb.)
│       ├── protocol/
│       │   ├── ZvtConstants.kt   # Tum protokol sabitleri (komutlar, BMP'ler, hata kodlari)
│       │   ├── ZvtPacket.kt      # APDU paket serializasyon/deserializasyon
│       │   ├── ZvtCommandBuilder.kt  # Komut olusturucular (Registration, Authorization, vb.)
│       │   └── ZvtResponseParser.kt  # Yanit parcalayici (BMP alanlari, TLV containerlari)
│       └── util/
│           ├── TlvParser.kt      # TLV (Tag-Length-Value) parser/builder
│           ├── BcdHelper.kt      # BCD kodlama/cozme yardimcilari
│           └── ByteExtensions.kt # Byte dizisi uzanti fonksiyonlari
└── gradle/
    └── libs.versions.toml        # Merkezi bagimlilik yonetimi
```

## Desteklenen Komutlar

| Komut | Kod | Aciklama |
|-------|-----|----------|
| Registration | `06 00` | ECR'yi terminale kaydet |
| Authorization | `06 01` | Odeme islemi |
| Log Off | `06 02` | Terminal baglantisini sonlandir |
| Pre-Authorization | `06 22` | On yetkilendirme (otel, arac kiralama) |
| Reversal | `06 30` | Onceki islemi iptal et |
| Refund | `06 31` | Iade islemi |
| End of Day | `06 50` | Gun sonu kapanisi |
| Diagnosis | `06 70` | Terminal durumunu sorgula |
| Status Enquiry | `05 01` | Terminal durumunu kontrol et |

## Kullanim

```kotlin
val config = ZvtConfig(
    host = "192.168.1.100",
    port = 20007,
    password = "000000",
    currencyCode = 978, // EUR
    debugMode = true
)

val client = ZvtClient(config)

// Baglan ve kayit ol
client.connect()
client.register(configByte = ZvtConstants.REG_INTERMEDIATE_STATUS)

// Odeme yap (12.50 EUR)
val result = client.authorize(amountInCents = 1250)
if (result.success) {
    println("Odeme basarili! Trace: ${result.traceNumber}")
    println("Kart: ${result.cardData?.cardType}")
} else {
    println("Odeme basarisiz: ${result.resultMessage}")
}

// Gun sonu
val eod = client.endOfDay()

// Baglanti kapat
client.disconnect()
```

## Protokol Akisi

```
ECR → PT:  Komut APDU (orn: Authorization icin 06 01)
PT  → ECR: ACK (80 00 00)
PT  → ECR: Ara Durum (04 FF) [tekrarlanan] - "Kart takin", "PIN girin"...
ECR → PT:  ACK (80 00 00) [her biri icin]
PT  → ECR: Durum Bilgisi (04 0F) - BMP alanlariyla islem sonucu
ECR → PT:  ACK (80 00 00)
PT  → ECR: Yazdir Satiri (06 D1) [tekrarlanan] - Fis satirlari
ECR → PT:  ACK (80 00 00) [her biri icin]
PT  → ECR: Tamamlandi (06 0F) veya Iptal (06 1E)
ECR → PT:  ACK (80 00 00)
```

## Derleme

```bash
./gradlew :zvt-library:assembleDebug
```

## Gereksinimler

- Android SDK 36
- Kotlin 1.9.22
- Min SDK 24
