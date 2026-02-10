package com.panda.zvt.simulator.handler

import com.panda.zvt.simulator.protocol.ApduParser

interface CommandHandler {
    suspend fun handle(apdu: ApduParser.ParsedApdu): List<ByteArray>
}
