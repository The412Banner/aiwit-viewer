package com.the412banner.aiwitviewer.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.the412banner.aiwitviewer.data.Device
import kotlinx.coroutines.flow.StateFlow

/**
 * Full-screen per-camera view. Mirrors AIWIT's behavior: tap a camera in
 * the list and you land here. Big live-preview tile on top, device-info
 * card below, then quick actions. Settings will grow as we add more
 * cmd-server property toggles in future commits.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraDetailScreen(
    device: Device,
    displayName: String,
    liveFrame: StateFlow<Bitmap?>,
    onBack: () -> Unit,
    onOpenClips: () -> Unit,
    onSnapshot: () -> Unit,
    onRequestRename: () -> Unit,
) {
    val frame by liveFrame.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(displayName)
                        Text(
                            "${device.oem.ifBlank { "?" }} • ${stateLabel(device.state)}",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // Live preview area
            Box(modifier = Modifier.padding(12.dp)) {
                LivePipBanner(
                    deviceName = displayName,
                    isOnline = device.state == 1,
                    frame = frame,
                )
            }

            // Device info card
            ElevatedCard(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Device information",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    InfoRow("Model", device.oem.ifBlank { "?" })
                    InfoRow("Firmware", device.firmware_ver.ifBlank { "?" })
                    InfoRow("MAC address", device.mac.ifBlank { "?" })
                    InfoRow("Battery", formatBattery(device.battery_level))
                    InfoRow("Time zone", device.timezone.ifBlank { "?" })
                    InfoRow("Cloud bucket", device.bucket.ifBlank { "?" })
                    InfoRow("Device SN", device.device_sn)
                }
            }

            // Settings card (placeholder — most knobs require cmd-server property
            // commands we haven't reverse-engineered yet; this card lists what
            // we currently support).
            ElevatedCard(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Live property control (PIR sensitivity, LED, motion mode, etc.) " +
                                "needs more cmd-server reverse-engineering. For now the only " +
                                "settable property is the camera's display name (local-only).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onRequestRename,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.DriveFileRenameOutline, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Rename (local)")
                    }
                }
            }

            // Actions card
            ElevatedCard(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Actions",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onOpenClips,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.PlayCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("View clips")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onSnapshot,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Snapshot now")
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            modifier = Modifier.width(108.dp),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun formatBattery(value: Int): String = when {
    value <= 0 -> "?"
    value <= 100 -> "$value%"
    value <= 1000 -> "${value / 10}.${value % 10}%"
    else -> "$value"
}

private fun stateLabel(state: Int): String = when (state) {
    0 -> "asleep"
    1 -> "online"
    else -> "state=$state"
}
