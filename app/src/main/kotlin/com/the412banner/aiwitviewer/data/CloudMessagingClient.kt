package com.the412banner.aiwitviewer.data

import android.util.Log
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.security.SecureRandom
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

/**
 * Skeleton messaging client for the AIWIT/GDXP cloud. Connects to the same
 * TLS endpoint AIWIT's `m1.f` uses, dumps everything received to logcat for
 * diagnostic work, and keeps the connection alive so we can layer in
 * concrete commands (login, wake-camera, etc.) on top.
 *
 * Endpoint: tcp/tls to `47.107.28.145:9003` (matches u1.q.f33910m / f33915n
 * in the AIWIT app — the only relay the app TLS-dials).
 *
 * Wire format from m1.f.L():
 *   - JSON objects sent without length prefix.
 *   - Multiple consecutive objects in one TCP read are split on `}{` and
 *     re-stitched back into well-formed JSON.
 *   - Partial reads are buffered until a complete JSON object parses.
 *
 * m1.f also has an alternate encrypted+framed path (M(), via x1.a) gated on
 * `DoorbellApplication.f13712d1`. We start with plain JSON; if the server
 * insists on the encrypted path we'll port that path next.
 *
 * Cert pinning: same posture as AIWIT — trust-all (matches u1.q.a()).
 */
/**
 * Default endpoint reflects what AIWIT actually connects to (verified on a
 * live device via /proc/<aiwit_pid>/net/tcp): `cmd_servers[0].tls_port` from
 * the list_v2 response, which is `8.222.190.34:8891`. The same response
 * also has a legacy `push_server: 47.107.28.145:9003` field that's a
 * fallback for older firmware paths and is firewall-filtered from outside
 * the China mainland.
 *
 * TODO once we know this connects, fetch the server map from list_v2's
 * config block dynamically instead of hardcoding.
 */
class CloudMessagingClient(
    private val serverHost: String = "8.222.190.34",
    private val serverPort: Int = 8891,
) {
    companion object { private const val TAG = "CloudMsg" }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var socket: SSLSocket? = null
    @Volatile private var readJob: Job? = null
    @Volatile private var heartbeatJob: Job? = null

    private var onMessage: ((org.json.JSONObject) -> Unit)? = null
    private var onState: ((State) -> Unit)? = null
    private var partial: String = ""

    enum class State { Disconnected, Connecting, Connected, Failed }

    fun setOnMessage(cb: (org.json.JSONObject) -> Unit) { onMessage = cb }
    fun setOnState(cb: (State) -> Unit) { onState = cb }

    fun connect() {
        if (socket?.isConnected == true) return
        onState?.invoke(State.Connecting)
        scope.launch {
            try {
                val ctx = SSLContext.getInstance("TLS").apply {
                    init(null, arrayOf(trustAll), SecureRandom())
                }
                val s = (ctx.socketFactory.createSocket() as SSLSocket).apply {
                    connect(InetSocketAddress(serverHost, serverPort), 15_000)
                    soTimeout = 0 // blocking reads, heartbeat keeps it alive
                }
                socket = s
                onState?.invoke(State.Connected)
                Log.i(TAG, "TLS connected to $serverHost:$serverPort")

                readJob = scope.launch { readLoop(s) }
                heartbeatJob = scope.launch { heartbeatLoop() }
            } catch (e: Throwable) {
                Log.e(TAG, "connect failed", e)
                onState?.invoke(State.Failed)
            }
        }
    }

    fun sendJson(payload: org.json.JSONObject) {
        scope.launch {
            try {
                val raw = payload.toString().toByteArray()
                socket?.getOutputStream()?.write(raw)
                socket?.getOutputStream()?.flush()
                Log.d(TAG, "TX: ${payload.toString().take(300)}")
            } catch (e: Throwable) {
                Log.w(TAG, "send failed", e)
            }
        }
    }

    fun close() {
        heartbeatJob?.cancel()
        readJob?.cancel()
        try { socket?.close() } catch (_: Throwable) {}
        socket = null
        onState?.invoke(State.Disconnected)
    }

    private suspend fun readLoop(s: SSLSocket) {
        val reader = BufferedReader(InputStreamReader(s.getInputStream()))
        val buf = CharArray(8 * 1024)
        try {
            while (currentCoroutineContext().isActive) {
                val n = reader.read(buf)
                if (n <= 0) break
                val chunk = String(buf, 0, n)
                Log.v(TAG, "RX raw (${n} chars): ${chunk.take(400)}")
                handleChunk(chunk)
            }
        } catch (e: CancellationException) {
            // expected on close
        } catch (e: Throwable) {
            Log.w(TAG, "read loop ended", e)
        }
        Log.i(TAG, "readLoop exit")
        close()
    }

    /**
     * Re-implements m1.f.L(): treat consecutive concatenated JSON objects in
     * one TCP read by splitting on `}{` and re-balancing each fragment.
     * Buffers a partial tail until the next read completes the object.
     */
    private fun handleChunk(chunk: String) {
        var data = partial + chunk
        partial = ""

        if (!data.contains("}{")) {
            try {
                val obj = org.json.JSONObject(data)
                Log.d(TAG, "RX: ${data.take(400)}")
                onMessage?.invoke(obj)
            } catch (_: org.json.JSONException) {
                partial = data
            }
            return
        }

        val parts = data.split("}{")
        for ((i, raw) in parts.withIndex()) {
            val piece = when {
                i == 0 -> "$raw}"
                i == parts.lastIndex -> "{$raw"
                else -> "{$raw}"
            }
            try {
                val obj = org.json.JSONObject(piece)
                Log.d(TAG, "RX: ${piece.take(400)}")
                onMessage?.invoke(obj)
            } catch (_: org.json.JSONException) {
                if (i == parts.lastIndex) partial = piece
                else Log.w(TAG, "skipped malformed fragment: ${piece.take(120)}")
            }
        }
    }

    private suspend fun heartbeatLoop() {
        val payload = org.json.JSONObject().apply { put("cmd", "heartbeat") }
        while (currentCoroutineContext().isActive) {
            delay(30_000)
            sendJson(payload)
        }
    }

    private val trustAll = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
    }
}
