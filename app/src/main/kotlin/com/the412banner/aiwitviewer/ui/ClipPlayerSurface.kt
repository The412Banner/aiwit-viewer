package com.the412banner.aiwitviewer.ui

import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import cn.coderfly.mediacodec.EZCloudStoragePlayer
import com.the412banner.aiwitviewer.data.Recording
import kotlinx.coroutines.*
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "ClipPlayerSurface"
private const val FEED_CHUNK_BYTES = 16 * 1024

/**
 * Inline clip player with throttled feed + control bar.
 *
 * The .ts2 recordings are video-only; the native lib has no audio clock to
 * pace decode against. Feeding bytes as fast as we can read them causes the
 * player to render every frame instantly — an 18s clip plays in under a
 * second. To fix, we feed at the clip's natural bitrate (length / duration)
 * plus a small look-ahead buffer, throttled with a sleep loop.
 */
@Composable
fun ClipPlayerSurface(
    clip: Recording,
    cacheDirPath: String,
    signedUrlProvider: suspend (Recording) -> String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val player = remember { mutableStateOf<EZCloudStoragePlayer?>(null) }
    var isPaused by remember(clip.fileName) { mutableStateOf(false) }
    var statusText by remember(clip.fileName) { mutableStateOf("Preparing…") }
    var errorText by remember(clip.fileName) { mutableStateOf<String?>(null) }
    var downloadProgress by remember(clip.fileName) { mutableFloatStateOf(0f) }
    var videoWidth by remember(clip.fileName) { mutableIntStateOf(0) }
    var videoHeight by remember(clip.fileName) { mutableIntStateOf(0) }
    var positionSec by remember(clip.fileName) { mutableFloatStateOf(0f) }
    var totalDurationSec by remember(clip.fileName) { mutableFloatStateOf(clip.duration / 1000f) }
    var restartTick by remember(clip.fileName) { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    DisposableEffect(clip.fileName, restartTick) {
        val cacheFile = "$cacheDirPath/${clip.fileName.replace('/', '_')}.cache"
        val p = EZCloudStoragePlayer("", "", cacheFile, false)
        p.setExceptedLength(clip.length)
        p.setFormatFlag(formatFlagFor(clip.fileName))
        p.setPKey(clip.pk)
        p.setListener(object : EZCloudStoragePlayer.Listener {
            override fun onCached(ratio: Float) { /* cache write progress — ignore */ }
            override fun onCheckoutInfo(width: Int, height: Int, duration: Float) {
                videoWidth = width; videoHeight = height
                if (duration > 0) totalDurationSec = duration
                statusText = "${width}×${height}"
            }
            override fun onComplete() { statusText = "Complete"; positionSec = totalDurationSec }
            override fun onError(code: Int, message: String?) {
                errorText = "$code: ${message ?: "(no message)"}"
                Log.e(TAG, "player error $code: $message")
            }
            override fun onPCMCallback(pcm: ByteArray?, len: Int, ts: Float) {}
            override fun onPCMParamCallback(sampleRate: Int, channels: Int, bitsPerSample: Int) {}
            override fun onPerpare() { statusText = "Buffering…" }
            override fun onPlaying(position: Float) { positionSec = position }
            override fun onRenderNNInfo(info: String?) {}
            override fun onStart() { statusText = "Playing" }
            override fun onStop() { statusText = "Stopped" }
        })
        player.value = p

        onDispose {
            try { p.stop() } catch (e: Throwable) { Log.w(TAG, "stop", e) }
            try { p.release() } catch (e: Throwable) { Log.w(TAG, "release", e) }
        }
    }

    val videoAspect = if (videoWidth > 0 && videoHeight > 0) {
        videoWidth.toFloat() / videoHeight.toFloat()
    } else {
        16f / 9f
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(videoAspect)
                .clip(RoundedCornerShape(8.dp))
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
                                private var feedJob: Job? = null
                                override fun surfaceCreated(holder: SurfaceHolder) {
                                    val p = player.value ?: return
                                    p.setSurface(holder.surface)
                                    p.play(clip.endpoint, clip.bucket, clip.fileName)
                                    feedJob = scope.launch(Dispatchers.IO) {
                                        feedClipIntoPlayer(
                                            clip = clip,
                                            cacheFilePath = "$cacheDirPath/${clip.fileName.replace('/', '_')}.cache",
                                            signedUrlProvider = signedUrlProvider,
                                            player = p,
                                            onProgress = { downloadProgress = it },
                                            onError = { errorText = it },
                                        )
                                    }
                                }
                                override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {}
                                override fun surfaceDestroyed(holder: SurfaceHolder) {
                                    feedJob?.cancel()
                                    player.value?.setSurface(null)
                                }
                            })
                        }
                    },
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close player", tint = Color.White.copy(alpha = 0.85f))
            }
        }

        // Control bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    val p = player.value ?: return@IconButton
                    isPaused = !isPaused
                    p.pause(isPaused)
                },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = if (isPaused) "Resume" else "Pause",
                )
            }
            IconButton(
                onClick = {
                    positionSec = 0f
                    isPaused = false
                    restartTick++  // forces DisposableEffect to recreate player
                },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(Icons.Filled.Replay, contentDescription = "Restart from beginning")
            }
            Spacer(Modifier.width(4.dp))
            Text(
                fmtTime(positionSec),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.widthIn(min = 36.dp),
            )
            Slider(
                value = positionSec.coerceIn(0f, totalDurationSec),
                onValueChange = { /* read-only — lib doesn't expose seek; user can Restart */ },
                valueRange = 0f..totalDurationSec.coerceAtLeast(0.001f),
                enabled = false, // visible progress, not draggable
                modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
            )
            Text(
                fmtTime(totalDurationSec),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.widthIn(min = 36.dp),
            )
        }
        if (downloadProgress > 0f && downloadProgress < 1f) {
            LinearProgressIndicator(
                progress = { downloadProgress },
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            )
        }
        Text(
            statusText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 8.dp, top = 2.dp),
        )
    }
}

