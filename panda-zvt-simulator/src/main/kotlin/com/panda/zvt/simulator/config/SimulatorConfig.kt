package com.panda.zvt.simulator.config

import kotlinx.serialization.Serializable

@Serializable
data class SimulatorConfig(
    val zvtPort: Int = 20007,
    val apiPort: Int = 8080,
    val terminalId: String = "29001234",
    val vuNumber: String = "SIMULATOR123456",
    val currencyCode: Int = 978,
    val delays: SimulatorDelays = SimulatorDelays(),
    val errorSimulation: ErrorSimulation = ErrorSimulation(),
    val cardData: SimulatedCardData = SimulatedCardData()
)

@Serializable
data class SimulatorDelays(
    val ackDelayMs: Long = 50,
    val intermediateDelayMs: Long = 500,
    val processingDelayMs: Long = 800,
    val printLineDelayMs: Long = 50,
    val betweenResponsesMs: Long = 100,
    val ackTimeoutMs: Long = 5000
)

@Serializable
data class ErrorSimulation(
    val enabled: Boolean = false,
    val errorPercentage: Int = 0,
    val forcedErrorCode: Int? = null
) {
    fun shouldError(): Boolean {
        if (!enabled) return false
        if (forcedErrorCode != null) return true
        if (errorPercentage <= 0) return false
        return (1..100).random() <= errorPercentage
    }

    fun getErrorCode(): Byte =
        forcedErrorCode?.toByte() ?: 0x6C.toByte()
}

@Serializable
data class SimulatedCardData(
    val pan: String = "6763890000001230",
    val cardType: Int = 6,
    val cardName: String = "Mastercard",
    val expiryDate: String = "2812",
    val sequenceNumber: Int = 1,
    val aid: String = "A000000004101001"
)
