package com.panda_erkan.zvtclientdemo.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.panda_erkan.zvtclientdemo.R
import com.panda_erkan.zvtclientdemo.ui.theme.Warning

private const val PREFS_NAME = "zvt_reg_config"

@Composable
fun RegistrationConfigDialog(
    onDismiss: () -> Unit,
    onSave: (configByte: Byte, tlvEnabled: Boolean) -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var receiptPayment by remember { mutableStateOf(prefs.getBoolean("receipt_payment", false)) }
    var receiptAdmin by remember { mutableStateOf(prefs.getBoolean("receipt_admin", false)) }
    var intermediateStatus by remember { mutableStateOf(prefs.getBoolean("intermediate_status", true)) }
    var allowPayment by remember { mutableStateOf(prefs.getBoolean("allow_payment", false)) }
    var allowAdmin by remember { mutableStateOf(prefs.getBoolean("allow_admin", false)) }
    var tlvSupport by remember { mutableStateOf(prefs.getBoolean("tlv_support", false)) }

    fun computeConfigByte(): Byte {
        var config = 0
        if (receiptPayment) config = config or 0x01
        if (receiptAdmin) config = config or 0x02
        if (intermediateStatus) config = config or 0x08
        if (allowPayment) config = config or 0x10
        if (allowAdmin) config = config or 0x20
        return config.toByte()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
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
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.registration_config),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "0x%02X".format(computeConfigByte().toInt() and 0xFF),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                }

                // Checkboxes
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    ConfigCheckbox(stringResource(R.string.reg_receipt_payment), receiptPayment) { receiptPayment = it }
                    ConfigCheckbox(stringResource(R.string.reg_receipt_admin), receiptAdmin) { receiptAdmin = it }
                    ConfigCheckbox(stringResource(R.string.reg_intermediate_status), intermediateStatus) { intermediateStatus = it }
                    ConfigCheckbox(stringResource(R.string.reg_allow_payment), allowPayment) { allowPayment = it }
                    ConfigCheckbox(stringResource(R.string.reg_allow_admin), allowAdmin) { allowAdmin = it }
                    ConfigCheckbox(stringResource(R.string.reg_tlv_support), tlvSupport) { tlvSupport = it }

                    Text(
                        text = stringResource(R.string.reg_tlv_warning),
                        fontSize = 10.sp,
                        color = Warning,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))

                // Save button
                Button(
                    onClick = {
                        prefs.edit()
                            .putBoolean("receipt_payment", receiptPayment)
                            .putBoolean("receipt_admin", receiptAdmin)
                            .putBoolean("intermediate_status", intermediateStatus)
                            .putBoolean("allow_payment", allowPayment)
                            .putBoolean("allow_admin", allowAdmin)
                            .putBoolean("tlv_support", tlvSupport)
                            .apply()
                        onSave(computeConfigByte(), tlvSupport)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.btn_save))
                }
            }
        }
    }
}

@Composable
private fun ConfigCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(Modifier.width(4.dp))
        Text(text = label, fontSize = 12.sp)
    }
}

fun getSavedConfigByte(context: Context): Byte {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    var config = 0
    if (prefs.getBoolean("receipt_payment", false)) config = config or 0x01
    if (prefs.getBoolean("receipt_admin", false)) config = config or 0x02
    if (prefs.getBoolean("intermediate_status", true)) config = config or 0x08
    if (prefs.getBoolean("allow_payment", false)) config = config or 0x10
    if (prefs.getBoolean("allow_admin", false)) config = config or 0x20
    return config.toByte()
}

fun isTlvEnabled(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean("tlv_support", false)
}
