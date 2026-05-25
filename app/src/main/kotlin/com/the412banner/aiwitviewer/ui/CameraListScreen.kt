package com.the412banner.aiwitviewer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.the412banner.aiwitviewer.data.Device

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraListScreen(
    devices: List<Device>,
    isLoading: Boolean,
    errorText: String?,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onSelectDevice: (Device) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cameras") },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Filled.Logout, contentDescription = "Log out")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                isLoading && devices.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Loading…")
                    }
                }
                errorText != null && devices.isEmpty() -> {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    ) {
                        Text(
                            errorText,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onRefresh) { Text("Retry") }
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(devices) { d -> DeviceRow(d, onClick = { onSelectDevice(d) }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(d: Device, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(d.name.ifBlank { d.device_sn }) },
        supportingContent = {
            Text("${d.oem.ifBlank { "?" }} • fw ${d.firmware_ver.ifBlank { "?" }} • bat ${formatBattery(d.battery_level)}")
        },
        trailingContent = {
            Text(stateLabel(d.state), style = MaterialTheme.typography.labelSmall)
        },
    )
    HorizontalDivider()
}

private fun formatBattery(value: Int): String = when {
    value <= 0 -> "?"
    value <= 100 -> "$value%"
    // EKEN firmware reports battery in tenths of percent for some models (356 = 35.6%).
    value <= 1000 -> "${value / 10}.${value % 10}%"
    else -> "$value"
}

private fun stateLabel(state: Int): String = when (state) {
    0 -> "asleep"
    1 -> "online"
    else -> "state=$state"
}
