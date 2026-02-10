package com.panda.zvt.simulator.api

import kotlinx.serialization.Serializable

@Serializable
data class StatusResponse(
    val running: Boolean,
    val registered: Boolean,
    val busy: Boolean,
    val activeSessions: Int,
    val transactionCount: Int,
    val currentTrace: Int,
    val currentReceipt: Int,
    val zvtPort: Int,
    val apiPort: Int
)

@Serializable
data class ErrorConfigRequest(
    val enabled: Boolean? = null,
    val errorPercentage: Int? = null,
    val errorCode: Int? = null
)

@Serializable
data class CardDataRequest(
    val pan: String? = null,
    val cardType: Int? = null,
    val cardName: String? = null,
    val expiryDate: String? = null,
    val sequenceNumber: Int? = null,
    val aid: String? = null
)

@Serializable
data class DelayConfigRequest(
    val ackDelayMs: Long? = null,
    val intermediateDelayMs: Long? = null,
    val processingDelayMs: Long? = null,
    val printLineDelayMs: Long? = null,
    val betweenResponsesMs: Long? = null,
    val ackTimeoutMs: Long? = null
)

@Serializable
data class MessageResponse(
    val message: String
)
