package com.panda_erkan.zvtclientdemo.ui.screen

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.panda.zvt_library.model.ConnectionState
import com.panda_erkan.zvtclientdemo.R
import com.panda_erkan.zvtclientdemo.ui.components.ProgressStatusDialog
import com.panda_erkan.zvtclientdemo.ui.components.ResultCard
import com.panda_erkan.zvtclientdemo.ui.components.ZvtButton
import com.panda_erkan.zvtclientdemo.ui.payment.PaymentViewModel
import com.panda_erkan.zvtclientdemo.ui.theme.BtnBookTotal
import com.panda_erkan.zvtclientdemo.ui.theme.BtnPartialRev
import com.panda_erkan.zvtclientdemo.ui.theme.BtnPayment
import com.panda_erkan.zvtclientdemo.ui.theme.BtnPreAuth
import com.panda_erkan.zvtclientdemo.ui.theme.BtnRefund
import com.panda_erkan.zvtclientdemo.ui.theme.BtnReversal
import com.panda_erkan.zvtclientdemo.ui.theme.Error
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaymentScreen(viewModel: PaymentViewModel = koinViewModel()) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val transactionResult by viewModel.transactionResult.collectAsStateWithLifecycle()
    val intermediateStatus by viewModel.intermediateStatus.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val receiptText by viewModel.receiptText.collectAsStateWithLifecycle()

    val registered = connectionState == ConnectionState.REGISTERED
    val enabled = registered && !isProcessing

    var amount by rememberSaveable { mutableStateOf("") }
    var receiptNo by rememberSaveable { mutableStateOf("") }

    // Hoist stringResource calls for use in onClick lambdas
    val opPaymentName = stringResource(R.string.op_payment)
    val opRefundName = stringResource(R.string.op_refund)
    val opReversalName = stringResource(R.string.op_reversal)
    val opPreAuthName = stringResource(R.string.op_pre_auth)
    val opBookTotalName = stringResource(R.string.op_book_total)
    val opPartialReversalName = stringResource(R.string.op_partial_reversal)

    // Progress dialog state
    var showProgress by remember { mutableStateOf(false) }
    var progressOperationName by remember { mutableStateOf("") }
    var progressOperationIcon by remember { mutableStateOf("") }
    var progressSuccess by remember { mutableStateOf<Boolean?>(null) }
    var progressResultMessage by remember { mutableStateOf("") }
    var progressDetails by remember { mutableStateOf("") }

    fun startOperation(name: String, icon: String) {
        progressOperationName = name
        progressOperationIcon = icon
        progressSuccess = null
        progressResultMessage = ""
        progressDetails = ""
        showProgress = true
    }

    // React to transaction results
    transactionResult?.let { result ->
        val pad = 12
        val msg = if (result.success) stringResource(R.string.transaction_success) else result.resultMessage
        val details = buildString {
            appendLine("${stringResource(R.string.label_amount).padEnd(pad)}: ${result.amountFormatted}")
            appendLine("${stringResource(R.string.label_result).padEnd(pad)}: ${result.resultMessage}")
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
        }.trimEnd()

        if (progressSuccess == null) {
            progressSuccess = result.success
            progressResultMessage = msg
            progressDetails = details
        }

        // Auto-fill receipt number
        if (result.success && result.receiptNumber > 0) {
            receiptNo = result.receiptNumber.toString()
        }
    }

    // React to error
    errorMessage?.let { error ->
        if (error.isNotEmpty() && progressSuccess == null) {
            progressSuccess = false
            progressResultMessage = error
        }
    }

    // Progress dialog
    if (showProgress) {
        ProgressStatusDialog(
            operationName = progressOperationName,
            operationIcon = progressOperationIcon,
            statusMessage = intermediateStatus,
            isProcessing = isProcessing,
            success = progressSuccess,
            resultMessage = progressResultMessage,
            resultDetails = progressDetails,
            onCancel = { viewModel.abort() },
            onDismiss = { showProgress = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Amount input
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text(stringResource(R.string.amount_eur)) },
                placeholder = { Text(stringResource(R.string.hint_amount)) },
                modifier = Modifier.weight(2f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            OutlinedTextField(
                value = receiptNo,
                onValueChange = { receiptNo = it },
                label = { Text(stringResource(R.string.hint_receipt_no)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Payment buttons
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ZvtButton(
                text = stringResource(R.string.btn_payment),
                icon = "\uD83D\uDCB3",
                containerColor = BtnPayment,
                enabled = enabled,
                onClick = {
                    if (amount.isNotEmpty()) {
                        startOperation(opPaymentName, "\uD83D\uDCB3")
                        viewModel.authorize(amount)
                    }
                }
            )
            ZvtButton(
                text = stringResource(R.string.btn_refund),
                icon = "\uD83D\uDD04",
                containerColor = BtnRefund,
                enabled = enabled,
                onClick = {
                    if (amount.isNotEmpty()) {
                        startOperation(opRefundName, "\uD83D\uDD04")
                        viewModel.refund(amount)
                    }
                }
            )
            ZvtButton(
                text = stringResource(R.string.btn_reversal),
                icon = "\u21A9\uFE0F",
                containerColor = BtnReversal,
                enabled = enabled,
                onClick = {
                    startOperation(opReversalName, "\u21A9\uFE0F")
                    viewModel.reversal(receiptNo.toIntOrNull())
                }
            )
            ZvtButton(
                text = stringResource(R.string.btn_pre_auth),
                icon = "\uD83D\uDD12",
                containerColor = BtnPreAuth,
                enabled = enabled,
                onClick = {
                    if (amount.isNotEmpty()) {
                        startOperation(opPreAuthName, "\uD83D\uDD12")
                        viewModel.preAuthorize(amount)
                    }
                }
            )
            ZvtButton(
                text = stringResource(R.string.btn_book_total),
                icon = "\u2705",
                containerColor = BtnBookTotal,
                enabled = enabled,
                onClick = {
                    val rn = receiptNo.toIntOrNull()
                    if (rn != null) {
                        val lastResult = transactionResult
                        val trace = lastResult?.traceNumber?.takeIf { it > 0 }
                        val aid = lastResult?.cardData?.aid?.takeIf { it.isNotEmpty() }
                        startOperation(opBookTotalName, "\u2705")
                        viewModel.bookTotal(amount, rn, trace, aid)
                    }
                }
            )
            ZvtButton(
                text = stringResource(R.string.btn_partial_reversal),
                icon = "\u2702\uFE0F",
                containerColor = BtnPartialRev,
                enabled = enabled,
                onClick = {
                    val rn = receiptNo.toIntOrNull()
                    if (rn != null) {
                        startOperation(opPartialReversalName, "\u2702\uFE0F")
                        viewModel.preAuthReversal(amount, rn)
                    }
                }
            )
            ZvtButton(
                text = stringResource(R.string.btn_abort),
                icon = "\u26D4",
                containerColor = Error,
                enabled = registered,
                onClick = { viewModel.abort() }
            )
        }

        Spacer(Modifier.height(16.dp))

        // Result card
        transactionResult?.let { result ->
            val pad = 12
            val details = buildString {
                appendLine("${stringResource(R.string.label_amount).padEnd(pad)}: ${result.amountFormatted}")
                appendLine("${stringResource(R.string.label_result).padEnd(pad)}: ${result.resultMessage}")
                if (result.traceNumber > 0) appendLine("${stringResource(R.string.label_trace_no).padEnd(pad)}: ${result.traceNumber}")
                if (result.receiptNumber > 0) appendLine("${stringResource(R.string.label_receipt_no).padEnd(pad)}: ${result.receiptNumber}")
                if (result.terminalId.isNotEmpty()) appendLine("${stringResource(R.string.label_terminal).padEnd(pad)}: ${result.terminalId}")
                result.cardData?.let { card ->
                    if (card.cardType.isNotEmpty()) appendLine("${stringResource(R.string.label_card_type).padEnd(pad)}: ${card.cardType}")
                    if (card.maskedPan.isNotEmpty()) appendLine("${stringResource(R.string.label_card_no).padEnd(pad)}: ${card.maskedPan}")
                }
            }.trimEnd()

            ResultCard(
                success = result.success,
                title = if (result.success) stringResource(R.string.transaction_success) else stringResource(R.string.transaction_failed),
                details = details
            )
        }

        // Error card
        errorMessage?.let { error ->
            if (error.isNotEmpty() && transactionResult == null) {
                Spacer(Modifier.height(8.dp))
                ResultCard(success = false, title = error, details = "")
            }
        }

        // Receipt
        if (receiptText.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = com.panda_erkan.zvtclientdemo.ui.theme.ReceiptBackground
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(R.string.receipt),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = receiptText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}