private fun fmtTime(sec: Float): String {
    val s = sec.coerceAtLeast(0f).toInt()
    return "%d:%02d".format(s / 60, s % 60)
}

/** Stream the OSS-signed .ts2 into the native player at the clip's natural bitrate. */
private suspend fun feedClipIntoPlayer(
    clip: Recording,
    cacheFilePath: String,
    signedUrlProvider: suspend (Recording) -> String,
    player: EZCloudStoragePlayer,
    onProgress: (Float) -> Unit,
    onError: (String) -> Unit,
) {
    try {
        val url = signedUrlProvider(clip)
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            requestMethod = "GET"
        }
        if (conn.responseCode !in 200..299) {
            val body = conn.errorStream?.bufferedReader()?.readText().orEmpty().take(300)
            onError("OSS HTTP ${conn.responseCode}: $body")
            return
        }
        val totalBytes = if (clip.length > 0) clip.length else conn.contentLengthLong
        // Throttle target: feed bytes at the clip's natural bitrate, with a 2-second
        // lookahead so the decoder has buffer headroom but doesn't burst-decode.
        val bytesPerSec: Long = if (clip.duration > 0 && totalBytes > 0) {
            (totalBytes * 1000L / clip.duration).coerceAtLeast(8_000L)
        } else {
            Long.MAX_VALUE
        }
        val lookaheadBytes = bytesPerSec * 2  // 2 seconds of headroom
        val buf = ByteArray(FEED_CHUNK_BYTES)
        var pushed: Long = 0
        val startMs = System.currentTimeMillis()
        conn.inputStream.use { input ->
            FileOutputStream(cacheFilePath).use { cache ->
                while (true) {
                    if (!currentCoroutineContext().isActive) break
                    val n = input.read(buf)
                    if (n == -1) break
                    try {
                        player.readData(buf, n)
                    } catch (e: Throwable) {
                        Log.w(TAG, "readData threw", e)
                        onError("readData failed: ${e.message ?: e.javaClass.simpleName}")
                        return
                    }
                    cache.write(buf, 0, n)
                    pushed += n
                    if (totalBytes > 0) onProgress(pushed.toFloat() / totalBytes.toFloat())

                    // Throttle: how long should we have taken to reach `pushed`?
                    if (bytesPerSec != Long.MAX_VALUE) {
                        val expectedMs = ((pushed - lookaheadBytes).coerceAtLeast(0) * 1000L) / bytesPerSec
                        val actualMs = System.currentTimeMillis() - startMs
                        val sleepMs = expectedMs - actualMs
                        if (sleepMs > 5) delay(sleepMs)
                    }
                }
            }
        }
        onProgress(1f)
    } catch (e: CancellationException) {
        // expected on screen exit
    } catch (e: Throwable) {
        Log.e(TAG, "feed failed", e)
        onError("Download failed: ${e.message ?: e.javaClass.simpleName}")
    }
}

private fun formatFlagFor(fileName: String): Int = when {
    fileName.endsWith(".v6ts") -> EZCloudStoragePlayer.DBCloudTSPlayerFormatFlagVIAv6TS
    fileName.endsWith(".ts2") -> EZCloudStoragePlayer.DBCloudTSPlayerFormatFlagTS2
    fileName.endsWith(".ts3") -> EZCloudStoragePlayer.DBCloudTSPlayerFormatFlagTS3
    fileName.endsWith(".ts4") -> EZCloudStoragePlayer.DBCloudTSPlayerFormatFlagTS4
    else -> EZCloudStoragePlayer.DBCloudTSPlayerFormatFlagDefault
}
