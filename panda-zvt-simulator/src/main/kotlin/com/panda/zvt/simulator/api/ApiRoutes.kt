package com.panda.zvt.simulator.api

import com.panda.zvt.simulator.config.ErrorSimulation
import com.panda.zvt.simulator.config.SimulatedCardData
import com.panda.zvt.simulator.config.SimulatorConfig
import com.panda.zvt.simulator.config.SimulatorDelays
import com.panda.zvt.simulator.state.SimulatorState
import com.panda.zvt.simulator.state.TransactionStore
import com.panda.zvt.simulator.state.toInfo
import com.panda.zvt.simulator.tcp.ZvtTcpServer
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.simulatorRoutes(
    state: SimulatorState,
    store: TransactionStore,
    tcpServer: ZvtTcpServer
) {
    route("/api") {
        // GET /api/status — simulator status
        get("/status") {
            call.respond(
                StatusResponse(
                    running = true,
                    registered = state.isRegistered(),
                    busy = state.isBusy(),
                    activeSessions = tcpServer.activeSessionCount,
                    transactionCount = store.getAllTransactions().size,
                    currentTrace = state.currentTrace(),
                    currentReceipt = state.currentReceipt(),
                    zvtPort = state.config.zvtPort,
                    apiPort = state.config.apiPort
                )
            )
        }

        // GET /api/config — current config
        get("/config") {
            call.respond(state.config)
        }

        // PUT /api/config — update full config
        put("/config") {
            val newConfig = call.receive<SimulatorConfig>()
            state.updateConfig(newConfig)
            call.respond(MessageResponse("Config updated"))
        }

        // PUT /api/error — configure error simulation
        put("/error") {
            val req = call.receive<ErrorConfigRequest>()
            val current = state.config.errorSimulation
            val updated = ErrorSimulation(
                enabled = req.enabled ?: current.enabled,
                errorPercentage = req.errorPercentage ?: current.errorPercentage,
                forcedErrorCode = req.errorCode ?: current.forcedErrorCode
            )
            state.updateConfig(state.config.copy(errorSimulation = updated))
            call.respond(MessageResponse("Error simulation updated: enabled=${updated.enabled}, rate=${updated.errorPercentage}%"))
        }

        // PUT /api/card — update simulated card data
        put("/card") {
            val req = call.receive<CardDataRequest>()
            val current = state.config.cardData
            val updated = SimulatedCardData(
                pan = req.pan ?: current.pan,
                cardType = req.cardType ?: current.cardType,
                cardName = req.cardName ?: current.cardName,
                expiryDate = req.expiryDate ?: current.expiryDate,
                sequenceNumber = req.sequenceNumber ?: current.sequenceNumber,
                aid = req.aid ?: current.aid
            )
            state.updateConfig(state.config.copy(cardData = updated))
            call.respond(MessageResponse("Card data updated"))
        }

        // PUT /api/delays — configure delays
        put("/delays") {
            val req = call.receive<DelayConfigRequest>()
            val current = state.config.delays
            val updated = SimulatorDelays(
                ackDelayMs = req.ackDelayMs ?: current.ackDelayMs,
                intermediateDelayMs = req.intermediateDelayMs ?: current.intermediateDelayMs,
                processingDelayMs = req.processingDelayMs ?: current.processingDelayMs,
                printLineDelayMs = req.printLineDelayMs ?: current.printLineDelayMs,
                betweenResponsesMs = req.betweenResponsesMs ?: current.betweenResponsesMs,
                ackTimeoutMs = req.ackTimeoutMs ?: current.ackTimeoutMs
            )
            state.updateConfig(state.config.copy(delays = updated))
            call.respond(MessageResponse("Delays updated"))
        }

        // GET /api/transactions — list all transactions
        get("/transactions") {
            call.respond(store.getAllTransactions().map { it.toInfo() })
        }

        // GET /api/transactions/last — last transaction
        get("/transactions/last") {
            val last = store.getLastTransaction()
            if (last != null) {
                call.respond(last.toInfo())
            } else {
                call.respond(HttpStatusCode.NotFound, MessageResponse("No transactions"))
            }
        }

        // DELETE /api/transactions — clear all transactions
        delete("/transactions") {
            store.clearAll()
            call.respond(MessageResponse("All transactions cleared"))
        }

        // POST /api/reset — full simulator reset
        post("/reset") {
            state.reset()
            store.clearAll()
            call.respond(MessageResponse("Simulator reset"))
        }
    }
}
