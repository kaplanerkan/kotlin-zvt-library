package com.panda.zvt.simulator.api

import com.panda.zvt.simulator.state.SimulatorState
import com.panda.zvt.simulator.state.TransactionStore
import com.panda.zvt.simulator.tcp.ZvtTcpServer
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class HttpApiServer(
    private val state: SimulatorState,
    private val store: TransactionStore,
    private val tcpServer: ZvtTcpServer
) {
    private val logger = LoggerFactory.getLogger(HttpApiServer::class.java)
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null

    fun start() {
        val port = state.config.apiPort
        server = embeddedServer(Netty, port = port) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    encodeDefaults = true
                })
            }
            install(CORS) {
                anyHost()
                allowHeader(io.ktor.http.HttpHeaders.ContentType)
                allowMethod(io.ktor.http.HttpMethod.Put)
                allowMethod(io.ktor.http.HttpMethod.Delete)
                allowMethod(io.ktor.http.HttpMethod.Post)
            }
            routing {
                simulatorRoutes(state, store, tcpServer)
                operationRoutes(state, store)
            }
        }.start(wait = false)
        logger.info("HTTP API server listening on port $port")
    }

    fun stop() {
        server?.stop(1000, 2000)
        logger.info("HTTP API server stopped")
    }
}
