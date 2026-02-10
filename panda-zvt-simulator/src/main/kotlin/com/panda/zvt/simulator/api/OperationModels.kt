package com.panda.zvt.simulator.api

import com.panda.zvt.simulator.state.TransactionInfo
import kotlinx.serialization.Serializable

// --- Request models ---

@Serializable
data class AmountRequest(val amount: Double)

@Serializable
data class ReceiptRequest(val receiptNo: Int)

@Serializable
data class BookTotalRequest(
    val amount: Double? = null,
    val receiptNo: Int? = null
)

// --- Response models ---

@Serializable
data class OperationResponse(
    val success: Boolean,
    val operation: String,
    val resultCode: Int,
    val resultMessage: String,
    val amount: String? = null,
    val amountCents: Long? = null,
    val trace: Int? = null,
    val receipt: Int? = null,
    val turnover: Int? = null,
    val terminalId: String,
    val cardData: CardInfo? = null,
    val timestamp: String,
    val transactionCount: Int? = null,
    val totalAmount: String? = null,
    val receiptLines: List<String>? = null,
    val originalTransaction: TransactionInfo? = null
)

@Serializable
data class CardInfo(
    val pan: String,
    val cardType: Int,
    val cardName: String,
    val expiryDate: String,
    val sequenceNumber: Int,
    val aid: String
)
