package com.the412banner.aiwitviewer.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwitchCamera
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.the412banner.aiwitviewer.data.Device
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full-screen per-camera view, AIWIT-style. Live preview is the primary
 * surface; secondary settings (rename, info, clips) live behind a
 * bottom-sheet that drops in when the user taps the settings icon.
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
    onSwitchCamera: () -> Unit,
) {
    val frame by liveFrame.collectAsState()
    var volumeOn by remember { mutableStateOf(false) }
    var isHd by remember { mutableStateOf(true) }
    var settingsOpen by remember { mutableStateOf(false) }
    var pttActive by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(displayName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { settingsOpen = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        containerColor = Color.Black,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.Black),
        ) {
            StatusRow(
                signal = "(no live yet)",
                batteryPct = formatBattery(device.battery_level),
                isHd = isHd,
                onToggleHd = { isHd = !isHd },
            )

            // Big video preview area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (frame != null) frame!!.width.toFloat() / frame!!.height.toFloat() else 4f / 3f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF101010)),
                contentAlignment = Alignment.Center,
            ) {
                val current = frame
                if (current != null) {
                    androidx.compose.foundation.Image(
                        bitmap = current.asImageBitmap(),
                        contentDescription = "Live preview of $displayName",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF888888),
                            modifier = Modifier.size(56.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Waking up…",
                            color = Color(0xFFCCCCCC),
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            if (device.state == 1) "Camera online — negotiating stream"
                            else "Camera asleep in cloud state",
                            color = Color(0xFF888888),
                            fontSize = 11.sp,
                        )
                    }
                }
                // Timestamp overlay (AIWIT-style — only visible when streaming)
                if (frame != null) {
                    Text(
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp),
                    )
                }
            }

            // Three-button action row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                CircleAction(
                    icon = Icons.Filled.CameraAlt,
                    label = "Screenshot",
                    color = Color(0xFF2A2A2A),
                    onClick = onSnapshot,
                )
                CircleAction(
                    icon = if (volumeOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                    label = "Volume",
                    color = if (volumeOn) MaterialTheme.colorScheme.primary else Color(0xFFD63B3B),
                    onClick = { volumeOn = !volumeOn },
                )
                CircleAction(
                    icon = Icons.Filled.SwitchCamera,
                    label = "Switch",
                    color = Color(0xFF2A2A2A),
                    onClick = onSwitchCamera,
                )
            }

            Spacer(Modifier.weight(1f))

            // Push-to-talk
            PushToTalk(
                active = pttActive,
                onPressChanged = { pttActive = it },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 32.dp),
            )
        }

        if (settingsOpen) {
            SettingsSheet(
                device = device,
                displayName = displayName,
                onDismiss = { settingsOpen = false },
                onOpenClips = { settingsOpen = false; onOpenClips() },
                onRequestRename = { settingsOpen = false; onRequestRename() },
            )
        }
    }
}

@Composable
private fun StatusRow(signal: String, batteryPct: String, isHd: Boolean, onToggleHd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("signal: $signal", color = Color(0xFFAAAAAA), fontSize = 11.sp)
        Spacer(Modifier.weight(1f))
        Text("bat: $batteryPct", color = Color(0xFF4CAF50), fontSize = 11.sp)
        Spacer(Modifier.width(12.dp))
        Surface(
            onClick = onToggleHd,
            shape = RoundedCornerShape(4.dp),
            color = if (isHd) Color(0xFF1976D2) else Color(0xFF333333),
            contentColor = Color.White,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.HighQuality, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(3.dp))
                Text(if (isHd) "HD" else "SD", fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun CircleAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = color,
            contentColor = Color.White,
            modifier = Modifier.size(56.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(26.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color(0xFFBBBBBB), fontSize = 11.sp)
    }
}

@Composable
private fun PushToTalk(
    active: Boolean,
    onPressChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (active) Color(0xFFD63B3B) else Color(0xFF333333)
    Box(
        modifier = modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(2.dp, Color(0xFF555555), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            onClick = { onPressChanged(!active) }, // simple toggle for now; real PTT needs gesture detector
            modifier = Modifier.fillMaxSize(),
        ) {
            Icon(
                Icons.Filled.Mic,
                contentDescription = if (active) "Stop talking" else "Hold to talk",
                tint = Color.White,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    device: Device,
    displayName: String,
    onDismiss: () -> Unit,
    onOpenClips: () -> Unit,
    onRequestRename: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(displayName, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            // Device info
            InfoRow("Model", device.oem.ifBlank { "?" })
            InfoRow("Firmware", device.firmware_ver.ifBlank { "?" })
            InfoRow("MAC address", device.mac.ifBlank { "?" })
            InfoRow("Battery", formatBattery(device.battery_level))
            InfoRow("Time zone", device.timezone.ifBlank { "?" })
            InfoRow("Cloud bucket", device.bucket.ifBlank { "?" })
            InfoRow("Device SN", device.device_sn)

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Button(onClick = onOpenClips, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.PlayCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("View recorded clips")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onRequestRename, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.DriveFileRenameOutline, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Rename (local)")
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Live property control (PIR sensitivity, LED, motion mode) needs more cmd-server reverse-engineering. Settings here will grow as we add support.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
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
