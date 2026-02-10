# ZVT Client Library for Android (Kotlin)

> 🇬🇧 **English** | [🇹🇷 Türkçe](README_tr.md) | [🇩🇪 Deutsch](README_de.md)

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
├── panda-zvt-library/                  # ZVT protocol library (reusable)
│   └── src/main/java/com/panda/zvt_library/
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

## Troubleshooting / Log Files

The application automatically writes log files to the device storage. Logs are kept for **30 days** and old files are automatically deleted.

**Log file location:**
```
Android/data/com.panda_erkan.zvtclientdemo/files/Download/logs/zvt_YYYY-MM-DD.log
```

**How to access:**
1. Connect the device to a computer via USB
2. Open the device storage and navigate to the path above
3. Or use a file manager app on the device

**Log format:**
```
2026-02-10 14:30:15.123 D/ZvtClient: [ZvtResponseParser] Parsing BMP field 0x04
2026-02-10 14:30:15.456 E/ZvtClient: Connection timeout after 5000ms
```

If you encounter a problem, please attach the relevant log file when reporting an issue.

## Build

```bash
./gradlew :panda-zvt-library:assembleDebug
```

## Requirements

- Android SDK 36
- Kotlin 1.9.22
- Min SDK 24

## License

```
MIT License

Copyright (c) 2026 Erkan Kaplan

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
