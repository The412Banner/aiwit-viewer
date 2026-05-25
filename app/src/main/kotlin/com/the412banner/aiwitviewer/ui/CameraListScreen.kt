package com.the412banner.aiwitviewer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
    val refreshState = rememberPullToRefreshState()
    if (refreshState.isRefreshing) {
        LaunchedEffect(Unit) { onRefresh() }
    }
    LaunchedEffect(isLoading) {
        if (!isLoading) refreshState.endRefresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cameras") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Filled.Logout, contentDescription = "Log out")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .nestedScroll(refreshState.nestedScrollConnection),
        ) {
            when {
                isLoading && devices.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
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
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(errorText, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onRefresh) { Text("Retry") }
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(devices, key = { it.device_sn }) { d ->
                            DeviceRow(d, onClick = { onSelectDevice(d) })
                        }
                    }
                }
            }

            PullToRefreshContainer(
                state = refreshState,
                modifier = Modifier.align(Alignment.TopCenter),
            )
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
