# ZVT Client Library for Android (Kotlin)

> **English** | [Turkce](README_tr.md) | [Deutsch](README_de.md)

A Kotlin/Android library implementing the **ZVT Protocol (v13.13)** for communication between an Electronic Cash Register (ECR) and Payment Terminals (PT) over TCP/IP.

## What is ZVT?

ZVT (Zahlungsverkehrstechnik) is the German standard protocol for communication between point-of-sale systems and payment terminals. It is widely used in Germany, Austria, and Switzerland for card payment processing.

- **Spec**: ZVT Protocol Specification v13.13 (PA00P015_13.13_final)
- **Transport**: TCP/IP, default port **20007**
- **Encoding**: Binary APDU (Application Protocol Data Unit)

## Features

- **Full ZVT Protocol v13.13** - 14 command types, 50+ error codes, 25+ intermediate statuses
- **Coroutine-based** - All I/O as suspend functions, no main-thread blocking
- **Broken Pipe detection** - Auto-disconnect on IOException, UI buttons disabled immediately
- **Connection state guard** - Buttons disabled until terminal is REGISTERED
- **Material 3 UI** - Turquoise theme, operation-colored buttons, bottom navigation
- **Multi-language** - English, Turkish, German (runtime switchable)
- **BCD / TLV helpers** - Complete encoding/decoding for protocol data structures
- **Thread-safe** - Synchronized send, IO dispatcher for all socket operations
- **File logging** - 30-day rolling log files with full protocol trace (TX/RX hex)
- **Koin DI** - Singleton client lifecycle, ViewModel injection

## Project Structure

```
zvt-project/
+-- app/                          # Demo Android application
+-- panda-zvt-library/            # ZVT protocol library (reusable)
|   +-- src/main/java/com/panda/zvt_library/
|       +-- ZvtClient.kt          # Main client facade (TCP connection, command execution)
|       +-- ZvtCallback.kt        # Event listener interface
|       +-- model/
|       |   +-- Models.kt         # Data models (TransactionResult, CardData, etc.)
|       +-- protocol/
|       |   +-- ZvtConstants.kt   # All protocol constants (commands, BMPs, error codes)
|       |   +-- ZvtPacket.kt      # APDU packet serialization/deserialization
|       |   +-- ZvtCommandBuilder.kt  # Command builders (Registration, Authorization, etc.)
|       |   +-- ZvtResponseParser.kt  # Response parser (BMP fields, TLV containers)
|       +-- util/
|           +-- TlvParser.kt      # TLV (Tag-Length-Value) parser/builder
|           +-- BcdHelper.kt      # BCD encoding/decoding utilities
|           +-- ByteExtensions.kt # Byte array extension functions
+-- gradle/
    +-- libs.versions.toml        # Centralized dependency management
```

## Demo App

The demo application (`app/`) is a fully functional ZVT client with Material 3 UI.

### What You Can Do

| Screen | Operations |
|--------|-----------|
| **Connection** | Connect to terminal via IP:Port, auto-register, disconnect |
| **Payment** | Payment, Refund, Reversal, Abort |
| **Pre-Auth** | Pre-Authorization, Book Total (close pre-auth), Partial Reversal |
| **Terminal** | Diagnosis, Status Enquiry, End of Day, Repeat Receipt, Log Off |
| **Log** | Live protocol log viewer with timestamps and severity levels |

### UI Features

- Operation-colored buttons (teal for payments, cyan for queries, amber for reversals, red for abort/disconnect)
- All buttons disabled until terminal is connected and registered
- Real-time intermediate status display ("Insert card", "Enter PIN", etc.)
- Transaction result card with card data, trace/receipt numbers, date/time
- Receipt printing support (print lines from terminal)
- Connection state indicator (red/green dot)
- Language switcher (EN/TR/DE) in toolbar

## ZVT Command Hex Codes

### ECR -> Terminal Commands

| Command | Hex Code | Description |
|---------|----------|-------------|
| Registration | `06 00` | Register ECR with terminal, set currency and config |
| Authorization | `06 01` | Payment transaction (amount in BCD) |
| Log Off | `06 02` | Graceful disconnect from terminal |
| Abort | `06 1E` | Cancel an ongoing operation |
| Repeat Receipt | `06 20` | Re-print last receipt |
| Pre-Authorization | `06 22` | Reserve amount (hotel, car rental) |
| Book Total | `06 24` | Complete a pre-authorization |
| Partial Reversal | `06 25` | Partially reverse a transaction |
| Reversal | `06 30` | Cancel a previous transaction |
| Refund | `06 31` | Refund a transaction |
| End of Day | `06 50` | Close daily batch |
| Diagnosis | `06 70` | Query terminal status |
| Status Enquiry | `05 01` | Check terminal state |

### Terminal -> ECR Responses

| Response | Hex Code | Description |
|----------|----------|-------------|
| ACK | `80 00 00` | Positive acknowledgement |
| NACK | `84 00 00` | Negative acknowledgement (command rejected) |
| Completion | `06 0F` | Transaction finished |
| Status Information | `04 0F` | Transaction result with BMP fields |
| Intermediate Status | `04 FF` | "Insert card", "Enter PIN", etc. |
| Print Line | `06 D1` | Single receipt line |
| Print Text Block | `06 D3` | Block of receipt text |
| Abort | `06 1E` | Transaction aborted by terminal |

### APDU Packet Format

