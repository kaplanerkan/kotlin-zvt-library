package com.panda_erkan.zvtclientdemo.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.panda.zvt_library.model.ConnectionState
import com.panda_erkan.zvtclientdemo.R
import com.panda_erkan.zvtclientdemo.ui.components.ProgressStatusDialog
import com.panda_erkan.zvtclientdemo.ui.components.ResultCard
import com.panda_erkan.zvtclientdemo.ui.components.ZvtButton
import com.panda_erkan.zvtclientdemo.ui.terminal.TerminalViewModel
import com.panda_erkan.zvtclientdemo.ui.theme.BtnDiagnosis
import com.panda_erkan.zvtclientdemo.ui.theme.BtnEndOfDay
import com.panda_erkan.zvtclientdemo.ui.theme.BtnLogOff
import com.panda_erkan.zvtclientdemo.ui.theme.BtnRepeatReceipt
import com.panda_erkan.zvtclientdemo.ui.theme.BtnStatus
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TerminalScreen(viewModel: TerminalViewModel = koinViewModel()) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val diagnosisResult by viewModel.diagnosisResult.collectAsStateWithLifecycle()
    val endOfDayResult by viewModel.endOfDayResult.collectAsStateWithLifecycle()
    val terminalStatus by viewModel.terminalStatus.collectAsStateWithLifecycle()
    val repeatReceiptResult by viewModel.repeatReceiptResult.collectAsStateWithLifecycle()

    val registered = connectionState == ConnectionState.REGISTERED
    val enabled = registered && !isLoading

    // Hoist stringResource calls for use in onClick lambdas
    val opDiagnosisName = stringResource(R.string.op_diagnosis)
    val opStatusEnquiryName = stringResource(R.string.op_status_enquiry)
    val opEndOfDayName = stringResource(R.string.op_end_of_day)
    val opRepeatReceiptName = stringResource(R.string.op_repeat_receipt)
    val opLogOffName = stringResource(R.string.op_log_off)

    // Progress dialog state
    var showProgress by remember { mutableStateOf(false) }
    var progressOperationName by remember { mutableStateOf("") }
    var progressOperationIcon by remember { mutableStateOf("") }
    var progressSuccess by remember { mutableStateOf<Boolean?>(null) }
    var progressResultMessage by remember { mutableStateOf("") }
    var progressDetails by remember { mutableStateOf("") }

    // Track last shown result to detect new results
    var lastResultTitle by remember { mutableStateOf("") }
    var lastResultDetails by remember { mutableStateOf("") }

    fun showResultInDialog(success: Boolean, message: String, details: String) {
        progressSuccess = success
        progressResultMessage = message
        progressDetails = details
        lastResultTitle = message
        lastResultDetails = details
    }

    fun startOperation(name: String, icon: String) {
        progressOperationName = name
        progressOperationIcon = icon
        progressSuccess = null
        progressResultMessage = ""
        progressDetails = ""
        showProgress = true
    }

    // React to results
    diagnosisResult?.let { result ->
        val pad = 10
        val msg = if (result.success) stringResource(R.string.diagnosis_successful) else result.errorMessage
        val details = buildString {
            appendLine("${stringResource(R.string.label_status).padEnd(pad)}: ${if (result.success) stringResource(R.string.status_success) else stringResource(R.string.status_failed)}")
            appendLine("${stringResource(R.string.label_connection).padEnd(pad)}: ${if (result.status.isConnected) stringResource(R.string.connection_active) else stringResource(R.string.connection_lost)}")
            if (result.status.terminalId.isNotEmpty())
                appendLine("TID".padEnd(pad) + ": ${result.status.terminalId}")
            if (result.errorMessage.isNotEmpty())
                appendLine("${stringResource(R.string.label_error).padEnd(pad)}: ${result.errorMessage}")
        }.trimEnd()
        if (details != lastResultDetails) showResultInDialog(result.success, msg, details)
    }

    endOfDayResult?.let { result ->
        val pad = 10
        val msg = if (result.success) stringResource(R.string.end_of_day_successful) else result.message
        val details = buildString {
            appendLine("${stringResource(R.string.label_status).padEnd(pad)}: ${if (result.success) stringResource(R.string.status_success) else stringResource(R.string.status_failed)}")
            appendLine("${stringResource(R.string.label_message).padEnd(pad)}: ${result.message}")
            if (result.totalAmountInCents > 0) {
                val euros = result.totalAmountInCents / 100.0
                appendLine("${stringResource(R.string.label_amount).padEnd(pad)}: ${"%.2f EUR".format(euros)}")
            }
            if (result.receiptLines.isNotEmpty()) {
                appendLine("\u2500".repeat(30))
                result.receiptLines.forEach { appendLine(it) }
            }
        }.trimEnd()
        if (details != lastResultDetails) showResultInDialog(result.success, msg, details)
    }

    terminalStatus?.let { status ->
        val pad = 10
        val msg = status.statusMessage.ifEmpty { stringResource(R.string.status_success) }
        val details = buildString {
            appendLine("${stringResource(R.string.label_connection).padEnd(pad)}: ${if (status.isConnected) stringResource(R.string.connection_active) else stringResource(R.string.connection_lost)}")
            if (status.terminalId.isNotEmpty())
                appendLine("TID".padEnd(pad) + ": ${status.terminalId}")
            if (status.statusMessage.isNotEmpty())
                appendLine("${stringResource(R.string.label_message).padEnd(pad)}: ${status.statusMessage}")
        }.trimEnd()
        if (details != lastResultDetails) showResultInDialog(true, msg, details)
    }

    repeatReceiptResult?.let { result ->
        val pad = 12
        val msg = if (result.success) stringResource(R.string.repeat_receipt_result) else result.resultMessage
        val details = buildString {
            appendLine("${stringResource(R.string.label_status).padEnd(pad)}: ${if (result.success) stringResource(R.string.status_success) else stringResource(R.string.status_failed)}")
            appendLine("${stringResource(R.string.label_result).padEnd(pad)}: ${result.resultMessage}")
            if (result.amountFormatted.isNotEmpty()) appendLine("${stringResource(R.string.label_amount).padEnd(pad)}: ${result.amountFormatted}")
            if (result.traceNumber > 0) appendLine("${stringResource(R.string.label_trace_no).padEnd(pad)}: ${result.traceNumber}")
            if (result.receiptNumber > 0) appendLine("${stringResource(R.string.label_receipt_no).padEnd(pad)}: ${result.receiptNumber}")
            if (result.terminalId.isNotEmpty()) appendLine("${stringResource(R.string.label_terminal).padEnd(pad)}: ${result.terminalId}")
            if (result.vuNumber.isNotEmpty()) appendLine("${stringResource(R.string.label_vu_number).padEnd(pad)}: ${result.vuNumber}")
            result.cardData?.let { card ->
                appendLine("\u2500".repeat(30))
                if (card.cardType.isNotEmpty()) appendLine("${stringResource(R.string.label_card_type).padEnd(pad)}: ${card.cardType}")
                if (card.maskedPan.isNotEmpty()) appendLine("${stringResource(R.string.label_card_no).padEnd(pad)}: ${card.maskedPan}")
                if (card.cardName.isNotEmpty()) appendLine("${stringResource(R.string.label_card_name).padEnd(pad)}: ${card.cardName}")
                if (card.expiryDate.isNotEmpty()) appendLine("${stringResource(R.string.label_expiry).padEnd(pad)}: ${card.expiryDate}")
                if (card.sequenceNumber > 0) appendLine("${stringResource(R.string.label_seq_no).padEnd(pad)}: ${card.sequenceNumber}")
                if (card.aid.isNotEmpty()) appendLine("${stringResource(R.string.label_aid).padEnd(pad)}: ${card.aid}")
            }
            if (result.date.isNotEmpty()) {
                appendLine("\u2500".repeat(30))
                appendLine("${stringResource(R.string.label_date).padEnd(pad)}: ${result.date}")
                appendLine("${stringResource(R.string.label_time).padEnd(pad)}: ${result.time}")
            }
            if (result.receiptLines.isNotEmpty()) {
                appendLine("\u2500".repeat(30))
                result.receiptLines.forEach { appendLine(it) }
            }
        }.trimEnd()
        if (details != lastResultDetails) showResultInDialog(result.success, msg, details)
    }

    // Progress dialog
    if (showProgress) {
        ProgressStatusDialog(
            operationName = progressOperationName,
            operationIcon = progressOperationIcon,
            statusMessage = statusMessage,
            isProcessing = isLoading,
            success = progressSuccess,
            resultMessage = progressResultMessage,
            resultDetails = progressDetails,
            onCancel = null,
            onDismiss = { showProgress = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.terminal_operations),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ZvtButton(
                text = stringResource(R.string.btn_diagnosis),
                icon = "\uD83D\uDCCA",
                containerColor = BtnDiagnosis,
                enabled = enabled,
                onClick = {
                    startOperation(opDiagnosisName, "\uD83D\uDCCA")
                    viewModel.diagnosis()
                }
            )
            ZvtButton(
                text = stringResource(R.string.btn_status_enquiry),
                icon = "\uD83D\uDD0D",
                containerColor = BtnStatus,
                enabled = enabled,
                onClick = {
                    startOperation(opStatusEnquiryName, "\uD83D\uDD0D")
                    viewModel.statusEnquiry()
                }
            )
            ZvtButton(
                text = stringResource(R.string.btn_end_of_day),
                icon = "\uD83D\uDCC5",
                containerColor = BtnEndOfDay,
                enabled = enabled,
                onClick = {
                    startOperation(opEndOfDayName, "\uD83D\uDCC5")
                    viewModel.endOfDay()
                }
            )
            ZvtButton(
                text = stringResource(R.string.btn_repeat_receipt),
                icon = "\uD83D\uDDA8\uFE0F",
                containerColor = BtnRepeatReceipt,
                enabled = enabled,
                onClick = {
                    startOperation(opRepeatReceiptName, "\uD83D\uDDA8\uFE0F")
                    viewModel.repeatReceipt()
                }
            )
            ZvtButton(
                text = stringResource(R.string.btn_log_off),
                icon = "\uD83D\uDEAA",
                containerColor = BtnLogOff,
                enabled = enabled,
                onClick = {
                    startOperation(opLogOffName, "\uD83D\uDEAA")
                    viewModel.logOff()
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        // Show last result card below buttons
        if (lastResultTitle.isNotEmpty()) {
            ResultCard(
                success = progressSuccess ?: false,
                title = lastResultTitle,
                details = lastResultDetails
            )
        }
    }
}
