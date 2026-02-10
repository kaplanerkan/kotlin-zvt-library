package com.panda.zvt.simulator.tcp

import com.panda.zvt.simulator.handler.CommandRouter
import com.panda.zvt.simulator.protocol.ApduParser
import com.panda.zvt.simulator.state.SimulatorState
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory

class ClientSession(
    private val sessionId: Int,
    private val socket: Socket,
    private val router: CommandRouter,
    private val state: SimulatorState
) {
    private val logger = LoggerFactory.getLogger("Session-$sessionId")
    private var closed = false

    suspend fun run() {
        val readChannel = socket.openReadChannel()
        val writeChannel = socket.openWriteChannel(autoFlush = true)

        try {
            while (!closed) {
                val apdu = readApdu(readChannel) ?: break

                val cmdHex = apdu.command.joinToString(" ") { "%02X".format(it) }
                val dataHex = if (apdu.data.isNotEmpty()) {
                    " data=[${apdu.data.joinToString(" ") { "%02X".format(it) }}]"
                } else ""
                logger.info("← ECR: cmd=[$cmdHex]$dataHex")

                val responses = router.route(apdu)

                for ((index, response) in responses.withIndex()) {
                    writeChannel.writeFully(response, 0, response.size)

                    val respHex = response.joinToString(" ") { "%02X".format(it) }
                    logger.info("→ ECR: [$respHex]")

                    // Wait for ECR ACK after each response except the last one
                    if (index < responses.size - 1) {
                        val ackReceived = waitForAck(readChannel)
                        if (!ackReceived) {
                            logger.warn("ECR did not send ACK, continuing anyway")
                        }
                    }

                    // Apply configurable delay between responses
                    val delay = state.config.delays.betweenResponsesMs
                    if (delay > 0) {
                        kotlinx.coroutines.delay(delay)
                    }
                }
            }
        } catch (e: Exception) {
            if (!closed) {
                logger.info("Connection closed: ${e.message}")
            }
        } finally {
            close()
        }
    }

    private suspend fun readApdu(channel: ByteReadChannel): ApduParser.ParsedApdu? {
        return try {
            // Read command class + instruction (2 bytes)
            val cmdClass = channel.readByte().toInt() and 0xFF
            val cmdInstr = channel.readByte().toInt() and 0xFF
            val command = byteArrayOf(cmdClass.toByte(), cmdInstr.toByte())

            // Check if this is ACK (80 00) or NACK (84 xx)
            if (ApduParser.isAck(command)) {
                // ACK has one more byte (00) for length
                channel.readByte()
                return ApduParser.ParsedApdu(command, byteArrayOf(), 3)
            }

            if (ApduParser.isNack(command)) {
                // NACK: read the error byte
                val errorByte = channel.readByte()
                return ApduParser.ParsedApdu(command, byteArrayOf(errorByte), 3)
            }

            // Read length
            val lenByte = channel.readByte().toInt() and 0xFF
            val dataLength = if (lenByte == 0xFF) {
                // Extended length: next 2 bytes little-endian
                val lo = channel.readByte().toInt() and 0xFF
                val hi = channel.readByte().toInt() and 0xFF
                (hi shl 8) or lo
            } else {
                lenByte
            }

            // Read data
            val data = if (dataLength > 0) {
                val buf = ByteArray(dataLength)
                channel.readFully(buf, 0, dataLength)
                buf
            } else {
                byteArrayOf()
            }

            val headerSize = if (lenByte == 0xFF) 5 else 3
            ApduParser.ParsedApdu(command, data, headerSize + dataLength)
        } catch (e: Exception) {
            null // Connection closed or read error
        }
    }

    private suspend fun waitForAck(channel: ByteReadChannel): Boolean {
        return withTimeoutOrNull(state.config.delays.ackTimeoutMs) {
            val apdu = readApdu(channel)
            apdu != null && ApduParser.isAck(apdu.command)
        } ?: false
    }

    fun close() {
        closed = true
        try {
            socket.close()
        } catch (_: Exception) {
        }
    }
}
