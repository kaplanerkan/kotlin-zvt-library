package com.panda_erkan.zvtclientdemo.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.panda_erkan.zvtclientdemo.R
import com.panda_erkan.zvtclientdemo.repository.LogEntry
import com.panda_erkan.zvtclientdemo.repository.LogLevel
import com.panda_erkan.zvtclientdemo.ui.main.MainViewModel

@Composable
fun LogScreen(mainViewModel: MainViewModel) {
    val entries by mainViewModel.logEntries.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new entries arrive
    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header with clear button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.protocol_logs),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { mainViewModel.clearLogs() }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.btn_clear),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            items(entries, key = { "${it.timestamp}_${it.message.hashCode()}" }) { entry ->
                LogEntryItem(entry)
            }
        }
    }
}

@Composable
private fun LogEntryItem(entry: LogEntry) {
    val info = resolveLogInfo(entry)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(info.rowBg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon badge
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(info.iconColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = info.icon,
                fontSize = 14.sp,
                color = Color.White
            )
        }

        Spacer(Modifier.width(8.dp))

        // Level label
        Text(
            text = info.label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = info.iconColor,
            modifier = Modifier.width(28.dp)
        )

        Spacer(Modifier.width(4.dp))

        // Time
        Text(
            text = entry.timeFormatted,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.width(6.dp))

        // Message
        Text(
            text = entry.message,
            fontSize = 11.sp,
            color = info.textColor,
            lineHeight = 14.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

private data class LogInfo(
    val icon: String,
    val iconColor: Color,
    val textColor: Color,
    val rowBg: Color,
    val label: String
)

private fun resolveLogInfo(entry: LogEntry): LogInfo {
    val msg = entry.message

    return when {
        msg.contains("\u2713") || msg.contains("successful") || msg.contains("basarili")
                || msg.contains("erfolgreich") -> LogInfo(
            icon = "\u2713", iconColor = Color(0xFF2E7D32), textColor = Color(0xFF1B5E20),
            rowBg = Color(0x0C4CAF50), label = "OK"
        )
        msg.contains("\u25B6") || msg.contains("starting") || msg.contains("baslatiliyor")
                || msg.contains("gestartet") -> LogInfo(
            icon = "\u25B6", iconColor = Color(0xFF00897B), textColor = Color(0xFF00695C),
            rowBg = Color(0x0C00897B), label = "RUN"
        )
        entry.level == LogLevel.ERROR || msg.contains("\u2717") || msg.contains("error")
                || msg.contains("hata") || msg.contains("Fehler") -> LogInfo(
            icon = "\u2716", iconColor = Color(0xFFD32F2F), textColor = Color(0xFFC62828),
            rowBg = Color(0x0CF44336), label = "ERR"
        )
        entry.level == LogLevel.WARN -> LogInfo(
            icon = "\u26A0", iconColor = Color(0xFFEF6C00), textColor = Color(0xFFE65100),
            rowBg = Color(0x0CFF9800), label = "WRN"
        )
        msg.startsWith("Connection") || msg.startsWith("Baglanti") || msg.startsWith("Verbindung") -> LogInfo(
            icon = "\u21C5", iconColor = Color(0xFF0097A7), textColor = Color(0xFF006064),
            rowBg = Color(0x0C0097A7), label = "NET"
        )
        msg.startsWith("Terminal:") -> LogInfo(
            icon = "\u25A0", iconColor = Color(0xFF7B1FA2), textColor = Color(0xFF4A148C),
            rowBg = Color(0x0C9C27B0), label = "PT"
        )
        msg.startsWith("Print:") || msg.startsWith("Fis:") || msg.startsWith("Druck:") -> LogInfo(
            icon = "\u2399", iconColor = Color(0xFF5D4037), textColor = Color(0xFF3E2723),
            rowBg = Color(0x0C795548), label = "PRN"
        )
        msg.contains("ECR -> PT") || msg.contains("TX ") -> LogInfo(
            icon = "\u2191", iconColor = Color(0xFF1565C0), textColor = Color(0xFF0D47A1),
            rowBg = Color(0x0C2196F3), label = "TX"
        )
        msg.contains("PT -> ECR") || msg.contains("RX ") -> LogInfo(
            icon = "\u2193", iconColor = Color(0xFF00838F), textColor = Color(0xFF006064),
            rowBg = Color(0x0C00BCD4), label = "RX"
        )
        entry.level == LogLevel.DEBUG -> LogInfo(
            icon = "\u2022", iconColor = Color(0xFF757575), textColor = Color(0xFF616161),
            rowBg = Color.Transparent, label = "DBG"
        )
        else -> LogInfo(
            icon = "\u2139", iconColor = Color(0xFF00897B), textColor = Color(0xFF424242),
            rowBg = Color(0x0C00897B), label = "INF"
        )
    }
}
