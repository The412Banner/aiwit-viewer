package com.the412banner.aiwitviewer.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/**
 * Placeholder for the live PIP that we'll wire up once [[project-aiwit-re]]
 * Phase 5c (libNatType.so RE) lands. For now it's a visual stub so the screen
 * layout doesn't need to be re-shuffled when the real preview goes in.
 *
 * Two variants:
 *   - [LivePipTile]   — small 16:9 tile used inline next to a camera row.
 *   - [LivePipBanner] — full-width 16:9 banner used above the clips list.
 */
@Composable
fun LivePipTile(
    isOnline: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(width = 96.dp, height = 54.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF101010))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isOnline) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
            contentDescription = if (isOnline) "Live preview" else "Camera offline",
            tint = if (isOnline) Color(0xFF4CAF50) else Color(0xFF555555),
        )
    }
}

@Composable
fun LivePipBanner(
    deviceName: String,
    isOnline: Boolean,
    modifier: Modifier = Modifier,
    frame: Bitmap? = null,
) {
    if (frame != null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(frame.width.toFloat() / frame.height.toFloat().coerceAtLeast(1f))
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = frame.asImageBitmap(),
                contentDescription = "Live preview of $deviceName",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
        return
    }
    LivePipBannerLayout(modifier = modifier) {
        // Always show the "we're trying" indication when a camera is selected —
        // even if the device state still reports asleep. Physical wakeup happens
        // before the cloud catches up.
        Icon(
            imageVector = Icons.Filled.Videocam,
            contentDescription = null,
            tint = Color(0xFFFFA000), // amber = "working on it"
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "$deviceName • waking up…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            if (isOnline) "camera online, negotiating stream" else "camera asleep in cloud state",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
    }
}

/** Top banner shown on the Cameras screen when no camera has been tapped yet. */
@Composable
fun LivePipBannerEmpty(modifier: Modifier = Modifier) {
    LivePipBannerLayout(modifier = modifier) {
        Icon(
            imageVector = Icons.Filled.Videocam,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Tap a camera below to view",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun LivePipBannerLayout(modifier: Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF101010))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { content() }
    }
}
