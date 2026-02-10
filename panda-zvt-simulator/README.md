# Panda ZVT Terminal Simulator

> **English** | [Turkce](../README_tr.md) | [Deutsch](../README_de.md)

A pure Kotlin/JVM application that simulates a ZVT payment terminal. It behaves exactly like a real CCV A920 terminal — same binary protocol, same APDU structure, same BMP fields, same ACK/Intermediate/Completion flow.

Use this to develop and test your ZVT ECR client **without real hardware**.

<img src="../screenshots/09_simulator_mode.png" width="300"/>

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

The demo app has a **Simulator Mode** toggle. When enabled, the Ktor simulator server starts **embedded on the Android device itself** (TCP:20007 + HTTP:8080).

| Mode | Simulator IP | HTTP API | Description |
|------|-------------|----------|-------------|
| **Embedded (toggle ON)** | Device's own IP | `http://<device_ip>:8080` | Simulator runs on the Android device |
| **Standalone (PC)** | PC's LAN IP | `http://<pc_ip>:8080` | Simulator runs as standalone JVM on PC |

When embedded mode is active, the app connects to the simulator via the device's WiFi IP. The HTTP API is accessible from any PC on the same network.

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

## REST API

Base URL: `http://localhost:8080`

### Management Endpoints

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

### Operation Endpoints

Trigger payment/terminal operations directly via HTTP — same logic as the TCP/ZVT handlers, but with JSON input/output.

| Method | Path | Body | Description |
|--------|------|------|-------------|
| `POST` | `/api/operations/payment` | `{"amount": 12.50}` | Payment (Authorization) |
| `POST` | `/api/operations/refund` | `{"amount": 12.50}` | Refund |
| `POST` | `/api/operations/reversal` | `{"receiptNo": 1}` | Reverse last transaction |
| `POST` | `/api/operations/pre-auth` | `{"amount": 50.00}` | Pre-Authorization |
| `POST` | `/api/operations/book-total` | `{"amount": 50.00, "receiptNo": 1}` | Book Total (capture) |
| `POST` | `/api/operations/pre-auth-reversal` | `{"receiptNo": 1}` | Pre-Auth Reversal |
| `POST` | `/api/operations/end-of-day` | _(empty)_ | End of Day (clears batch) |
| `POST` | `/api/operations/diagnosis` | _(empty)_ | Diagnosis |
| `POST` | `/api/operations/status-enquiry` | _(empty)_ | Status Enquiry |
| `POST` | `/api/operations/repeat-receipt` | _(empty)_ | Repeat last receipt |
| `POST` | `/api/operations/registration` | _(empty)_ | Registration |
| `POST` | `/api/operations/log-off` | _(empty)_ | Log Off |
| `POST` | `/api/operations/abort` | _(empty)_ | Abort |

### Curl Examples — All Endpoints

> Replace `localhost` with the device IP (e.g. `192.168.1.131`) when running embedded on Android.

#### Management Endpoints

**GET /api/status — Simulator status:**
```bash
curl http://localhost:8080/api/status
```
```json
{"running":true,"registered":true,"busy":false,"activeSessions":1,"transactionCount":0,"currentTrace":1,"currentReceipt":1,"zvtPort":20007,"apiPort":8080}
```

**GET /api/config — Current configuration:**
```bash
curl http://localhost:8080/api/config
```

**PUT /api/delays — Adjust response delays:**
```bash
curl -X PUT http://localhost:8080/api/delays \
  -H "Content-Type: application/json" \
  -d '{"intermediateDelayMs": 1000, "processingDelayMs": 2000}'
```

**PUT /api/card — Change simulated card to Visa:**
```bash
curl -X PUT http://localhost:8080/api/card \
  -H "Content-Type: application/json" \
  -d '{"pan": "4111111111111111", "cardType": 10, "cardName": "Visa"}'
```

