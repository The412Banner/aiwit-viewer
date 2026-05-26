package com.the412banner.aiwitviewer.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import cn.coderfly.ezmediautils.EZMediaUtils
import com.eken.doorbell.p2p.P2PSession
import com.eken.nat.Nat
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
    private class RxStats(var rxBytes: Int = 0, var rxTotal: Long = 0, var parseOk: Int = 0, var parseNull: Int = 0)
    private val rxStats = mutableMapOf<String, RxStats>()
    @Volatile private var activeDeviceSn: String? = null
    @Volatile private var activePk: String? = null
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

        // AIWIT (o1/u0.java:3049) prefers p2p_encrypt_servers if present,
        // regardless of enable_tls_encryption. The boolean is just an "encrypted
        // mode" hint that gets passed to setEncrypt(true) immediately before
        // loginP2P. Skipping setEncrypt was the bug in the first attempt — the
        // native lib defaults to plain mode and the TLS relay silently drops
        // un-encrypted handshakes.
        val encrypted = config.p2p_encrypt_servers.isNotEmpty()
        val p2pServers = if (encrypted) config.p2p_encrypt_servers else config.p2p_servers
        val p2p = p2pServers.firstOrNull() ?: run {
            Log.e(TAG, "no p2p server in config"); return
        }
        val stun = config.stun_servers.firstOrNull() ?: HostPort("", 0)

        // Kick the lib's internal NAT-discovery loop. AIWIT calls this in
        // DevicePreview to display the NAT type, but it appears the side effect
        // of triggering NAT discovery is what populates the relay-peer table.
        val natType = try { Nat.getNatType() } catch (t: Throwable) { Log.w(TAG, "Nat.getNatType threw", t); -1 }
        Log.i(TAG, "Nat.getNatType() = $natType (pre-bootstrap)")

        Log.i(TAG, "P2P setEncrypt($encrypted) + loginP2P apkId=$appSn p2p=${p2p.ip}:${p2p.port} stun=${stun.ip}:${stun.port}")
        val session = P2PSession.getInstance(context)
        try {
            session.setEncrypt(encrypted)
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
                if (bytes == null) return
                // Important: AIWIT's listener IGNORES the `sn` arg and uses the
                // pk from the most recent preview-start (LiveViewForTwoWayIntercom.f15957e2).
                // The lib passes "12345678" (or similar placeholder) as sn instead of the
                // device SN, so a `pkByDevice[sn]` lookup always misses.
                // We route through the currently-active camera's pk + jitter buffer.
                val activeSn = activeDeviceSn
                val pk = activePk
                val stats = rxStats.getOrPut(activeSn ?: "no-active") { RxStats() }
                stats.rxBytes++
                stats.rxTotal += len
                if (activeSn == null) return
                val frame = try {
                    n1.c.k(bytes, pk)
                } catch (t: Throwable) {
                    Log.w(TAG, "n1.c.k threw", t); return
                }
                if (frame == null) {
                    stats.parseNull++
                } else {
                    stats.parseOk++
                    val buf = sessionByDevice.getOrPut(activeSn) { n1.a() }
                    buf.f(frame)
                }
                if (stats.rxBytes % 50 == 0) {
                    Log.i(TAG, "RX active=$activeSn (cb-sn=$sn): ${stats.rxBytes} cbs, ${stats.rxTotal} bytes, parsed=${stats.parseOk}, null=${stats.parseNull}, pk=${pk?.take(6)}...")
                }
            }
        }
        listener = cb
        session.addListener(cb)
        loggedIn = true

        // Periodically poll Nat.getNatType() and P2PSession.getNatType() — AIWIT
        // does this once per second (visible as `natType... 0` in libVCTP2P logs).
        // Without these polls the lib's internal NAT machinery appears to stall.
        scope.launch {
            while (currentCoroutineContext().isActive) {
                try {
                    val nt1 = Nat.getNatType()
                    val nt2 = session.getNatType()
                    if (Math.random() < 0.05) Log.d(TAG, "nat poll: nat=$nt1 p2p=$nt2 speed=${session.getSpeed()}")
                } catch (_: Throwable) {}
                delay(1000)
            }
        }
    }

    private val connectJobs = mutableMapOf<String, Job>()

    /** Hand off the cmd-server's `wakeup`/`preview-start` reply: cache pk, kick connectToPeer + poller. */
    fun onPreviewStartReply(deviceSn: String, pk: String) {
        // First call wins as active. Subsequent calls (e.g. preview-start notification
        // arriving after the wakeup reply for the same camera) just refresh the pk.
        val firstForThisSn = activeDeviceSn != deviceSn
        activeDeviceSn = deviceSn
        activePk = pk
        pkByDevice[deviceSn] = pk
        Log.i(TAG, "onPreviewStartReply $deviceSn pk=$pk (active=$deviceSn, firstTime=$firstForThisSn)")

        val session = P2PSession.getInstance(context)

        sessionByDevice.getOrPut(deviceSn) { n1.a() }
        pollerJobs.getOrPut(deviceSn) {
            scope.launch { pollFramesLoop(deviceSn) }
        }

        // Call connectToPeer ONCE. The lib's internal retry/state machine handles
        // the rest. Calling repeatedly was resetting state and confusing the lib.
        if (firstForThisSn) {
            try {
                session.disconnectToPeer(deviceSn)
            } catch (_: Throwable) {}
            try {
                session.connectToPeer(deviceSn)
                Log.i(TAG, "$deviceSn connectToPeer called once")
            } catch (t: Throwable) {
                Log.e(TAG, "connectToPeer threw", t)
            }
        }
    }

    fun stop(deviceSn: String) {
        pollerJobs.remove(deviceSn)?.cancel()
        connectJobs.remove(deviceSn)?.cancel()
        try { P2PSession.getInstance(context).disconnectToPeer(deviceSn) } catch (_: Throwable) {}
        connectedFlows[deviceSn]?.value = false
        frameFlows[deviceSn]?.value = null
    }

    private suspend fun pollFramesLoop(deviceSn: String) {
        Log.i(TAG, "pollFramesLoop start for $deviceSn")
        val buffer = sessionByDevice[deviceSn] ?: return
        var framesPolled = 0
        var framesDecoded = 0
        var decodeNulls = 0
        var lastLogAt = System.currentTimeMillis()
        while (currentCoroutineContext().isActive) {
            val item: n1.b? = buffer.d()
            if (item == null) {
                delay(15)
                val now = System.currentTimeMillis()
                if (now - lastLogAt > 2000) {
                    Log.i(TAG, "$deviceSn polled=$framesPolled decoded=$framesDecoded decode-null=$decodeNulls (empty d() this tick)")
                    lastLogAt = now
                }
                continue
            }
            framesPolled++
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
            } else {
                decodeNulls++
            }
            val now = System.currentTimeMillis()
            if (now - lastLogAt > 2000) {
                Log.i(TAG, "$deviceSn polled=$framesPolled decoded=$framesDecoded decode-null=$decodeNulls")
                lastLogAt = now
            }
        }
    }
}
