package com.panda_erkan.zvtclientdemo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.panda_erkan.zvtclientdemo.R
import com.panda_erkan.zvtclientdemo.ui.theme.Error
import com.panda_erkan.zvtclientdemo.ui.theme.ErrorContainer
import com.panda_erkan.zvtclientdemo.ui.theme.Success
import com.panda_erkan.zvtclientdemo.ui.theme.SuccessContainer
import kotlinx.coroutines.delay

@Composable
fun ProgressStatusDialog(
    operationName: String,
    operationIcon: String,
    statusMessage: String,
    isProcessing: Boolean,
    success: Boolean?,
    resultMessage: String,
    resultDetails: String,
    onCancel: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    // Elapsed timer
    var elapsedMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isProcessing) {
        if (isProcessing) {
            elapsedMs = 0
            while (true) {
                delay(1000)
                elapsedMs += 1000
            }
        }
    }

    val minutes = (elapsedMs / 1000) / 60
    val seconds = (elapsedMs / 1000) % 60
    val timerText = String.format("%02d:%02d", minutes, seconds)

    val hasResult = success != null
    val headerColor = when {
        hasResult && success == true -> Success
        hasResult && success == false -> Error
        else -> MaterialTheme.colorScheme.primary
    }

    Dialog(
        onDismissRequest = { if (hasResult) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = hasResult,
            dismissOnClickOutside = hasResult,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x80000000)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(headerColor)
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = operationIcon, fontSize = 22.sp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = operationName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = timerText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!hasResult) {
                            // Progress state
                            CircularProgressIndicator(
                                modifier = Modifier.padding(vertical = 16.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.please_wait),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Status message card
                        if (statusMessage.isNotEmpty() && !hasResult) {
                            Spacer(Modifier.height(12.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = resolveStatusIcon(statusMessage),
                                        fontSize = 18.sp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = statusMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Result card
                        if (hasResult) {
                            val resultBg = if (success == true) SuccessContainer else ErrorContainer
                            val resultBorder = if (success == true) Success else Error
                            val resultIcon = when {
                                success == true -> "\u2705"
                                resultMessage.contains("timeout", ignoreCase = true) -> "\u23F0"
                                resultMessage.contains("abort", ignoreCase = true) -> "\u26D4"
                                else -> "\u274C"
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = resultBg),
                                border = androidx.compose.foundation.BorderStroke(1.dp, resultBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = resultIcon, fontSize = 20.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = resultMessage,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = resultBorder
                                    )
                                }
                            }

                            // Result details
                            if (resultDetails.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = resultDetails,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))

                    // Bottom button
                    if (hasResult) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.btn_ok),
                                color = if (success == true) Success else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (onCancel != null) {
                        TextButton(
                            onClick = onCancel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.btn_abort),
                                color = Error
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun resolveStatusIcon(message: String): String = when {
    message.contains("PIN", ignoreCase = true) -> "\uD83D\uDD10"
    message.contains("card", ignoreCase = true)
            || message.contains("Kart", ignoreCase = true)
            || message.contains("Karte", ignoreCase = true) -> "\uD83D\uDCB3"
    message.contains("wait", ignoreCase = true)
            || message.contains("bekle", ignoreCase = true)
            || message.contains("warten", ignoreCase = true) -> "\u23F3"
    message.contains("abort", ignoreCase = true)
            || message.contains("iptal", ignoreCase = true)
            || message.contains("Abbruch", ignoreCase = true) -> "\u26D4"
    message.contains("approved", ignoreCase = true) -> "\u2705"
    message.contains("declined", ignoreCase = true) -> "\u274C"
    message.contains("timeout", ignoreCase = true) -> "\u23F0"
    else -> "\uD83D\uDCAC"
}