**PUT /api/error — Enable error simulation (forced error code):**
```bash
curl -X PUT http://localhost:8080/api/error \
  -H "Content-Type: application/json" \
  -d '{"enabled": true, "errorCode": 104}'
```
```json
{"message":"Error simulation updated: enabled=true, rate=0%"}
```

**PUT /api/error — Enable random errors (30% failure rate):**
```bash
curl -X PUT http://localhost:8080/api/error \
  -H "Content-Type: application/json" \
  -d '{"enabled": true, "errorPercentage": 30}'
```

**PUT /api/error — Disable error simulation:**
```bash
curl -X PUT http://localhost:8080/api/error \
  -H "Content-Type: application/json" \
  -d '{"enabled": false}'
```

**GET /api/transactions — List all transactions:**
```bash
curl http://localhost:8080/api/transactions
```

**GET /api/transactions/last — Last transaction:**
```bash
curl http://localhost:8080/api/transactions/last
```

**DELETE /api/transactions — Clear all transactions:**
```bash
curl -X DELETE http://localhost:8080/api/transactions
```

**POST /api/reset — Full simulator reset:**
```bash
curl -X POST http://localhost:8080/api/reset
```

#### Operation Endpoints

**POST /api/operations/registration:**
```bash
curl -X POST http://localhost:8080/api/operations/registration
```
```json
{"success":true,"operation":"Registration","resultCode":0,"resultMessage":"Success","terminalId":"29001234","timestamp":"2026-02-10T20:08:15"}
```

**POST /api/operations/payment — Payment (Authorization):**
```bash
curl -X POST http://localhost:8080/api/operations/payment \
  -H "Content-Type: application/json" \
  -d '{"amount": 25.50}'
```
```json
{"success":true,"operation":"Payment","resultCode":0,"resultMessage":"Success","amount":"25,50 EUR","amountCents":2550,"trace":1,"receipt":1,"turnover":1,"terminalId":"29001234","cardData":{"pan":"6763890000001230","cardType":6,"cardName":"Mastercard","expiryDate":"2812","sequenceNumber":1,"aid":"A000000004101001"},"timestamp":"2026-02-10T20:08:16"}
```

**POST /api/operations/refund:**
```bash
curl -X POST http://localhost:8080/api/operations/refund \
  -H "Content-Type: application/json" \
  -d '{"amount": 5.00}'
```
```json
{"success":true,"operation":"Refund","resultCode":0,"resultMessage":"Success","amount":"5,00 EUR","amountCents":500,"trace":2,"receipt":2,"turnover":2,"terminalId":"29001234","cardData":{"pan":"6763890000001230","cardType":6,"cardName":"Mastercard",...},"timestamp":"..."}
```

**POST /api/operations/reversal:**
```bash
curl -X POST http://localhost:8080/api/operations/reversal \
  -H "Content-Type: application/json" \
  -d '{"receiptNo": 2}'
```
```json
{"success":true,"operation":"Reversal","resultCode":0,"resultMessage":"Success","amount":"5,00 EUR","amountCents":500,"trace":3,"receipt":2,"turnover":3,...}
```

**POST /api/operations/pre-auth — Pre-Authorization:**
```bash
curl -X POST http://localhost:8080/api/operations/pre-auth \
  -H "Content-Type: application/json" \
  -d '{"amount": 100.00}'
```
```json
{"success":true,"operation":"Pre-Authorization","resultCode":0,"resultMessage":"Success","amount":"100,00 EUR","amountCents":10000,"trace":4,"receipt":3,"turnover":4,...}
```

**POST /api/operations/book-total — Book Total (Capture):**
```bash
curl -X POST http://localhost:8080/api/operations/book-total \
  -H "Content-Type: application/json" \
  -d '{"amount": 85.00, "receiptNo": 3}'
```
```json
{"success":true,"operation":"Book Total","resultCode":0,"resultMessage":"Success","amount":"85,00 EUR","amountCents":8500,"trace":5,"receipt":3,"turnover":5,...}
```

