# ZVT Client Library for Android (Kotlin)

> **Available languages / Mevcut diller / Verfügbare Sprachen:**
>
> [🇬🇧 English](#-english) | [🇹🇷 Türkçe](#-türkçe) | [🇩🇪 Deutsch](#-deutsch)

---

## 🇬🇧 English

A Kotlin/Android library implementing the **ZVT Protocol (v13.13)** for communication between an Electronic Cash Register (ECR) and Payment Terminals (PT) over TCP/IP.

### What is ZVT?

ZVT (Zahlungsverkehrstechnik) is the German standard protocol for communication between point-of-sale systems and payment terminals. It is widely used in Germany, Austria, and Switzerland for card payment processing.

- **Spec**: ZVT Protocol Specification v13.13 (PA00P015_13.13_final)
- **Transport**: TCP/IP, default port **20007**
- **Encoding**: Binary APDU (Application Protocol Data Unit)

### Project Structure

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

### Supported Commands

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

### Usage

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

### Protocol Flow

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

### BMP Fields Parsed

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

### Build

```bash
./gradlew :zvt-library:assembleDebug
```

### Requirements

- Android SDK 36
- Kotlin 1.9.22
- Min SDK 24

---

## 🇹🇷 Türkçe

Android için **ZVT Protokolü (v13.13)** uygulayan bir Kotlin kütüphanesi. Yazar Kasa (ECR) ile Ödeme Terminalleri (PT) arasında TCP/IP üzerinden iletişim sağlar.

### ZVT Nedir?

ZVT (Zahlungsverkehrstechnik), satış noktası (POS) sistemleri ile ödeme terminalleri arasındaki iletişimi sağlayan Alman standart protokolüdür. Almanya, Avusturya ve İsviçre'de kartlı ödeme işlemlerinde yaygın olarak kullanılır.

- **Spesifikasyon**: ZVT Protocol Specification v13.13 (PA00P015_13.13_final)
- **İletişim**: TCP/IP, varsayılan port **20007**
- **Kodlama**: Binary APDU (Application Protocol Data Unit)

### Proje Yapısı

```
zvt-project/
├── app/                          # Demo/test Android uygulaması
├── zvt-library/                  # ZVT protokol kütüphanesi (yeniden kullanılabilir)
│   └── src/main/java/com/erkan/zvt/
│       ├── ZvtClient.kt          # Ana istemci (TCP bağlantı, komut yürütme)
│       ├── ZvtCallback.kt        # Olay dinleyici arayüzü
│       ├── model/
│       │   └── Models.kt         # Veri modelleri (TransactionResult, CardData, vb.)
│       ├── protocol/
│       │   ├── ZvtConstants.kt   # Tüm protokol sabitleri (komutlar, BMP'ler, hata kodları)
│       │   ├── ZvtPacket.kt      # APDU paket serializasyon/deserializasyon
│       │   ├── ZvtCommandBuilder.kt  # Komut oluşturucular (Registration, Authorization, vb.)
│       │   └── ZvtResponseParser.kt  # Yanıt ayrıştırıcı (BMP alanları, TLV containerları)
│       └── util/
│           ├── TlvParser.kt      # TLV (Tag-Length-Value) parser/builder
│           ├── BcdHelper.kt      # BCD kodlama/çözme yardımcıları
│           └── ByteExtensions.kt # Byte dizisi uzantı fonksiyonları
└── gradle/
    └── libs.versions.toml        # Merkezi bağımlılık yönetimi
```

### Desteklenen Komutlar

| Komut | Kod | Açıklama |
|-------|-----|----------|
| Registration | `06 00` | ECR'yi terminale kaydet |
| Authorization | `06 01` | Ödeme işlemi |
| Log Off | `06 02` | Terminal bağlantısını sonlandır |
| Pre-Authorization | `06 22` | Ön yetkilendirme (otel, araç kiralama) |
| Reversal | `06 30` | Önceki işlemi iptal et |
| Refund | `06 31` | İade işlemi |
| End of Day | `06 50` | Gün sonu kapanışı |
| Diagnosis | `06 70` | Terminal durumunu sorgula |
| Status Enquiry | `05 01` | Terminal durumunu kontrol et |

### Kullanım

```kotlin
val config = ZvtConfig(
    host = "192.168.1.100",
    port = 20007,
    password = "000000",
    currencyCode = 978, // EUR
    debugMode = true
)

val client = ZvtClient(config)

// Bağlan ve kayıt ol
client.connect()
client.register(configByte = ZvtConstants.REG_INTERMEDIATE_STATUS)

// Ödeme yap (12.50 EUR)
val result = client.authorize(amountInCents = 1250)
if (result.success) {
    println("Ödeme başarılı! Trace: ${result.traceNumber}")
    println("Kart: ${result.cardData?.cardType}")
} else {
    println("Ödeme başarısız: ${result.resultMessage}")
}

// Gün sonu
val eod = client.endOfDay()

// Bağlantıyı kapat
client.disconnect()
```

### Derleme

```bash
./gradlew :zvt-library:assembleDebug
```

### Gereksinimler

- Android SDK 36
- Kotlin 1.9.22
- Min SDK 24

---

## 🇩🇪 Deutsch

Eine Kotlin/Android-Bibliothek, die das **ZVT-Protokoll (v13.13)** für die Kommunikation zwischen einer Elektronischen Registrierkasse (ECR) und Zahlungsterminals (PT) über TCP/IP implementiert.

### Was ist ZVT?

ZVT (Zahlungsverkehrstechnik) ist das deutsche Standardprotokoll für die Kommunikation zwischen Kassensystemen und Zahlungsterminals. Es wird in Deutschland, Österreich und der Schweiz häufig für die Kartenzahlungsabwicklung eingesetzt.

- **Spezifikation**: ZVT Protocol Specification v13.13 (PA00P015_13.13_final)
- **Transport**: TCP/IP, Standardport **20007**
- **Kodierung**: Binäre APDU (Application Protocol Data Unit)

### Projektstruktur

```
zvt-project/
├── app/                          # Demo-/Test-Android-Anwendung
├── zvt-library/                  # ZVT-Protokollbibliothek (wiederverwendbar)
│   └── src/main/java/com/erkan/zvt/
│       ├── ZvtClient.kt          # Haupt-Client (TCP-Verbindung, Befehlsausführung)
│       ├── ZvtCallback.kt        # Ereignis-Listener-Schnittstelle
│       ├── model/
│       │   └── Models.kt         # Datenmodelle (TransactionResult, CardData, usw.)
│       ├── protocol/
│       │   ├── ZvtConstants.kt   # Alle Protokollkonstanten (Befehle, BMPs, Fehlercodes)
│       │   ├── ZvtPacket.kt      # APDU-Paket-Serialisierung/Deserialisierung
│       │   ├── ZvtCommandBuilder.kt  # Befehlsgeneratoren (Registration, Authorization, usw.)
│       │   └── ZvtResponseParser.kt  # Antwort-Parser (BMP-Felder, TLV-Container)
│       └── util/
│           ├── TlvParser.kt      # TLV (Tag-Length-Value) Parser/Builder
│           ├── BcdHelper.kt      # BCD-Kodierung/Dekodierung
│           └── ByteExtensions.kt # Byte-Array-Erweiterungsfunktionen
└── gradle/
    └── libs.versions.toml        # Zentrale Abhängigkeitsverwaltung
```

### Unterstützte Befehle

| Befehl | Code | Beschreibung |
|--------|------|--------------|
| Registration | `06 00` | ECR am Terminal registrieren |
| Authorization | `06 01` | Zahlungstransaktion |
| Log Off | `06 02` | Verbindung zum Terminal trennen |
| Pre-Authorization | `06 22` | Vorautorisierung (Hotel, Mietwagen) |
| Reversal | `06 30` | Vorherige Transaktion stornieren |
| Refund | `06 31` | Erstattung durchführen |
| End of Day | `06 50` | Tagesabschluss |
| Diagnosis | `06 70` | Terminalstatus abfragen |
| Status Enquiry | `05 01` | Terminalzustand prüfen |

### Verwendung

```kotlin
val config = ZvtConfig(
    host = "192.168.1.100",
    port = 20007,
    password = "000000",
    currencyCode = 978, // EUR
    debugMode = true
)

val client = ZvtClient(config)

// Verbinden und registrieren
client.connect()
client.register(configByte = ZvtConstants.REG_INTERMEDIATE_STATUS)

// Zahlung durchführen (12,50 EUR)
val result = client.authorize(amountInCents = 1250)
if (result.success) {
    println("Zahlung erfolgreich! Trace: ${result.traceNumber}")
    println("Karte: ${result.cardData?.cardType}")
} else {
    println("Zahlung fehlgeschlagen: ${result.resultMessage}")
}

// Tagesabschluss
val eod = client.endOfDay()

// Verbindung trennen
client.disconnect()
```

### Erstellen

```bash
./gradlew :zvt-library:assembleDebug
```

### Voraussetzungen

- Android SDK 36
- Kotlin 1.9.22
- Min SDK 24

---

## License / Lisans / Lizenz

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

Bu proje MIT Lisansı ile lisanslanmıştır - detaylar için [LICENSE](LICENSE) dosyasına bakınız.

Dieses Projekt ist unter der MIT-Lizenz lizenziert - siehe [LICENSE](LICENSE) für Details.