```
[CMD_CLASS(1)] [CMD_INSTR(1)] [LEN(1-3)] [DATA(N)]

Length encoding:
  0x00-0xFE  -> 1 byte length (0-254 bytes data)
  0xFF LL HH -> 3 byte extended length (little-endian, up to 65535 bytes)

Examples:
  06 00 08 ...        -> Registration, 8 bytes data
  06 01 07 ...        -> Authorization, 7 bytes data
  80 00 00            -> ACK (no data)
  04 FF 03 ...        -> Intermediate Status, 3 bytes data
```

### Example: Registration Packet

```
TX: 06 00 08 00 00 00 08 09 78 03 00
     |  |  |  |        |  |     |  |
     |  |  |  +--------+  |     |  +-- Service byte value (0x00)
     |  |  |  Password     |     +-- BMP 0x03 (Service byte tag)
     |  |  |  (000000 BCD) |
     |  |  |               +-- Currency code (0978 = EUR, positional)
     |  |  +-- Length: 8 bytes
     |  +-- Instruction: 0x00
     +-- Class: 0x06 (Registration)
```

### Example: Authorization Packet

```
TX: 06 01 07 04 00 00 00 01 00 19 40
     |  |  |  |  |              |  |
     |  |  |  |  +--------------+  +-- Payment type EC-Cash (0x40)
     |  |  |  |  Amount: 000001 00     BMP 0x19
     |  |  |  |  = 1.00 EUR (BCD)
     |  |  |  +-- BMP 0x04 (Amount tag)
     |  |  +-- Length: 7 bytes
     +--+-- Command: 06 01 (Authorization)
```

## Result Codes (BMP 0x27)

| Code | Constant | Description |
|------|----------|-------------|
| `00` | `RC_SUCCESS` | Successful |
| `64` | `RC_CARD_NOT_READABLE` | Card not readable (LRC/parity error) |
| `65` | `RC_CARD_DATA_NOT_PRESENT` | Card data not present |
| `66` | `RC_PROCESSING_ERROR` | Processing error |
| `6C` | `RC_ABORT_VIA_TIMEOUT` | Abort via timeout or abort key |
| `6F` | `RC_WRONG_CURRENCY` | Wrong currency |
| `78` | `RC_CARD_EXPIRED` | Card expired |
| `7D` | `RC_COMMUNICATION_ERROR` | Communication error |
| `9A` | `RC_ZVT_PROTOCOL_ERROR` | ZVT protocol error |
| `A0` | `RC_RECEIVER_NOT_READY` | Receiver not ready |
| `B4` | `RC_ALREADY_REVERSED` | Already reversed |
| `B5` | `RC_REVERSAL_NOT_POSSIBLE` | Reversal not possible |
| `C2` | `RC_DIAGNOSIS_REQUIRED` | Diagnosis required |
| `C3` | `RC_MAX_AMOUNT_EXCEEDED` | Maximum amount exceeded |
| `FF` | `RC_SYSTEM_ERROR` | System error |

> Full list: 50+ result codes defined in `ZvtConstants.kt`

## Intermediate Status Codes (04 FF)

| Code | Message |
|------|---------|
| `00` | Waiting for amount confirmation |
| `01` | Please watch PIN-pad |
| `04` | Waiting for FEP response |
| `0A` | Insert card |
| `0B` | Please remove card |
| `0E` | Please wait |
| `15` | Incorrect PIN |
| `18` | PIN try limit exceeded |
| `1C` | Approved, please take goods |
| `1D` | Declined |

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

// Payment (12.50 EUR)
val result = client.authorize(amountInCents = 1250)
if (result.success) {
    println("Payment successful! Trace: ${result.traceNumber}")
    println("Card: ${result.cardData?.cardType}")
}

// Pre-Authorization flow
val preAuth = client.preAuthorize(amountInCents = 5000) // Reserve 50.00 EUR
val booking = client.bookTotal(amountInCents = 4200, receiptNumber = preAuth.receiptNumber) // Book 42.00

// Partial Reversal
val partial = client.partialReversal(amountInCents = 500, receiptNumber = 123) // Reverse 5.00

// Repeat last receipt
val receipt = client.repeatReceipt()

// End of day
val eod = client.endOfDay()

// Disconnect
client.logOff()
client.disconnect()
```

## Protocol Flow

```
ECR -> PT:  Command APDU (e.g. 06 01 for Authorization)
PT  -> ECR: ACK (80 00 00)
PT  -> ECR: Intermediate Status (04 FF) [repeated] - "Insert card", "Enter PIN"...
ECR -> PT:  ACK (80 00 00) [for each]
PT  -> ECR: Status Information (04 0F) - Transaction result with BMP fields
ECR -> PT:  ACK (80 00 00)
PT  -> ECR: Print Line (06 D1) [repeated] - Receipt lines
ECR -> PT:  ACK (80 00 00) [for each]
PT  -> ECR: Completion (06 0F) or Abort (06 1E)
ECR -> PT:  ACK (80 00 00)
```

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
2026-02-10 14:30:15.123 D/ZVT: ECR -> PT | TX Registration (06 00) | 11 bytes | 06-00-08-00-00-00-08-09-78-03-00
2026-02-10 14:30:15.456 D/ZVT: PT -> ECR | RX ACK (80 00) | 3 bytes | 80-00-00
```

If you encounter a problem, please attach the relevant log file when reporting an issue.

## Build

```bash
./gradlew :panda-zvt-library:assembleDebug   # Library only
./gradlew :app:assembleDebug                 # Demo app
```

## Requirements

- Android SDK 36
- Kotlin 1.9.22
- Min SDK 24
- JVM Target 17

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
