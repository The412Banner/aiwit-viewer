package com.the412banner.aiwitviewer.ui

import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import cn.coderfly.mediacodec.EZCloudStoragePlayer
import com.the412banner.aiwitviewer.data.Recording

private const val TAG = "PlayerScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    clip: Recording,
    cacheDirPath: String,
    onBack: () -> Unit,
) {
    val player = remember { mutableStateOf<EZCloudStoragePlayer?>(null) }
    var isPaused by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Preparing…") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    var videoWidth by remember { mutableIntStateOf(0) }
    var videoHeight by remember { mutableIntStateOf(0) }

    DisposableEffect(clip.fileName) {
        val cacheFile = "$cacheDirPath/${clip.fileName.replace('/', '_')}.cache"
        val p = EZCloudStoragePlayer("", "", cacheFile, false)
        p.setExceptedLength(clip.length)
        p.setFormatFlag(formatFlagFor(clip.fileName))
        p.setPKey(clip.pk)
        p.setListener(object : EZCloudStoragePlayer.Listener {
            override fun onCached(ratio: Float) {
                progress = ratio.coerceIn(0f, 1f)
            }
            override fun onCheckoutInfo(width: Int, height: Int, duration: Float) {
                videoWidth = width; videoHeight = height
                statusText = "${width}×${height} • ${"%.1fs".format(duration)}"
            }
            override fun onComplete() { statusText = "Complete" }
            override fun onError(code: Int, message: String?) {
                errorText = "$code: ${message ?: "(no message)"}"
                Log.e(TAG, "player error $code: $message")
            }
            override fun onPCMCallback(pcm: ByteArray?, len: Int, ts: Float) { /* audio handled natively by AudioTrack elsewhere */ }
            override fun onPCMParamCallback(sampleRate: Int, channels: Int, bitsPerSample: Int) {
                Log.d(TAG, "PCM params: $sampleRate Hz, $channels ch, $bitsPerSample bps")
            }
            override fun onPerpare() { statusText = "Buffering…" }
            override fun onPlaying(position: Float) { /* per-frame, ignore */ }
            override fun onRenderNNInfo(info: String?) { /* AI overlay info, ignore */ }
            override fun onStart() { statusText = "Playing" }
            override fun onStop() { statusText = "Stopped" }
        })

        player.value = p
        onDispose {
            try { p.stop() } catch (e: Throwable) { Log.w(TAG, "stop", e) }
            try { p.release() } catch (e: Throwable) { Log.w(TAG, "release", e) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(clip.fileName.substringAfterLast('/')) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            BottomAppBar {
                IconButton(onClick = {
                    val p = player.value ?: return@IconButton
                    isPaused = !isPaused
                    p.pause(isPaused)
                }) {
                    Icon(
                        if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause",
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(statusText, style = MaterialTheme.typography.bodyMedium)
                    if (progress > 0f && progress < 1f) {
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (errorText != null) {
                Text("Error: $errorText", color = MaterialTheme.colorScheme.error)
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        SurfaceView(ctx).also { sv ->
                            sv.holder.addCallback(object : SurfaceHolder.Callback {
                                override fun surfaceCreated(holder: SurfaceHolder) {
                                    val p = player.value ?: return
                                    p.setSurface(holder.surface)
                                    // Kick playback once the surface is ready.
                                    p.play(clip.endpoint, clip.bucket, clip.fileName)
                                }
                                override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {}
                                override fun surfaceDestroyed(holder: SurfaceHolder) {
                                    player.value?.setSurface(null)
                                }
                            })
                        }
                    },
                )
            }
        }
    }
}

private fun formatFlagFor(fileName: String): Int = when {
    fileName.endsWith(".v6ts") -> EZCloudStoragePlayer.DBCloudTSPlayerFormatFlagVIAv6TS
    fileName.endsWith(".ts2") -> EZCloudStoragePlayer.DBCloudTSPlayerFormatFlagTS2
    fileName.endsWith(".ts3") -> EZCloudStoragePlayer.DBCloudTSPlayerFormatFlagTS3
    fileName.endsWith(".ts4") -> EZCloudStoragePlayer.DBCloudTSPlayerFormatFlagTS4
    else -> EZCloudStoragePlayer.DBCloudTSPlayerFormatFlagDefault
}
