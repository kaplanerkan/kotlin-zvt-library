package com.panda_erkan.zvtclientdemo.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.panda_erkan.zvtclientdemo.R
import com.panda_erkan.zvtclientdemo.data.entity.JournalEntry
import com.panda_erkan.zvtclientdemo.data.model.OperationType
import com.panda_erkan.zvtclientdemo.ui.journals.JournalsViewModel
import com.panda_erkan.zvtclientdemo.ui.theme.BtnBookTotal
import com.panda_erkan.zvtclientdemo.ui.theme.BtnDiagnosis
import com.panda_erkan.zvtclientdemo.ui.theme.BtnEndOfDay
import com.panda_erkan.zvtclientdemo.ui.theme.BtnLogOff
import com.panda_erkan.zvtclientdemo.ui.theme.BtnPartialRev
import com.panda_erkan.zvtclientdemo.ui.theme.BtnPayment
import com.panda_erkan.zvtclientdemo.ui.theme.BtnPreAuth
import com.panda_erkan.zvtclientdemo.ui.theme.BtnRefund
import com.panda_erkan.zvtclientdemo.ui.theme.BtnRepeatReceipt
import com.panda_erkan.zvtclientdemo.ui.theme.BtnReversal
import com.panda_erkan.zvtclientdemo.ui.theme.BtnStatus
import org.koin.compose.viewmodel.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalsScreen(viewModel: JournalsViewModel = koinViewModel()) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val selectedEntry by viewModel.selectedEntry.collectAsStateWithLifecycle()

    var showClearConfirm by remember { mutableStateOf(false) }

    // Clear confirmation dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            text = { Text(stringResource(R.string.journal_clear_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllEntries()
                    showClearConfirm = false
                }) {
                    Text(stringResource(R.string.btn_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.btn_close))
                }
            }
        )
    }

    // Detail dialog
    selectedEntry?.let { entry ->
        JournalDetailDialog(
            entry = entry,
            onDismiss = { viewModel.clearSelectedEntry() }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.nav_journals),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { showClearConfirm = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.btn_clear),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        // Filter chips
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { viewModel.setFilter(null) },
                label = { Text(stringResource(R.string.journal_filter_all), fontSize = 11.sp) }
            )
            val filters = listOf(
                OperationType.PAYMENT to stringResource(R.string.op_payment),
                OperationType.REFUND to stringResource(R.string.op_refund),
                OperationType.REVERSAL to stringResource(R.string.op_reversal),
                OperationType.PRE_AUTHORIZATION to stringResource(R.string.op_pre_auth),
                OperationType.END_OF_DAY to stringResource(R.string.op_end_of_day),
                OperationType.DIAGNOSIS to stringResource(R.string.op_diagnosis)
            )
            filters.forEach { (type, label) ->
                FilterChip(
                    selected = selectedFilter == type,
                    onClick = { viewModel.setFilter(type) },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = type.toComposeColor().copy(alpha = 0.2f)
                    )
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.journal_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    JournalEntryItem(
                        entry = entry,
                        onClick = { viewModel.selectEntry(entry) }
                    )
                }
            }
        }
    }
}

@Composable
private fun JournalEntryItem(
    entry: JournalEntry,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left color indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(60.dp)
                    .background(entry.operationType.toComposeColor())
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (entry.success) "\u2705" else "\u274C",
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = entry.operationType.toDisplayString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    entry.amountInCents?.let { cents ->
                        if (cents > 0) {
                            Text(
                                text = "%.2f EUR".format(cents / 100.0),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Text(
                    text = entry.resultMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val cardInfo = buildString {
                        if (!entry.cardName.isNullOrEmpty()) append(entry.cardName)
                        if (!entry.maskedPan.isNullOrEmpty()) {
                            if (isNotEmpty()) append(" \u2022 ")
                            append(entry.maskedPan)
                        }
                    }
                    if (cardInfo.isNotEmpty()) {
                        Text(
                            text = cardInfo,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val sdf = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())
                    Text(
                        text = sdf.format(Date(entry.timestamp)),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun JournalDetailDialog(
    entry: JournalEntry,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.operationType.toIcon(),
                        fontSize = 18.sp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = entry.operationType.toDisplayString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.btn_close),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                // Result badge
                val resultBg = if (entry.success)
                    com.panda_erkan.zvtclientdemo.ui.theme.SuccessContainer
                else
                    com.panda_erkan.zvtclientdemo.ui.theme.ErrorContainer
                val resultBorder = if (entry.success)
                    com.panda_erkan.zvtclientdemo.ui.theme.Success
                else
                    com.panda_erkan.zvtclientdemo.ui.theme.Error

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = resultBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, resultBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (entry.success) "\u2705" else "\u274C",
                            fontSize = 20.sp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = entry.resultMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = resultBorder
                        )
                    }
                }

                // Details
                val details = buildJournalDetails(entry)
                if (details.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = details,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(stringResource(R.string.btn_ok))
                }
            }
        }
    }
}

