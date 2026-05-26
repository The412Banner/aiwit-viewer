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
import org.json.JSONObject
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.security.MessageDigest

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

    companion object {
        private const val TAG = "LiveSession"
        // From AIWIT's y1.e (XOR transform applied to cloud-relay UDP payloads).
        private val XOR_KEY = "jq_Q#`@Ui{&Nx:1HVMrvw\$zKT[GX<9Wg".toByteArray(Charsets.US_ASCII)
        // Same salt as the cmd-server signing (DoorbellApplication.f13706c).
        private const val CMD_SIGN_SALT = "eead%Hb27Zf\$v#vG"
    }

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
                // AIWIT's listener IGNORES sn AND len; it just runs n1.c.k(bArr, pk)
                // on the full byte[]. `len` appears to be a packet-type flag (0/1),
                // not a byte count. So we log bytes.size, not len.
                val activeSn = activeDeviceSn
                val pk = activePk
                val stats = rxStats.getOrPut(activeSn ?: "no-active") { RxStats() }
                stats.rxBytes++
                stats.rxTotal += bytes.size
                // Sample first 16 bytes of the first 5 packets per session so we
                // can see the actual framing on the wire.
                if (stats.rxBytes <= 5) {
                    val hex = bytes.take(minOf(bytes.size, 16))
                        .joinToString(" ") { "%02x".format(it.toInt() and 0xff) }
                    Log.i(TAG, "RX hex#${stats.rxBytes} len-arg=$len bytes.size=${bytes.size} sn=$sn first16=[$hex]")
                }
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
                    Log.i(TAG, "RX active=$activeSn (cb-sn=$sn): ${stats.rxBytes} cbs, totalBytes=${stats.rxTotal}, parsed=${stats.parseOk}, null=${stats.parseNull}, pk=${pk?.take(6)}...")
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
    private val udpReceiverJobs = mutableMapOf<String, Job>()

    /**
     * Hand off the cmd-server's `wakeup`/`preview-start` reply. The reply
     * carries the per-session AES key (pk), the cloud-relay IP, and per-stream
     * UDP ports. The actual video bytes do NOT come through `p2pReceiveDataCall`;
     * AIWIT's `LiveViewForTwoWayIntercom.f.run` opens its own DatagramSocket and
     * receives directly from `cloudIp:videoPort`, sending a periodic XOR-wrapped
     * `cmd:sync` packet as a NAT punch.
     */
    fun onPreviewStartReply(deviceSn: String, pk: String, cloudIp: String, videoPort: Int) {
        val firstForThisSn = activeDeviceSn != deviceSn
        activeDeviceSn = deviceSn
        activePk = pk
        pkByDevice[deviceSn] = pk
        Log.i(TAG, "onPreviewStartReply $deviceSn pk=$pk cloudIp=$cloudIp video_port=$videoPort firstTime=$firstForThisSn")

        val session = P2PSession.getInstance(context)

        sessionByDevice.getOrPut(deviceSn) { n1.a() }
        pollerJobs.getOrPut(deviceSn) {
            scope.launch { pollFramesLoop(deviceSn) }
        }

        // Keep the libVCTP2P connection going — AIWIT uses it for control messages
        // (heartbeats, intercom signalling) while the video flows over the
        // separate UDP socket below.
        if (firstForThisSn) {
            try { session.disconnectToPeer(deviceSn) } catch (_: Throwable) {}
            try {
                session.connectToPeer(deviceSn)
                Log.i(TAG, "$deviceSn connectToPeer called once")
            } catch (t: Throwable) {
                Log.e(TAG, "connectToPeer threw", t)
            }
        }

        // Start (or restart) the cloud-relay UDP video receiver for this camera.
        if (cloudIp.isNotBlank() && videoPort > 0) {
            udpReceiverJobs.remove(deviceSn)?.cancel()
            udpReceiverJobs[deviceSn] = scope.launch {
                runUdpVideoReceiver(deviceSn, cloudIp, videoPort, pk)
            }
        }
    }

    fun stop(deviceSn: String) {
        pollerJobs.remove(deviceSn)?.cancel()
        connectJobs.remove(deviceSn)?.cancel()
        udpReceiverJobs.remove(deviceSn)?.cancel()
        try { P2PSession.getInstance(context).disconnectToPeer(deviceSn) } catch (_: Throwable) {}
        connectedFlows[deviceSn]?.value = false
        frameFlows[deviceSn]?.value = null
    }

    private suspend fun runUdpVideoReceiver(deviceSn: String, cloudIp: String, port: Int, pk: String) = coroutineScope {
        val sn = appSn ?: run { Log.w(TAG, "no appSn yet, can't start UDP receiver"); return@coroutineScope }
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.soTimeout = 250
        } catch (t: Throwable) {
            Log.e(TAG, "DatagramSocket open failed", t); return@coroutineScope
        }
        val addr = try { InetAddress.getByName(cloudIp) } catch (t: Throwable) {
            Log.e(TAG, "resolve $cloudIp failed", t); socket.close(); return@coroutineScope
        }
        Log.i(TAG, "UDP video receiver up: localPort=${socket.localPort} → $cloudIp:$port pk=${pk.take(6)}…")

        // NAT-punch keepalive: send a `cmd:sync` packet every 2s so the cloud
        // relay can map us back through NAT.
        val natJob = launch {
            while (isActive) {
                try {
                    val pkt = buildSyncPacket(sn, deviceSn, from = 1)
                    socket.send(DatagramPacket(pkt, pkt.size, addr, port))
                } catch (t: Throwable) {
                    Log.w(TAG, "sync send to $cloudIp:$port failed: ${t.message}")
                }
                delay(2000)
            }
        }

        var rxPkts = 0
        var rxBytes = 0L
        var rtpParsed = 0
        var rtpNull = 0
        var lastLog = System.currentTimeMillis()
        val buf = ByteArray(1600)
        try {
            while (isActive) {
                val datagram = DatagramPacket(buf, buf.size)
                try {
                    socket.receive(datagram)
                } catch (e: SocketTimeoutException) { continue }
                catch (t: Throwable) {
                    if (!isActive) break
                    Log.w(TAG, "udp recv error: ${t.message}"); delay(50); continue
                }
                rxPkts++
                rxBytes += datagram.length
                val raw = datagram.data.copyOf(datagram.length)
                val decoded = xorDecodeIfNeeded(raw)
                if (rxPkts <= 30) {
                    val rawHex = raw.take(16).joinToString(" ") { "%02x".format(it.toInt() and 0xff) }
                    val decHex = decoded?.take(16)?.joinToString(" ") { "%02x".format(it.toInt() and 0xff) } ?: "<null>"
                    Log.i(TAG, "udp rx#$rxPkts raw len=${raw.size} first16=[$rawHex] -> dec len=${decoded?.size ?: -1} first16=[$decHex]")
                }
                if (decoded == null) continue
                if (decoded.size < 12) continue
                val isRtp = decoded[0] in 0x80.toByte()..0x83.toByte()
                if (!isRtp) continue
                val frame = try { n1.c.k(decoded, pk) } catch (t: Throwable) {
                    Log.w(TAG, "n1.c.k threw on udp pkt: ${t::class.java.simpleName}: ${t.message}", t); null
                }
                if (rxPkts in 3..10) {
                    val payloadLen = try { frame?.l()?.size } catch (_: Throwable) { -2 }
                    val type = try { frame?.i() } catch (_: Throwable) { -2 }
                    Log.i(TAG, "udp pkt#$rxPkts n1.c.k => ${if (frame == null) "NULL" else "type=$type seq=${frame.m()} payload-len=$payloadLen"}")
                }
                if (frame == null) {
                    rtpNull++
                } else {
                    rtpParsed++
                    sessionByDevice.getOrPut(deviceSn) { n1.a() }.f(frame)
                }
                val now = System.currentTimeMillis()
                if (now - lastLog > 2000) {
                    Log.i(TAG, "udp $deviceSn: pkts=$rxPkts bytes=$rxBytes rtp-parsed=$rtpParsed rtp-null=$rtpNull")
                    lastLog = now
                }
            }
        } finally {
            natJob.cancel()
            try { socket.close() } catch (_: Throwable) {}
            Log.i(TAG, "UDP video receiver stopped for $deviceSn")
        }
    }

    /**
     * Inverse of y1.e.O: if the packet begins with the AIWIT relay marker 0x90,
     * drop the 12-byte header and XOR-decrypt the rest. Otherwise return the
     * packet unchanged.
     */
    private fun xorDecodeIfNeeded(data: ByteArray): ByteArray? {
        if (data.isEmpty()) return null
        if (data[0] != 0x90.toByte()) return data
        if (data.size <= 12) return null
        val out = ByteArray(data.size - 12)
        for (i in out.indices) {
            out[i] = (data[i + 12].toInt() xor XOR_KEY[i % XOR_KEY.size].toInt()).toByte()
        }
        return out
    }

    /**
     * Equivalent of `LiveViewForTwoWayIntercom.v2`: build a `cmd:sync` JSON,
     * sign it with the same MD5(salt+udid+cmd) scheme as the cmd-server, then
     * wrap it with `y1.e.O` (12-byte 0x90 header + XOR-encrypted body).
     */
    private fun buildSyncPacket(appSn: String, peerSn: String, from: Int): ByteArray {
        val k = "0" + md5Hex(CMD_SIGN_SALT + appSn + "sync")
        val json = JSONObject().apply {
            put("cmd", "sync")
            put("udid", appSn)
            put("peer", peerSn)
            put("type", "udp")
            put("k", k)
            put("from", from)
        }.toString().toByteArray(Charsets.UTF_8)
        val encrypted = ByteArray(json.size)
        for (i in json.indices) {
            encrypted[i] = (json[i].toInt() xor XOR_KEY[i % XOR_KEY.size].toInt()).toByte()
        }
        val out = ByteArray(encrypted.size + 12)
        out[0] = 0x90.toByte()
        System.arraycopy(encrypted, 0, out, 12, encrypted.size)
        return out
    }

    private fun md5Hex(s: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(s.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(32)
        for (b in digest) sb.append("%02x".format(b.toInt() and 0xff))
        return sb.toString()
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
