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
private const val FEED_CHUNK_BYTES = 32 * 1024

/**
 * Inline clip player — the same EZCloudStoragePlayer wiring we had in the
 * standalone PlayerScreen, but rendered as a reusable composable that can be
 * embedded in the top of another screen (currently used above the clips list
 * on the ClipsScreen).
 *
 * Lifecycle: the native player is created/torn down by a [DisposableEffect]
 * keyed to [Recording.fileName]. Tapping a different clip swaps the player
 * cleanly. A close button signals the caller (via [onClose]) that the user
 * wants to dismiss the player and go back to whatever placeholder was here.
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
    var bufferProgress by remember(clip.fileName) { mutableFloatStateOf(0f) }
    var videoWidth by remember(clip.fileName) { mutableIntStateOf(0) }
    var videoHeight by remember(clip.fileName) { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    DisposableEffect(clip.fileName) {
        val cacheFile = "$cacheDirPath/${clip.fileName.replace('/', '_')}.cache"
        val p = EZCloudStoragePlayer("", "", cacheFile, false)
        p.setExceptedLength(clip.length)
        p.setFormatFlag(formatFlagFor(clip.fileName))
        p.setPKey(clip.pk)
        p.setListener(object : EZCloudStoragePlayer.Listener {
            override fun onCached(ratio: Float) { bufferProgress = ratio.coerceIn(0f, 1f) }
            override fun onCheckoutInfo(width: Int, height: Int, duration: Float) {
                videoWidth = width; videoHeight = height
                statusText = "${width}×${height} • ${"%.1fs".format(duration)}"
            }
            override fun onComplete() { statusText = "Complete" }
            override fun onError(code: Int, message: String?) {
                errorText = "$code: ${message ?: "(no message)"}"
                Log.e(TAG, "player error $code: $message")
            }
            override fun onPCMCallback(pcm: ByteArray?, len: Int, ts: Float) {}
            override fun onPCMParamCallback(sampleRate: Int, channels: Int, bitsPerSample: Int) {}
            override fun onPerpare() { statusText = "Buffering…" }
            override fun onPlaying(position: Float) {}
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

            // Close button overlay (top-right)
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close player",
                    tint = Color.White.copy(alpha = 0.85f),
                )
            }
        }

        // Slim status row under the surface — keeps the inline footprint small.
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
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = if (isPaused) "Resume" else "Pause",
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(statusText, style = MaterialTheme.typography.bodySmall)
                val dlVisible = downloadProgress > 0f && downloadProgress < 1f
                val bufVisible = bufferProgress > 0f && bufferProgress < 1f
                if (dlVisible || bufVisible) {
                    LinearProgressIndicator(
                        progress = { if (dlVisible) downloadProgress else bufferProgress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** Stream the OSS-signed .ts2 into the native player + a local cache file. */
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
        val buf = ByteArray(FEED_CHUNK_BYTES)
        var pushed: Long = 0
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
