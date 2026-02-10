package com.panda.zvt.simulator

import com.panda.zvt.simulator.config.SimulatorConfig
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    val config = parseArgs(args)
    val server = SimulatorServer(config)

    Runtime.getRuntime().addShutdownHook(Thread {
        server.stop()
    })

    runBlocking {
        server.start()
    }

    // Keep main thread alive
    Thread.currentThread().join()
}

private fun parseArgs(args: Array<String>): SimulatorConfig {
    var config = SimulatorConfig()

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--zvt-port" -> {
                config = config.copy(zvtPort = args[++i].toInt())
            }
            "--api-port" -> {
                config = config.copy(apiPort = args[++i].toInt())
            }
            "--terminal-id" -> {
                config = config.copy(terminalId = args[++i])
            }
            "--help", "-h" -> {
                println("""
                    ZVT Terminal Simulator

                    Usage: panda-zvt-simulator [options]

                    Options:
                      --zvt-port <port>      ZVT TCP port (default: 20007)
                      --api-port <port>      HTTP API port (default: 8080)
                      --terminal-id <id>     Terminal ID (default: 29000065)
                      --help, -h             Show this help
                """.trimIndent())
                System.exit(0)
            }
            else -> {
                System.err.println("Unknown argument: ${args[i]}")
                System.exit(1)
            }
        }
        i++
    }

    return config
}
