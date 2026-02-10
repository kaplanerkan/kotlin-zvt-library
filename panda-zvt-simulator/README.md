# Panda ZVT Terminal Simulator

> **English** | [Turkce](../README_tr.md) | [Deutsch](../README_de.md)

A pure Kotlin/JVM application that simulates a ZVT payment terminal. It behaves exactly like a real CCV A920 terminal — same binary protocol, same APDU structure, same BMP fields, same ACK/Intermediate/Completion flow.

Use this to develop and test your ZVT ECR client **without real hardware**.

## Quick Start

```bash
# Build
./gradlew.bat :panda-zvt-simulator:build

# Run (default: ZVT on port 20007, HTTP API on port 8080)
./gradlew.bat :panda-zvt-simulator:run

# Run with custom ports
./gradlew.bat :panda-zvt-simulator:run --args="--zvt-port 20007 --api-port 8080 --terminal-id 29001234"
```

On startup you'll see:

```
18:32:15 INFO  SimulatorServer - Starting ZVT Terminal Simulator...
18:32:15 INFO  SimulatorServer - Terminal ID: 29001234
18:32:15 INFO  SimulatorServer - VU Number: SIMULATOR123456
18:32:15 INFO  SimulatorServer - Card: Mastercard (6763890000001230)
18:32:16 INFO  ZvtTcpServer   - ZVT TCP server listening on port 20007
18:32:16 INFO  HttpApiServer  - HTTP API server listening on port 8080
18:32:16 INFO  SimulatorServer - Simulator ready!
```

## Connecting from Android

| Environment | Simulator IP | Why |
|-------------|-------------|-----|
| **Android Emulator** | `10.0.2.2` | Emulator maps `10.0.2.2` to host machine's `localhost` |
| **Real Android Device** | PC's LAN IP (e.g. `192.168.1.50`) | Device and PC must be on the same WiFi network |

The demo app has a **Simulator Mode** toggle that automatically sets the correct IP.

## CLI Arguments

| Argument | Default | Description |
|----------|---------|-------------|
| `--zvt-port <port>` | `20007` | ZVT binary TCP port |
| `--api-port <port>` | `8080` | HTTP REST API port |
| `--terminal-id <id>` | `29001234` | Simulated Terminal ID (BMP 0x29) |
| `--help`, `-h` | - | Show help |

## How It Works

The simulator listens on TCP port 20007 for ZVT binary commands and responds exactly as a real terminal would:

```
ECR -> Simulator:  Command APDU (e.g. 06 01 Authorization)
Simulator -> ECR:  ACK (80 00 00)
  ECR -> Simulator:  ACK (80 00 00)
Simulator -> ECR:  Intermediate Status (04 FF) - "Insert card"
  ECR -> Simulator:  ACK (80 00 00)
Simulator -> ECR:  Intermediate Status (04 FF) - "Enter PIN"
  ECR -> Simulator:  ACK (80 00 00)
Simulator -> ECR:  Intermediate Status (04 FF) - "Please wait"
  ECR -> Simulator:  ACK (80 00 00)
Simulator -> ECR:  Status Info (04 0F) - Full BMP data (amount, trace, card, etc.)
  ECR -> Simulator:  ACK (80 00 00)
Simulator -> ECR:  Completion (06 0F 00)
```

## Supported Commands

| Command | Hex | Handler | Response Pattern |
|---------|-----|---------|-----------------|
| Registration | `06 00` | RegistrationHandler | ACK -> StatusInfo(TID, currency) -> Completion |
| Authorization | `06 01` | AuthorizationHandler | ACK -> 3x Intermediate -> StatusInfo(full txn) -> Completion |
| Log Off | `06 02` | LogOffHandler | ACK only |
| Repeat Receipt | `06 20` | RepeatReceiptHandler | ACK -> StatusInfo(last txn) -> Completion |
| Pre-Auth | `06 22` | PreAuthHandler | ACK -> 3x Intermediate -> StatusInfo(full txn) -> Completion |
| Book Total | `06 24` | BookTotalHandler | ACK -> StatusInfo -> Completion |
| Pre-Auth Reversal | `06 25` | PreAuthReversalHandler | ACK -> StatusInfo -> Completion |
| Reversal | `06 30` | ReversalHandler | ACK -> StatusInfo -> Completion |
| Refund | `06 31` | RefundHandler | ACK -> 3x Intermediate -> StatusInfo(full txn) -> Completion |
| End of Day | `06 50` | EndOfDayHandler | ACK -> Intermediate -> PrintLines -> StatusInfo -> Completion |
| Diagnosis | `06 70` | DiagnosisHandler | ACK -> StatusInfo(TID) -> Completion |
| Status Enquiry | `05 01` | StatusEnquiryHandler | ACK -> StatusInfo(TID) -> Completion |
| Abort | `06 B0` | AbortHandler | ACK -> Completion |

## Default Simulated Card

| Field | Value |
|-------|-------|
| PAN | `6763890000001230` (masked: `6763 89** **** 1230`) |
| Card Type | `6` (Mastercard) |
| Card Name | `Mastercard` |
| Expiry | `12/2028` |
| Sequence Nr | `1` |
| AID | `A000000004101001` |

All card data can be changed at runtime via the REST API.

## REST API (Management)

