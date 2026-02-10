package com.panda_erkan.zvtclientdemo.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.panda_erkan.zvtclientdemo.data.model.OperationType

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val operationType: OperationType,
    val success: Boolean,
    val resultCode: Int = 0,
    val resultMessage: String = "",
    val timestamp: Long = System.currentTimeMillis(),

    // Transaction fields (nullable)
    val amountInCents: Long? = null,
    val receiptNumber: Int? = null,
    val traceNumber: Int? = null,
    val terminalId: String? = null,
    val vuNumber: String? = null,
    val transactionDate: String? = null,
    val transactionTime: String? = null,
    val originalTrace: Int? = null,
    val turnoverNumber: Int? = null,

    // Card fields (nullable)
    val maskedPan: String? = null,
    val cardType: String? = null,
    val cardName: String? = null,
    val expiryDate: String? = null,
    val cardSequenceNumber: Int? = null,
    val aid: String? = null,

    // End of Day fields (nullable)
    val transactionCount: Int? = null,
    val totalAmountInCents: Long? = null,

    // General
    val receiptLines: String? = null,
    val statusMessage: String? = null
)