**POST /api/operations/pre-auth-reversal:**
```bash
curl -X POST http://localhost:8080/api/operations/pre-auth-reversal \
  -H "Content-Type: application/json" \
  -d '{"receiptNo": 3}'
```
```json
{"success":true,"operation":"Pre-Auth Reversal","resultCode":0,"resultMessage":"Success","amount":"85,00 EUR","amountCents":8500,...}
```

**POST /api/operations/diagnosis:**
```bash
curl -X POST http://localhost:8080/api/operations/diagnosis
```
```json
{"success":true,"operation":"Diagnosis","resultCode":0,"resultMessage":"Success","terminalId":"29001234","timestamp":"..."}
```

**POST /api/operations/status-enquiry:**
```bash
curl -X POST http://localhost:8080/api/operations/status-enquiry
```
```json
{"success":true,"operation":"Status Enquiry","resultCode":0,"resultMessage":"Success","terminalId":"29001234","timestamp":"..."}
```

**POST /api/operations/repeat-receipt:**
```bash
curl -X POST http://localhost:8080/api/operations/repeat-receipt
```
```json
{"success":true,"operation":"Repeat Receipt","resultCode":0,"resultMessage":"Success","terminalId":"29001234","originalTransaction":{"type":"Pre-Auth Reversal","amountCents":8500,"amountFormatted":"85,00 EUR","trace":6,"receipt":4,...},"timestamp":"..."}
```

**POST /api/operations/end-of-day:**
```bash
curl -X POST http://localhost:8080/api/operations/end-of-day
```
```json
{"success":true,"operation":"End of Day","resultCode":0,"resultMessage":"Success","terminalId":"29001234","transactionCount":6,"totalAmount":"305,50 EUR","receiptLines":["================================","       TAGESABSCHLUSS          ","================================","Terminal: 29001234","VU:       SIMULATOR123456","Datum:    10.02.2026","--------------------------------","Anzahl:   6","Gesamt:   305,50 EUR","================================","     BATCH ABGESCHLOSSEN       ","================================"],"timestamp":"..."}
```

**POST /api/operations/log-off:**
```bash
curl -X POST http://localhost:8080/api/operations/log-off
```
```json
{"success":true,"operation":"Log Off","resultCode":0,"resultMessage":"Success","terminalId":"29001234","timestamp":"..."}
```

**POST /api/operations/abort:**
```bash
curl -X POST http://localhost:8080/api/operations/abort
```
```json
{"success":true,"operation":"Abort","resultCode":0,"resultMessage":"Success","terminalId":"29001234","timestamp":"..."}
```

#### Error Simulation Example

```bash
# Enable forced error (0x68 = card expired)
curl -X PUT http://localhost:8080/api/error \
  -H "Content-Type: application/json" \
  -d '{"enabled": true, "errorCode": 104}'

# Payment will now fail
curl -X POST http://localhost:8080/api/operations/payment \
  -H "Content-Type: application/json" \
  -d '{"amount": 10.00}'
```
```json
{"success":false,"operation":"Payment","resultCode":104,"resultMessage":"Card expired","terminalId":"29001234","timestamp":"..."}
```

```bash
# Disable error simulation
curl -X PUT http://localhost:8080/api/error \
  -H "Content-Type: application/json" \
  -d '{"enabled": false}'
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
|       +-- HttpApiServer.kt       # Ktor CIO HTTP server (port 8080)
|       +-- ApiRoutes.kt           # Management REST endpoints
|       +-- ApiModels.kt           # Management request/response models
|       +-- OperationRoutes.kt     # Operation REST endpoints (payment, refund, etc.)
|       +-- OperationModels.kt     # Operation request/response models
+-- src/main/resources/
    +-- logback.xml                # Logging configuration
```

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 2.3.10 (JVM) |
| TCP Server | Ktor Network (`ktor-network`) |
| HTTP Server | Ktor Server (CIO engine) |
| Serialization | kotlinx.serialization JSON |
| Logging | SLF4J + Logback |
| Concurrency | Kotlin Coroutines |
| JVM Target | Java 17 |