Base URL: `http://localhost:8080`

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/status` | Simulator status (running, registered, sessions, tx count) |
| `GET` | `/api/config` | Full current configuration |
| `PUT` | `/api/config` | Replace full configuration |
| `PUT` | `/api/error` | Configure error simulation |
| `PUT` | `/api/card` | Update simulated card data |
| `PUT` | `/api/delays` | Configure response delays |
| `GET` | `/api/transactions` | List all stored transactions |
| `GET` | `/api/transactions/last` | Get last transaction |
| `DELETE` | `/api/transactions` | Clear all transactions |
| `POST` | `/api/reset` | Full simulator reset (counters, state, transactions) |

### Examples

**Check simulator status:**
```bash
curl http://localhost:8080/api/status
```
```json
{
  "running": true,
  "registered": false,
  "busy": false,
  "activeSessions": 0,
  "transactionCount": 0,
  "currentTrace": 1,
  "currentReceipt": 1,
  "zvtPort": 20007,
  "apiPort": 8080
}
```

**Change simulated card to Visa:**
```bash
curl -X PUT http://localhost:8080/api/card \
  -H "Content-Type: application/json" \
  -d '{"pan": "4111111111111111", "cardType": 10, "cardName": "Visa"}'
```

**Enable error simulation (30% failure rate):**
```bash
curl -X PUT http://localhost:8080/api/error \
  -H "Content-Type: application/json" \
  -d '{"enabled": true, "errorPercentage": 30}'
```

**Force a specific error code:**
```bash
curl -X PUT http://localhost:8080/api/error \
  -H "Content-Type: application/json" \
  -d '{"enabled": true, "errorCode": 108}'
```

**Adjust response delays:**
```bash
curl -X PUT http://localhost:8080/api/delays \
  -H "Content-Type: application/json" \
  -d '{"intermediateDelayMs": 1000, "processingDelayMs": 2000}'
```

**View transactions:**
```bash
curl http://localhost:8080/api/transactions
```

**Reset simulator:**
```bash
curl -X POST http://localhost:8080/api/reset
```

## Configuration

### Default Config

| Setting | Default | Description |
|---------|---------|-------------|
| `zvtPort` | `20007` | ZVT TCP binary protocol port |
| `apiPort` | `8080` | HTTP REST management API port |
| `terminalId` | `29001234` | Terminal ID (BMP 0x29, 8 digits) |
| `vuNumber` | `SIMULATOR123456` | VU Number (BMP 0x2A, 15 chars) |
| `currencyCode` | `978` | Currency (978 = EUR) |

### Delays

| Setting | Default | Description |
|---------|---------|-------------|
| `ackDelayMs` | `50` | Delay before sending ACK |
| `intermediateDelayMs` | `500` | Delay between intermediate status messages |
| `processingDelayMs` | `800` | Delay simulating card processing |
| `printLineDelayMs` | `50` | Delay between print line messages |
| `betweenResponsesMs` | `100` | Delay between response packets |
| `ackTimeoutMs` | `5000` | Timeout waiting for ECR ACK |

### Error Simulation

| Setting | Default | Description |
|---------|---------|-------------|
| `enabled` | `false` | Enable random error responses |
| `errorPercentage` | `0` | Percentage of transactions that fail (0-100) |
| `forcedErrorCode` | `null` | Force a specific ZVT error code (overrides random) |

Common error codes for testing: `0x6C` (timeout), `0x64` (card not readable), `0x78` (card expired), `0xC2` (diagnosis required).

## Architecture

```
panda-zvt-simulator/
+-- src/main/kotlin/com/panda/zvt/simulator/
|   +-- Main.kt                    # Entry point, CLI arg parsing
|   +-- SimulatorServer.kt         # Orchestrator (starts TCP + HTTP)
|   +-- config/
|   |   +-- SimulatorConfig.kt     # @Serializable config data classes
|   +-- protocol/
|   |   +-- ZvtProtocolConstants.kt # Command codes, BMP tags, error codes
|   |   +-- BcdEncoder.kt          # BCD encoding/decoding
|   |   +-- LlvarEncoder.kt        # LLVAR/LLLVAR FxFy encoding
|   |   +-- ApduBuilder.kt         # Build outgoing APDUs
|   |   +-- ApduParser.kt          # Parse incoming ECR commands
|   |   +-- BmpFieldBuilder.kt     # Build BMP fields for responses
|   +-- state/
|   |   +-- SimulatorState.kt      # Thread-safe counters and flags
|   |   +-- TransactionStore.kt    # In-memory transaction storage
|   +-- response/
|   |   +-- StatusInfoBuilder.kt   # Build 04 0F status info packets
|   |   +-- IntermediateStatusBuilder.kt # Build 04 FF intermediate packets
|   |   +-- PrintLineBuilder.kt    # Build 06 D1 print line packets
|   |   +-- ReceiptGenerator.kt    # Generate receipt text
|   +-- handler/
|   |   +-- CommandHandler.kt      # Handler interface
|   |   +-- CommandRouter.kt       # Routes commands to handlers
|   |   +-- *Handler.kt            # 13 command handlers
|   +-- tcp/
|   |   +-- ZvtTcpServer.kt        # Ktor TCP server (port 20007)
|   |   +-- ClientSession.kt       # Per-connection APDU read/write
|   +-- api/
|       +-- HttpApiServer.kt       # Ktor Netty HTTP server (port 8080)
|       +-- ApiRoutes.kt           # REST endpoint definitions
|       +-- ApiModels.kt           # JSON request/response models
+-- src/main/resources/
    +-- logback.xml                # Logging configuration
```

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 2.3.10 (JVM) |
| TCP Server | Ktor Network (`ktor-network`) |
| HTTP Server | Ktor Server (Netty engine) |
| Serialization | kotlinx.serialization JSON |
| Logging | SLF4J + Logback |
| Concurrency | Kotlin Coroutines |
| JVM Target | Java 17 |
