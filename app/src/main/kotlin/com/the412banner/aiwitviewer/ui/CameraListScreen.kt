package com.the412banner.aiwitviewer.ui

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.the412banner.aiwitviewer.data.Device
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraListScreen(
    devices: List<Device>,
    selectedDeviceSn: String?,
    displayNameFor: (Device) -> String,
    isLoading: Boolean,
    errorText: String?,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onSelectDevice: (Device) -> Unit,
    onOpenClips: (Device) -> Unit,
    onSnapshot: (Device) -> Unit,
    onRequestRename: (Device) -> Unit,
    liveFrameFlow: (String) -> StateFlow<Bitmap?>,
) {
    val refreshState = rememberPullToRefreshState()
    // Only kick onRefresh when the user actually pulled — don't fire on initial composition.
    LaunchedEffect(refreshState.isRefreshing) {
        if (refreshState.isRefreshing) onRefresh()
    }
    // Only end the refresh spinner when the load is done AND a refresh was active —
    // otherwise we'd call endRefresh on initial composition and leave the indicator
    // in a half-retracted "gray circle" state next time it actually fires.
    LaunchedEffect(isLoading, refreshState.isRefreshing) {
        if (!isLoading && refreshState.isRefreshing) refreshState.endRefresh()
    }

    val selected = devices.firstOrNull { it.device_sn == selectedDeviceSn }

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
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Sticky top live area — placeholder until frames arrive, then the live preview.
            Box(modifier = Modifier.padding(12.dp)) {
                if (selected == null) {
                    LivePipBannerEmpty()
                } else {
                    val frame by liveFrameFlow(selected.device_sn).collectAsState()
                    LivePipBanner(
                        deviceName = displayNameFor(selected),
                        isOnline = selected.state == 1,
                        frame = frame,
                    )
                }
            }
            HorizontalDivider()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(refreshState.nestedScrollConnection),
            ) {
                when {
                    isLoading && devices.isEmpty() -> {
                        Column(
                            Modifier.fillMaxSize(),
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
                            Modifier.padding(24.dp).fillMaxSize(),
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
                                DeviceRow(
                                    device = d,
                                    displayName = displayNameFor(d),
                                    isSelected = d.device_sn == selectedDeviceSn,
                                    onSelect = { onSelectDevice(d) },
                                    onOpenClips = { onOpenClips(d) },
                                    onSnapshot = { onSnapshot(d) },
                                    onRequestRename = { onRequestRename(d) },
                                )
                            }
                        }
                    }
                }
                PullToRefreshContainer(state = refreshState, modifier = Modifier.align(Alignment.TopCenter))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviceRow(
    device: Device,
    displayName: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onOpenClips: () -> Unit,
    onSnapshot: () -> Unit,
    onRequestRename: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val bg = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .combinedClickable(
                onClick = onSelect,
                onLongClick = { menuExpanded = true },
            ),
    ) {
        ListItem(
            leadingContent = { LivePipTile(isOnline = device.state == 1) },
            headlineContent = { Text(displayName) },
            supportingContent = {
                Text("${device.oem.ifBlank { "?" }} • fw ${device.firmware_ver.ifBlank { "?" }} • bat ${formatBattery(device.battery_level)}")
            },
            trailingContent = {
                Text(stateLabel(device.state), style = MaterialTheme.typography.labelSmall)
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("Clips") },
                leadingIcon = { Icon(Icons.Filled.PlayCircle, contentDescription = null) },
                onClick = { menuExpanded = false; onOpenClips() },
            )
            DropdownMenuItem(
                text = { Text("Snapshot now") },
                leadingIcon = { Icon(Icons.Filled.CameraAlt, contentDescription = null) },
                onClick = { menuExpanded = false; onSnapshot() },
            )
            DropdownMenuItem(
                text = { Text("Rename camera") },
                leadingIcon = { Icon(Icons.Filled.DriveFileRenameOutline, contentDescription = null) },
                onClick = { menuExpanded = false; onRequestRename() },
            )
        }
    }
    HorizontalDivider()
}

@Composable
fun RenameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (newName: String) -> Unit,
) {
    var text by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename camera") },
        text = {
            Column {
                Text(
                    "Local rename only — the AIWIT cloud copy of the name doesn't change.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Display name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
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
