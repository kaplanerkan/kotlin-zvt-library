package com.panda_erkan.zvtclientdemo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.panda.zvt_library.model.ConnectionState
import com.panda_erkan.zvtclientdemo.R
import com.panda_erkan.zvtclientdemo.ui.main.MainViewModel
import com.panda_erkan.zvtclientdemo.ui.theme.Error
import com.panda_erkan.zvtclientdemo.ui.theme.Success

@Composable
fun ConnectionCard(
    mainViewModel: MainViewModel,
    onRegConfigClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connectionState by mainViewModel.connectionState.collectAsStateWithLifecycle()
    val statusMessage by mainViewModel.statusMessage.collectAsStateWithLifecycle()
    val isLoading by mainViewModel.isLoading.collectAsStateWithLifecycle()
    val simulatorRunning by mainViewModel.simulatorRunning.collectAsStateWithLifecycle()
    val simulatorStarting by mainViewModel.simulatorStarting.collectAsStateWithLifecycle()

    var host by rememberSaveable { mutableStateOf("192.168.1.135") }
    var port by rememberSaveable { mutableStateOf("20007") }
    var keepAlive by rememberSaveable { mutableStateOf(true) }
    var simulatorMode by rememberSaveable { mutableStateOf(false) }
    var savedRealHost by rememberSaveable { mutableStateOf("192.168.1.135") }
    var savedRealPort by rememberSaveable { mutableStateOf("20007") }

    val isConnected = connectionState == ConnectionState.CONNECTED
            || connectionState == ConnectionState.REGISTERED
    val isRegistered = connectionState == ConnectionState.REGISTERED

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Connection state indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            when (connectionState) {
                                ConnectionState.REGISTERED, ConnectionState.CONNECTED -> Success
                                ConnectionState.ERROR -> Error
                                else -> Color.Gray
                            }
                        )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when (connectionState) {
                        ConnectionState.DISCONNECTED -> stringResource(R.string.disconnected)
                        ConnectionState.CONNECTING, ConnectionState.REGISTERING -> stringResource(R.string.connecting)
                        ConnectionState.CONNECTED -> stringResource(R.string.connected_unregistered)
                        ConnectionState.REGISTERED -> stringResource(R.string.connected_registered)
                        ConnectionState.ERROR -> stringResource(R.string.connection_error)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (connectionState) {
                        ConnectionState.REGISTERED -> Success
                        ConnectionState.ERROR -> Error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                if (isLoading || simulatorStarting) {
                    Spacer(Modifier.width(8.dp))
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            }

            Spacer(Modifier.height(12.dp))

            // IP and Port fields
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text(stringResource(R.string.hint_terminal_ip)) },
                    modifier = Modifier.weight(2f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    textStyle = TextStyle(fontSize = 14.sp),
                    enabled = !isConnected && !isLoading
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text(stringResource(R.string.hint_port)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(fontSize = 14.sp),
                    enabled = !isConnected && !isLoading
                )
            }

            Spacer(Modifier.height(8.dp))

            // Simulator toggle + Keep-Alive
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.simulator_mode),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.width(4.dp))
                    Switch(
                        checked = simulatorMode,
                        onCheckedChange = { checked ->
                            simulatorMode = checked
                            if (checked) {
                                savedRealHost = host
                                savedRealPort = port
                                mainViewModel.startSimulator()
                                // IP will be set after simulator starts
                                host = "127.0.0.1"
                                port = "20007"
                            } else {
                                mainViewModel.stopSimulator()
                                host = savedRealHost
                                port = savedRealPort
                            }
                        },
                        enabled = !isLoading && !simulatorStarting
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.tcp_keep_alive),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.width(4.dp))
                    Switch(
                        checked = keepAlive,
                        onCheckedChange = { keepAlive = it }
                    )
                }
            }

            // Simulator hint
            if (simulatorRunning) {
                Text(
                    text = "ZVT: $host:$port",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            // Status message
            if (statusMessage.isNotEmpty()) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isConnected) {
                    Button(
                        onClick = {
                            val portInt = port.toIntOrNull() ?: 20007
                            mainViewModel.connectAndRegister(host, portInt, keepAlive = keepAlive)
                        },
                        enabled = !isLoading && !simulatorStarting && host.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.btn_connect))
                    }
                } else {
                    OutlinedButton(
                        onClick = { mainViewModel.disconnect() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Error)
                    ) {
                        Text(stringResource(R.string.btn_disconnect))
                    }
                }

                TextButton(onClick = onRegConfigClick) {
                    Text(
                        text = stringResource(R.string.btn_reg_config),
                        fontSize = 12.sp
                    )
                }
            }

            // Language switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                val langButtons = listOf("EN" to "en", "TR" to "tr", "DE" to "de")
                langButtons.forEach { (label, _) ->
                    TextButton(
                        onClick = { /* handled by Activity for AppCompat locale */ },
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