private fun buildJournalDetails(entry: JournalEntry): String {
    val pad = 12
    return buildString {
        entry.amountInCents?.let { cents ->
            if (cents > 0) appendLine("Amount".padEnd(pad) + ": ${"%.2f EUR".format(cents / 100.0)}")
        }
        entry.receiptNumber?.let { if (it > 0) appendLine("Receipt No".padEnd(pad) + ": $it") }
        entry.traceNumber?.let { if (it > 0) appendLine("Trace No".padEnd(pad) + ": $it") }
        entry.terminalId?.let { if (it.isNotEmpty()) appendLine("Terminal".padEnd(pad) + ": $it") }
        entry.vuNumber?.let { if (it.isNotEmpty()) appendLine("VU No".padEnd(pad) + ": $it") }

        if (!entry.cardType.isNullOrEmpty() || !entry.maskedPan.isNullOrEmpty()) {
            appendLine("\u2500".repeat(30))
            entry.cardType?.let { if (it.isNotEmpty()) appendLine("Card Type".padEnd(pad) + ": $it") }
            entry.maskedPan?.let { if (it.isNotEmpty()) appendLine("Card No".padEnd(pad) + ": $it") }
            entry.cardName?.let { if (it.isNotEmpty()) appendLine("Card Name".padEnd(pad) + ": $it") }
            entry.expiryDate?.let { if (it.isNotEmpty()) appendLine("Expiry".padEnd(pad) + ": $it") }
            entry.cardSequenceNumber?.let { if (it > 0) appendLine("Seq No".padEnd(pad) + ": $it") }
            entry.aid?.let { if (it.isNotEmpty()) appendLine("AID".padEnd(pad) + ": $it") }
        }

        entry.transactionDate?.let { date ->
            if (date.isNotEmpty()) {
                appendLine("\u2500".repeat(30))
                appendLine("Date".padEnd(pad) + ": $date")
                entry.transactionTime?.let { appendLine("Time".padEnd(pad) + ": $it") }
            }
        }

        entry.transactionCount?.let { count ->
            appendLine("\u2500".repeat(30))
            appendLine("Tx Count".padEnd(pad) + ": $count")
            entry.totalAmountInCents?.let { appendLine("Total".padEnd(pad) + ": ${"%.2f EUR".format(it / 100.0)}") }
        }

        entry.receiptLines?.let { lines ->
            if (lines.isNotEmpty()) {
                appendLine("\u2500".repeat(30))
                lines.split("|||").forEach { appendLine(it) }
            }
        }
    }.trimEnd()
}

@Composable
private fun OperationType.toDisplayString(): String = when (this) {
    OperationType.PAYMENT -> stringResource(R.string.op_payment)
    OperationType.REFUND -> stringResource(R.string.op_refund)
    OperationType.REVERSAL -> stringResource(R.string.op_reversal)
    OperationType.PRE_AUTHORIZATION -> stringResource(R.string.op_pre_auth)
    OperationType.BOOK_TOTAL -> stringResource(R.string.op_book_total)
    OperationType.PARTIAL_REVERSAL -> stringResource(R.string.op_partial_reversal)
    OperationType.END_OF_DAY -> stringResource(R.string.op_end_of_day)
    OperationType.DIAGNOSIS -> stringResource(R.string.op_diagnosis)
    OperationType.STATUS_ENQUIRY -> stringResource(R.string.op_status_enquiry)
    OperationType.REPEAT_RECEIPT -> stringResource(R.string.op_repeat_receipt)
    OperationType.LOG_OFF -> stringResource(R.string.op_log_off)
}

private fun OperationType.toIcon(): String = when (this) {
    OperationType.PAYMENT -> "\uD83D\uDCB3"
    OperationType.REFUND -> "\uD83D\uDD04"
    OperationType.REVERSAL -> "\u21A9\uFE0F"
    OperationType.PRE_AUTHORIZATION -> "\uD83D\uDD12"
    OperationType.BOOK_TOTAL -> "\u2705"
    OperationType.PARTIAL_REVERSAL -> "\u2702\uFE0F"
    OperationType.END_OF_DAY -> "\uD83D\uDCC5"
    OperationType.DIAGNOSIS -> "\uD83D\uDCCA"
    OperationType.STATUS_ENQUIRY -> "\uD83D\uDD0D"
    OperationType.REPEAT_RECEIPT -> "\uD83D\uDDA8\uFE0F"
    OperationType.LOG_OFF -> "\uD83D\uDEAA"
}

private fun OperationType.toComposeColor(): Color = when (this) {
    OperationType.PAYMENT -> BtnPayment
    OperationType.REFUND -> BtnRefund
    OperationType.REVERSAL -> BtnReversal
    OperationType.PRE_AUTHORIZATION -> BtnPreAuth
    OperationType.BOOK_TOTAL -> BtnBookTotal
    OperationType.PARTIAL_REVERSAL -> BtnPartialRev
    OperationType.END_OF_DAY -> BtnEndOfDay
    OperationType.DIAGNOSIS -> BtnDiagnosis
    OperationType.STATUS_ENQUIRY -> BtnStatus
    OperationType.REPEAT_RECEIPT -> BtnRepeatReceipt
    OperationType.LOG_OFF -> BtnLogOff
}
