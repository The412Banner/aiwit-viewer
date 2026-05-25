package com.the412banner.aiwitviewer.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import cn.coderfly.ezmediautils.EZMediaUtils
import com.eken.doorbell.p2p.P2PSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONException
import java.io.IOException

/**
 * Drives one camera's live preview end-to-end:
 *   - P2PSession.loginP2P(...) once with the p2p+stun servers from list_v2.config
 *   - P2PSession.addListener(P2PClientCall) once
 *   - per camera: cache the `pk` from the cmd-server's `preview-start` reply,
 *     then connectToPeer(deviceSn)
 *   - on p2pReceiveDataCall(sn, bytes, len): parse with n1.c.k(bytes, pk),
 *     feed n1.a jitter buffer, poll complete frames, decode each with
 *     EZMediaUtils.decode*FrameWithData → Bitmap
 *   - emit Bitmaps via a per-device StateFlow that UI can render
 *
 * This is the "stretch goal first frame" path. Audio + two-way intercom are
 * intentionally left for later — video bitmaps only.
 */
class LiveSession(private val context: Context) {

    companion object { private const val TAG = "LiveSession" }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pkByDevice = mutableMapOf<String, String>()
    private val sessionByDevice = mutableMapOf<String, n1.a>()
    private val pollerJobs = mutableMapOf<String, Job>()
    private val frameFlows = mutableMapOf<String, MutableStateFlow<Bitmap?>>()
    private val connectedFlows = mutableMapOf<String, MutableStateFlow<Boolean>>()
    @Volatile private var loggedIn = false
    @Volatile private var listener: P2PSession.P2PClientCall? = null
    @Volatile private var appSn: String? = null

    /** Public flow of the latest decoded Bitmap for a given camera. */
    fun frames(deviceSn: String): StateFlow<Bitmap?> = frameFlows.getOrPut(deviceSn) { MutableStateFlow(null) }
    fun connected(deviceSn: String): StateFlow<Boolean> = connectedFlows.getOrPut(deviceSn) { MutableStateFlow(false) }

    /** Call once after REST login completes (config available). Safe to call repeatedly. */
    fun bootstrap(appSn: String, config: ConnectionConfig) {
        if (loggedIn) return
        this.appSn = appSn

        val p2pServers = if (config.enable_tls_encryption == 1 && config.p2p_encrypt_servers.isNotEmpty()) {
            config.p2p_encrypt_servers
        } else {
            config.p2p_servers
        }
        val p2p = p2pServers.firstOrNull() ?: run {
            Log.e(TAG, "no p2p server in config"); return
        }
        val stun = config.stun_servers.firstOrNull() ?: HostPort("", 0)

        Log.i(TAG, "P2P loginP2P apkId=$appSn p2p=${p2p.ip}:${p2p.port} stun=${stun.ip}:${stun.port}")
        val session = P2PSession.getInstance(context)
        try {
            session.loginP2P(appSn, p2p.ip, p2p.port, stun.ip, stun.port)
        } catch (t: Throwable) {
            Log.e(TAG, "loginP2P threw", t)
            return
        }

        val cb = object : P2PSession.P2PClientCall {
            override fun p2pConnected(sn: String?, ok: Boolean) {
                Log.i(TAG, "p2pConnected sn=$sn ok=$ok")
                if (sn != null) connectedFlows.getOrPut(sn) { MutableStateFlow(false) }.value = ok
            }
            @Throws(JSONException::class, IOException::class)
            override fun p2pReceiveDataCall(sn: String?, bytes: ByteArray?, len: Int) {
                if (sn == null || bytes == null) return
                val pk = pkByDevice[sn]
                val frame = try {
                    n1.c.k(bytes, pk)
                } catch (t: Throwable) {
                    Log.w(TAG, "n1.c.k threw", t); return
                } ?: return
                val buf = sessionByDevice.getOrPut(sn) { n1.a() }
                buf.f(frame)
                // d() poll happens in a per-device coroutine; only need to mark
                // that data is flowing here for diagnostics
            }
        }
        listener = cb
        session.addListener(cb)
        loggedIn = true
    }

    /** Hand off the cmd-server's `preview-start` reply: cache pk, kick connectToPeer + poller. */
    fun onPreviewStartReply(deviceSn: String, pk: String) {
        Log.i(TAG, "onPreviewStartReply $deviceSn pk=$pk (cached) — kicking connectToPeer")
        pkByDevice[deviceSn] = pk
        val session = P2PSession.getInstance(context)

        // Ensure a jitter buffer + poller exist for this camera.
        sessionByDevice.getOrPut(deviceSn) { n1.a() }
        pollerJobs.getOrPut(deviceSn) {
            scope.launch { pollFramesLoop(deviceSn) }
        }

        try {
            session.disconnectToPeer(deviceSn)
        } catch (_: Throwable) {}
        try {
            session.connectToPeer(deviceSn)
        } catch (t: Throwable) {
            Log.e(TAG, "connectToPeer threw", t)
        }
    }

    fun stop(deviceSn: String) {
        pollerJobs.remove(deviceSn)?.cancel()
        try { P2PSession.getInstance(context).disconnectToPeer(deviceSn) } catch (_: Throwable) {}
        connectedFlows[deviceSn]?.value = false
        frameFlows[deviceSn]?.value = null
    }

    private suspend fun pollFramesLoop(deviceSn: String) {
        Log.i(TAG, "pollFramesLoop start for $deviceSn")
        val buffer = sessionByDevice[deviceSn] ?: return
        var framesDecoded = 0
        var lastLogAt = System.currentTimeMillis()
        while (currentCoroutineContext().isActive) {
            val item: n1.b? = buffer.d()
            if (item == null) {
                delay(15); continue
            }
            val byteBuffer = try { item.b() } catch (t: Throwable) { Log.w(TAG, "item.b() threw", t); null }
            if (byteBuffer == null) continue
            val payload = ByteArray(byteBuffer.remaining()).also { byteBuffer.get(it) }
            val bmp: Bitmap? = try {
                EZMediaUtils.decodeFrameWithData(payload)
            } catch (t: Throwable) {
                Log.w(TAG, "decodeFrameWithData threw", t); null
            }
            if (bmp != null) {
                framesDecoded++
                val flow = frameFlows.getOrPut(deviceSn) { MutableStateFlow<Bitmap?>(null) }
                flow.value = bmp
            }
            val now = System.currentTimeMillis()
            if (now - lastLogAt > 2000) {
                Log.i(TAG, "$deviceSn frames=$framesDecoded")
                lastLogAt = now
            }
        }
    }
}
