package com.panda.zvt.simulator.api

import com.panda.zvt.simulator.config.SimulatedCardData
import com.panda.zvt.simulator.config.SimulatorConfig
import com.panda.zvt.simulator.response.ReceiptGenerator
import com.panda.zvt.simulator.state.SimulatorState
import com.panda.zvt.simulator.state.StoredTransaction
import com.panda.zvt.simulator.state.TransactionStore
import com.panda.zvt.simulator.state.toInfo
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val ERROR_NAMES = mapOf(
    0x00 to "Success",
    0x64 to "Card not readable",
    0x65 to "Card data error",
    0x66 to "Processing error",
    0x67 to "Function not permitted for card",
    0x68 to "Card expired",
    0x6A to "Card unknown",
    0x6B to "Card not accepted",
    0x6C to "Abort via timeout",
    0x78 to "Card blocked or lost",
    0xB5 to "Reversal not possible",
    0xFF to "System error"
)

fun Route.operationRoutes(
    state: SimulatorState,
    store: TransactionStore
) {
    route("/api/operations") {

        // POST /api/operations/payment
        post("/payment") {
            val config = state.config
            if (config.errorSimulation.shouldError()) {
                call.respond(errorResponse("Payment", config))
                return@post
            }
            val req = call.receive<AmountRequest>()
            val amountCents = (req.amount * 100).toLong()
            val trace = state.nextTraceNumber()
            val receipt = state.nextReceiptNumber()
            val turnover = state.nextTurnoverNumber()
            val now = LocalDateTime.now()

            store.recordTransaction(StoredTransaction(
                type = "Payment", amount = amountCents, trace = trace, receipt = receipt,
                turnover = turnover, timestamp = now, cardData = config.cardData
            ))

            call.respond(transactionResponse("Payment", amountCents, trace, receipt, turnover, now, config))
        }

        // POST /api/operations/refund
        post("/refund") {
            val config = state.config
            if (config.errorSimulation.shouldError()) {
                call.respond(errorResponse("Refund", config))
                return@post
            }
            val req = call.receive<AmountRequest>()
            val amountCents = (req.amount * 100).toLong()
            val trace = state.nextTraceNumber()
            val receipt = state.nextReceiptNumber()
            val turnover = state.nextTurnoverNumber()
            val now = LocalDateTime.now()

            store.recordTransaction(StoredTransaction(
                type = "Refund", amount = amountCents, trace = trace, receipt = receipt,
                turnover = turnover, timestamp = now, cardData = config.cardData
            ))

            call.respond(transactionResponse("Refund", amountCents, trace, receipt, turnover, now, config))
        }

        // POST /api/operations/reversal
        post("/reversal") {
            val config = state.config
            if (config.errorSimulation.shouldError()) {
                call.respond(errorResponse("Reversal", config))
                return@post
            }
            val req = call.receive<ReceiptRequest>()
            val lastTxn = store.getLastTransaction()
            val amountCents = lastTxn?.amount ?: 0L
            val trace = state.nextTraceNumber()
            val turnover = state.nextTurnoverNumber()
            val now = LocalDateTime.now()

            store.recordTransaction(StoredTransaction(
                type = "Reversal", amount = amountCents, trace = trace, receipt = req.receiptNo,
                turnover = turnover, timestamp = now, cardData = config.cardData
            ))

            call.respond(transactionResponse("Reversal", amountCents, trace, req.receiptNo, turnover, now, config))
        }

        // POST /api/operations/pre-auth
        post("/pre-auth") {
            val config = state.config
            if (config.errorSimulation.shouldError()) {
                call.respond(errorResponse("Pre-Authorization", config))
                return@post
            }
            val req = call.receive<AmountRequest>()
            val amountCents = (req.amount * 100).toLong()
            val trace = state.nextTraceNumber()
            val receipt = state.nextReceiptNumber()
            val turnover = state.nextTurnoverNumber()
            val now = LocalDateTime.now()

            store.recordTransaction(StoredTransaction(
                type = "Pre-Authorization", amount = amountCents, trace = trace, receipt = receipt,
                turnover = turnover, timestamp = now, cardData = config.cardData
            ))

            call.respond(transactionResponse("Pre-Authorization", amountCents, trace, receipt, turnover, now, config))
        }

        // POST /api/operations/book-total
        post("/book-total") {
            val config = state.config
            if (config.errorSimulation.shouldError()) {
                call.respond(errorResponse("Book Total", config))
                return@post
            }
            val req = call.receive<BookTotalRequest>()
            val amountCents = if (req.amount != null) (req.amount * 100).toLong()
                              else store.getLastTransaction()?.amount ?: 0L
            val receiptNo = req.receiptNo ?: 0
            val trace = state.nextTraceNumber()
            val turnover = state.nextTurnoverNumber()
            val now = LocalDateTime.now()

            store.recordTransaction(StoredTransaction(
                type = "Book Total", amount = amountCents, trace = trace, receipt = receiptNo,
                turnover = turnover, timestamp = now, cardData = config.cardData
            ))

            call.respond(transactionResponse("Book Total", amountCents, trace, receiptNo, turnover, now, config))
        }

        // POST /api/operations/pre-auth-reversal
        post("/pre-auth-reversal") {
            val config = state.config
            if (config.errorSimulation.shouldError()) {
                call.respond(errorResponse("Pre-Auth Reversal", config))
                return@post
            }
            val req = call.receive<ReceiptRequest>()
            val lastTxn = store.getLastTransaction()
            val amountCents = lastTxn?.amount ?: 0L
            val trace = state.nextTraceNumber()
            val turnover = state.nextTurnoverNumber()
            val now = LocalDateTime.now()

            store.recordTransaction(StoredTransaction(
                type = "Pre-Auth Reversal", amount = amountCents, trace = trace, receipt = req.receiptNo,
                turnover = turnover, timestamp = now, cardData = config.cardData
            ))

            call.respond(transactionResponse("Pre-Auth Reversal", amountCents, trace, req.receiptNo, turnover, now, config))
        }

        // POST /api/operations/end-of-day
        post("/end-of-day") {
            val config = state.config
            val transactions = store.getAllTransactions()
            val totalCents = transactions.sumOf { it.amount }
            val receiptLines = ReceiptGenerator.generateEndOfDayReceipt(store, config)
            val now = LocalDateTime.now()

            store.clearBatch()

            call.respond(OperationResponse(
                success = true,
                operation = "End of Day",
                resultCode = 0x00,
                resultMessage = "Success",
                terminalId = config.terminalId,
                timestamp = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                transactionCount = transactions.size,
                totalAmount = "%.2f EUR".format(totalCents / 100.0),
                receiptLines = receiptLines
            ))
        }

        // POST /api/operations/diagnosis
        post("/diagnosis") {
            val config = state.config
            call.respond(OperationResponse(
                success = true,
                operation = "Diagnosis",
                resultCode = 0x00,
                resultMessage = "Success",
                terminalId = config.terminalId,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ))
        }

        // POST /api/operations/status-enquiry
        post("/status-enquiry") {
            val config = state.config
            call.respond(OperationResponse(
                success = true,
                operation = "Status Enquiry",
                resultCode = 0x00,
                resultMessage = "Success",
                terminalId = config.terminalId,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ))
        }

        // POST /api/operations/repeat-receipt
        post("/repeat-receipt") {
            val config = state.config
            val lastTxn = store.getLastTransaction()
            if (lastTxn == null) {
                call.respond(HttpStatusCode.NotFound, OperationResponse(
                    success = false,
                    operation = "Repeat Receipt",
                    resultCode = 0x6D,
                    resultMessage = "No transaction to repeat",
                    terminalId = config.terminalId,
                    timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                ))
                return@post
            }
            call.respond(OperationResponse(
                success = true,
                operation = "Repeat Receipt",
                resultCode = 0x00,
                resultMessage = "Success",
                terminalId = config.terminalId,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                originalTransaction = lastTxn.toInfo()
            ))
        }

        // POST /api/operations/registration
        post("/registration") {
            state.setRegistered(true)
            val config = state.config
            call.respond(OperationResponse(
                success = true,
                operation = "Registration",
                resultCode = 0x00,
                resultMessage = "Success",
                terminalId = config.terminalId,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ))
        }

        // POST /api/operations/log-off
        post("/log-off") {
            state.setRegistered(false)
            call.respond(OperationResponse(
                success = true,
                operation = "Log Off",
                resultCode = 0x00,
                resultMessage = "Success",
                terminalId = state.config.terminalId,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ))
        }

        // POST /api/operations/abort
        post("/abort") {
            call.respond(OperationResponse(
                success = true,
                operation = "Abort",
                resultCode = 0x00,
                resultMessage = "Success",
                terminalId = state.config.terminalId,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ))
        }
    }
}

private fun SimulatedCardData.toCardInfo() = CardInfo(
    pan = pan, cardType = cardType, cardName = cardName,
    expiryDate = expiryDate, sequenceNumber = sequenceNumber, aid = aid
)

private fun transactionResponse(
    operation: String, amountCents: Long, trace: Int, receipt: Int,
    turnover: Int, now: LocalDateTime, config: SimulatorConfig
) = OperationResponse(
    success = true,
    operation = operation,
    resultCode = 0x00,
    resultMessage = "Success",
    amount = "%.2f EUR".format(amountCents / 100.0),
    amountCents = amountCents,
    trace = trace,
    receipt = receipt,
    turnover = turnover,
    terminalId = config.terminalId,
    cardData = config.cardData.toCardInfo(),
    timestamp = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
)

private fun errorResponse(operation: String, config: SimulatorConfig): OperationResponse {
    val code = config.errorSimulation.getErrorCode().toInt() and 0xFF
    return OperationResponse(
        success = false,
        operation = operation,
        resultCode = code,
        resultMessage = ERROR_NAMES[code] ?: "Error 0x${code.toString(16).uppercase()}",
        terminalId = config.terminalId,
        timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    )
}
