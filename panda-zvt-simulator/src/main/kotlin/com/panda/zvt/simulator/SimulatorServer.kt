package com.panda.zvt.simulator

import com.panda.zvt.simulator.api.HttpApiServer
import com.panda.zvt.simulator.config.SimulatorConfig
import com.panda.zvt.simulator.handler.CommandRouter
import com.panda.zvt.simulator.state.SimulatorState
import com.panda.zvt.simulator.state.TransactionStore
import com.panda.zvt.simulator.tcp.ZvtTcpServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.slf4j.LoggerFactory

class SimulatorServer(config: SimulatorConfig = SimulatorConfig()) {
    private val logger = LoggerFactory.getLogger(SimulatorServer::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val state = SimulatorState(config)
    val store = TransactionStore()
    private val router = CommandRouter(state, store)
    val tcpServer = ZvtTcpServer(state, router)
    val httpServer = HttpApiServer(state, store, tcpServer)

    suspend fun start() {
        logger.info("Starting ZVT Terminal Simulator...")
        logger.info("Terminal ID: ${state.config.terminalId}")
        logger.info("VU Number: ${state.config.vuNumber}")
        logger.info("Card: ${state.config.cardData.cardName} (${state.config.cardData.pan})")

        tcpServer.start(scope)
        httpServer.start()

        logger.info("Simulator ready!")
        logger.info("  ZVT TCP: port ${state.config.zvtPort}")
        logger.info("  HTTP API: port ${state.config.apiPort}")
        logger.info("  Management: http://localhost:${state.config.apiPort}/api/status")
    }

    fun stop() {
        logger.info("Stopping simulator...")
        tcpServer.stop()
        httpServer.stop()
        logger.info("Simulator stopped")
    }
}
