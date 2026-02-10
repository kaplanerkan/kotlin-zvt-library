# Panda ZVT Library for Android

A Kotlin/Android library implementing the ZVT Protocol (v13.13) for ECR-to-PT communication over TCP/IP.

## Features

- **Full ZVT Protocol v13.13** - 14 command types, 50+ error codes, 25+ intermediate statuses
- **Coroutine-based** - All I/O as suspend functions, no main-thread blocking
- **Broken Pipe detection** - Auto-disconnect on IOException, state transitions immediately
- **Callback mechanism** - Intermediate status, print line, connection state, error events
- **StateFlow** - Reactive connection state tracking
- **BCD / TLV helpers** - Complete encoding/decoding for protocol data structures
- **Thread-safe** - Synchronized send, IO dispatcher for all socket operations
- **Minimal dependencies** - Only `kotlinx-coroutines` and `timber`

## Supported Commands

| Command | Hex Code | Method |
|---------|----------|--------|
| Registration | `06 00` | `register()` |
| Authorization | `06 01` | `authorize()` / `authorizeEuro()` |
| Log Off | `06 02` | `logOff()` |
| Abort | `06 1E` | `abort()` |
| Repeat Receipt | `06 20` | `repeatReceipt()` |
| Pre-Authorization | `06 22` | `preAuthorize()` |
| Book Total | `06 24` | `bookTotal()` |
| Partial Reversal | `06 25` | `partialReversal()` |
| Reversal | `06 30` | `reversal()` |
| Refund | `06 31` | `refund()` |
| End of Day | `06 50` | `endOfDay()` |
| Diagnosis | `06 70` | `diagnosis()` |
| Status Enquiry | `05 01` | `statusEnquiry()` |

## Installation

### Maven Central

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.kaplanerkan:panda-zvt-library:1.0.0")
}
```

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

### Local Module

```kotlin
// settings.gradle.kts
include(":panda-zvt-library")

// app/build.gradle.kts
dependencies {
    implementation(project(":panda-zvt-library"))
}
```

## Quick Start

```kotlin
// 1. Create config
val config = ZvtConfig(
    host = "192.168.1.100",
    port = 20007,
    currencyCode = 978,  // EUR
    debugMode = true
)

// 2. Create client
val client = ZvtClient(config)

// 3. Set callback (optional)
client.setCallback(object : ZvtCallback {
    override fun onConnectionStateChanged(state: ConnectionState) {
        println("State: ${state.name}")
    }
    override fun onIntermediateStatus(status: IntermediateStatus) {
        println("Terminal: ${status.message}")
    }
    override fun onPrintLine(line: String) {
        println("Receipt: $line")
    }
    override fun onError(error: ZvtError) {
        println("Error: ${error.message}")
    }
    override fun onDebugLog(tag: String, message: String) {
        println("[$tag] $message")
    }
})

// 4. Connect and register
client.connect()
client.register()

// 5. Payment (12.50 EUR)
val result = client.authorize(1250)
if (result.success) {
    println("Payment OK: ${result.amountFormatted}")
    println("Card: ${result.cardData?.cardType}")
    println("Trace: ${result.traceNumber}")
    println("Receipt: ${result.receiptNumber}")
}

// 6. Pre-Authorization flow
val preAuth = client.preAuthorize(5000)  // Reserve 50.00 EUR
val booked = client.bookTotal(4200, preAuth.receiptNumber) // Book 42.00

// 7. Disconnect
client.logOff()
client.disconnect()
```

## Connection States

```
DISCONNECTED -> CONNECTING -> CONNECTED -> REGISTERING -> REGISTERED
                                                             |
     DISCONNECTED <-- ERROR <------ (IOException / Broken Pipe)
```

| State | Description |
|-------|-------------|
| `DISCONNECTED` | No TCP connection |
| `CONNECTING` | TCP connect in progress |
| `CONNECTED` | TCP connected, not yet registered |
| `REGISTERING` | Registration command sent |
| `REGISTERED` | Ready for operations |
| `ERROR` | Connection or protocol error |

## Project Structure

```
panda-zvt-library/
+-- src/main/java/com/panda/zvt_library/
    +-- ZvtClient.kt           # Main TCP client
    +-- ZvtCallback.kt         # Event listener interface
    +-- model/
    |   +-- Models.kt          # All data classes
    +-- protocol/
    |   +-- ZvtConstants.kt    # Protocol constants (commands, BMPs, error codes)
    |   +-- ZvtPacket.kt       # APDU packet structure
    |   +-- ZvtCommandBuilder.kt # Command builder
    |   +-- ZvtResponseParser.kt # Response parser
    +-- util/
        +-- BcdHelper.kt       # BCD encode/decode
        +-- TlvParser.kt       # TLV parse/build
        +-- ByteExtensions.kt  # Extension functions
```

## License

MIT License - Copyright (c) 2026 Erkan Kaplan
