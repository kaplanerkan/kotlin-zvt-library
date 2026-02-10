# ZVT Client Library for Android (Kotlin)

> **English** | [Turkce](README_tr.md) | [Deutsch](README_de.md)

[![Maven Central](https://img.shields.io/maven-central/v/io.github.kaplanerkan/panda-zvt-library)](https://central.sonatype.com/artifact/io.github.kaplanerkan/panda-zvt-library)
[![JitPack](https://jitpack.io/v/kaplanerkan/kotlin-zvt-library.svg)](https://jitpack.io/#kaplanerkan/kotlin-zvt-library)
[![GitHub Release](https://img.shields.io/github/v/release/kaplanerkan/kotlin-zvt-library)](https://github.com/kaplanerkan/kotlin-zvt-library/releases/latest)
[![Download APK](https://img.shields.io/badge/Download-Demo%20APK-brightgreen?logo=android&logoColor=white)](https://github.com/kaplanerkan/kotlin-zvt-library/releases/latest)
[![Wiki](https://img.shields.io/badge/Wiki-Documentation-blue?logo=github&logoColor=white)](https://github.com/kaplanerkan/kotlin-zvt-library/wiki)

A Kotlin/Android library implementing the **ZVT Protocol (v13.13)** for communication between an Electronic Cash Register (ECR) and Payment Terminals (PT) over TCP/IP.

## 📚 Documentation

Full documentation is available in the **[Wiki](https://github.com/kaplanerkan/kotlin-zvt-library/wiki)**:

| | Page | Content |
|---|------|---------|
| 🏠 | **[Home](https://github.com/kaplanerkan/kotlin-zvt-library/wiki)** | Features, UI guide, memory safety, troubleshooting |
| 💳 | **[Transaction Types](https://github.com/kaplanerkan/kotlin-zvt-library/wiki/Transaction-Types)** | All 7 operations, Reversal vs Refund, Pre-Auth flow, test results |
| 📡 | **[ZVT Protocol Reference](https://github.com/kaplanerkan/kotlin-zvt-library/wiki/ZVT-Protocol-Reference)** | Hex codes, APDU format, BMP fields, error codes, card types |
| 🖥️ | **[Terminal Simulator](https://github.com/kaplanerkan/kotlin-zvt-library/wiki/Simulator)** | REST API (22 endpoints), curl examples, error simulation, architecture |

> Wiki is available in **English**, **[Turkce](https://github.com/kaplanerkan/kotlin-zvt-library/wiki/Home-TR)** and **[Deutsch](https://github.com/kaplanerkan/kotlin-zvt-library/wiki/Home-DE)**.

## Installation

### Maven Central

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.kaplanerkan:panda-zvt-library:1.0.0")
}
```

### GitHub Packages

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/kaplanerkan/kotlin-zvt-library")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

// build.gradle.kts
dependencies {
    implementation("io.github.kaplanerkan:panda-zvt-library:1.0.0")
}
```

> **Note:** GitHub Packages requires authentication. Add your GitHub username and a [Personal Access Token](https://github.com/settings/tokens) (with `read:packages` scope) to your `~/.gradle/gradle.properties`:
> ```properties
> gpr.user=YOUR_GITHUB_USERNAME
> gpr.key=YOUR_GITHUB_TOKEN
> ```

### JitPack

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven("https://jitpack.io")
    }
}

// build.gradle.kts
dependencies {
    implementation("com.github.kaplanerkan:kotlin-zvt-library:v1.0.1")
}
```

## What is ZVT?

ZVT (Zahlungsverkehrstechnik) is the German standard protocol for communication between point-of-sale systems and payment terminals. It is widely used in Germany, Austria, and Switzerland for card payment processing.

- **Spec**: ZVT Protocol Specification v13.13 (PA00P015_13.13_final)
- **Transport**: TCP/IP, default port **20007**
- **Encoding**: Binary APDU (Application Protocol Data Unit)
- **Resources**: [terminalhersteller.de](https://www.terminalhersteller.de/downloads.aspx)

## Features

- **Full ZVT Protocol v13.13** - 14 command types, 50+ error codes, 25+ intermediate statuses
- **Coroutine-based** - All I/O as suspend functions, no main-thread blocking
- **Broken Pipe detection** - Auto-disconnect on IOException, UI buttons disabled immediately
- **Connection state guard** - Buttons disabled until terminal is REGISTERED
- **Jetpack Compose UI** - Material 3, turquoise theme, operation-colored buttons, bottom navigation
- **Multi-language** - English, Turkish, German (runtime switchable)
- **BCD / TLV helpers** - Complete encoding/decoding for protocol data structures
- **Thread-safe** - Synchronized send, IO dispatcher for all socket operations
- **File logging** - 30-day rolling log files with full protocol trace (TX/RX hex)
- **Koin DI** - Singleton client lifecycle, ViewModel injection

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
// Book Total requires receipt, trace, and AID from the Pre-Auth response
val booking = client.bookTotal(
    receiptNumber = preAuth.receiptNumber,
    amountInCents = 4200, // Book 42.00 EUR (optional — omit to book full amount)
    traceNumber = preAuth.traceNumber,
    aid = preAuth.cardData?.aid
)

// Pre-Auth Reversal
val reversal = client.preAuthReversal(receiptNumber = preAuth.receiptNumber) // Full reversal

// Repeat last receipt
val receipt = client.repeatReceipt()

// End of day
val eod = client.endOfDay()

// Disconnect
client.logOff()
client.disconnect()
```

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
+-- panda-zvt-simulator/          # ZVT terminal simulator (Ktor, pure JVM)
|   +-- src/main/kotlin/com/panda/zvt/simulator/
|       +-- Main.kt               # Entry point, CLI args
|       +-- SimulatorServer.kt    # Orchestrator (TCP + HTTP servers)
|       +-- protocol/             # APDU, BCD, BMP encoding (self-contained)
|       +-- handler/              # 13 command handlers (Registration, Payment, etc.)
|       +-- tcp/                  # Raw TCP server (port 20007)
|       +-- api/                  # HTTP REST management API (port 8080)
+-- gradle/
    +-- libs.versions.toml        # Centralized dependency management
```

## Screenshots

| Payment Tab | Pre-Auth Tab | Terminal Tab |
|:-----------:|:------------:|:------------:|
| <img src="screenshots/02_payment_tab.png" width="250"/> | <img src="screenshots/03_preauth_tab.png" width="250"/> | <img src="screenshots/04_terminal_tab.png" width="250"/> |

| Connection & Log | Transaction Result | Registration Config |
|:----------------:|:------------------:|:-------------------:|
| <img src="screenshots/01_connection_log.png" width="250"/> | <img src="screenshots/08_transaction_result.png" width="250"/> | <img src="screenshots/05_registration_config.png" width="250"/> |

| Simulator Mode | Real Terminal (CCV A920) |
|:--------------:|:-----------------------:|
| <img src="screenshots/09_simulator_mode.png" width="250"/> | <img src="screenshots/06_real_terminal_payment.jpeg" width="450"/> |

## Demo App

The demo application (`app/`) is a fully functional ZVT client with Jetpack Compose Material 3 UI.

| Screen | Operations |
|--------|-----------|
| **Connection** | Connect to terminal via IP:Port, auto-register, disconnect |
| **Payment** | Payment, Refund, Reversal, Abort |
| **Pre-Auth** | Pre-Authorization, Book Total (close pre-auth), Partial Reversal |
| **Terminal** | Diagnosis, Status Enquiry, End of Day, Repeat Receipt, Log Off |
| **Journals** | Transaction history with filters by operation type |
| **Log** | Live protocol log viewer with timestamps and severity levels |

## Terminal Simulator

The `panda-zvt-simulator` module simulates a ZVT payment terminal. Develop and test your ECR client **without real hardware** — same binary responses as a real CCV A920.

```bash
# Start with default settings (ZVT: 20007, HTTP API: 8080)
./gradlew :panda-zvt-simulator:run
```

The simulator provides a **ZVT TCP server** (port 20007) and an **HTTP REST API** (port 8080) with 22 endpoints for configuration, error simulation, and direct operation triggers.

> See **[Simulator Wiki](https://github.com/kaplanerkan/kotlin-zvt-library/wiki/Simulator)** for full REST API reference, curl examples, error simulation, and architecture.

## Build

```bash
./gradlew :panda-zvt-library:assembleDebug   # Library only
./gradlew :app:assembleDebug                 # Demo app
./gradlew :panda-zvt-simulator:build         # Simulator
```

## Requirements & Tech Stack

| Category | Technology | Version |
|----------|-----------|---------|
| Language | Kotlin | 2.3.10 |
| Min SDK | Android | API 24 (Android 7.0) |
| Target/Compile SDK | Android | API 36 |
| JVM Target | Java | 17 |
| Build System | Gradle (AGP) | 8.13.2 |
| UI Framework | Jetpack Compose (Material 3) | BOM 2025.06.01 |
| Architecture | MVVM | - |
| DI Framework | Koin | 4.1.1 |
| Async | Kotlin Coroutines | 1.10.2 |
| Navigation | Compose Navigation | 2.9.7 |
| Lifecycle | Jetpack Lifecycle (ViewModel, StateFlow) | 2.10.0 |
| Database | Room | 2.8.4 |
| Logging | Timber | 5.0.1 |
| Leak Detection | LeakCanary (debug only) | 2.14 |

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
