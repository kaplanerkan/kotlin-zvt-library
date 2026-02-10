package com.panda.zvt.simulator.tcp

import com.panda.zvt.simulator.handler.CommandRouter
import com.panda.zvt.simulator.state.SimulatorState
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class ZvtTcpServer(
    private val state: SimulatorState,
    private val router: CommandRouter
) {
    private val logger = LoggerFactory.getLogger(ZvtTcpServer::class.java)
    private val sessionCounter = AtomicInteger(0)
    private val activeSessions = ConcurrentHashMap<Int, ClientSession>()

    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null

    val activeSessionCount: Int get() = activeSessions.size

    suspend fun start(scope: CoroutineScope) {
        val port = state.config.zvtPort
        val selectorManager = SelectorManager(Dispatchers.IO)

        serverSocket = aSocket(selectorManager).tcp().bind("0.0.0.0", port)
        logger.info("ZVT TCP server listening on port $port")

        acceptJob = scope.launch {
            while (isActive) {
                try {
                    val socket = serverSocket!!.accept()
                    val sessionId = sessionCounter.incrementAndGet()
                    val remoteAddress = socket.remoteAddress.toString()
                    logger.info("New connection #$sessionId from $remoteAddress")

                    val session = ClientSession(sessionId, socket, router, state)
                    activeSessions[sessionId] = session

                    launch {
                        try {
                            session.run()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            logger.error("Session #$sessionId error: ${e.message}")
                        } finally {
                            activeSessions.remove(sessionId)
                            logger.info("Session #$sessionId disconnected ($remoteAddress)")
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error("Accept error: ${e.message}")
                }
            }
        }
    }

    fun stop() {
        acceptJob?.cancel()
        activeSessions.values.forEach { it.close() }
        activeSessions.clear()
        serverSocket?.close()
        logger.info("ZVT TCP server stopped")
    }
}
