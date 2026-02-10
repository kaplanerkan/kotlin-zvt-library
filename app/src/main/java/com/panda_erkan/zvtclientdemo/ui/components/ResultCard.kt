package com.panda_erkan.zvtclientdemo.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.panda_erkan.zvtclientdemo.ui.theme.Error
import com.panda_erkan.zvtclientdemo.ui.theme.ErrorContainer
import com.panda_erkan.zvtclientdemo.ui.theme.Success
import com.panda_erkan.zvtclientdemo.ui.theme.SuccessContainer

@Composable
fun ResultCard(
    success: Boolean,
    title: String,
    details: String,
    modifier: Modifier = Modifier
) {
    val bgColor = if (success) SuccessContainer else ErrorContainer
    val borderColor = if (success) Success else Error
    val icon = if (success) "\u2705" else "\u274C"

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = icon, fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = borderColor
                )
            }
            if (details.isNotEmpty()) {
                Text(
                    text = details,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
