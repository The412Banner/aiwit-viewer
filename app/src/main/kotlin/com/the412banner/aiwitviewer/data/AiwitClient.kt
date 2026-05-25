package com.the412banner.aiwitviewer.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.UUID
import javax.net.ssl.HttpsURLConnection

/**
 * Kotlin port of the Python AIWIT client. The cloud REST API is shared by
 * many EKEN/AIWIT rebrands (Kemo Pro, EKEN, TMEZON, ZUMIMALL, FISHBOT, ...).
 *
 * Reverse-engineered from `com.eken.aiwit` 3.5.6 (jadx). Cert pinning is NOT
 * enforced by the AIWIT app (trust-all SSL context in `j2/a.java`); we keep
 * normal CA validation in our own app since there's no reason to weaken it.
 */
class AiwitClient(private val appSn: String = "APK_${UUID.randomUUID()}") {

    companion object {
        private const val BASE = "https://api.v2.gdxp.com"
        private const val SIGN_SALT = "EKDB_ni&Hb&Zt&zz^7qn9"
        private const val APP_VERSION = "3.5.6"
        private const val USER_AGENT = "okhttp/4.10.0"
    }

    @Volatile private var sessionId: String? = null
    @Volatile var username: String? = null
        private set
    @Volatile var uid: Long? = null
        private set
    @Volatile var lastConfig: ConnectionConfig? = null
        private set
    private var ossTokenCache: OssToken? = null
    private var ossTokenExpiresAt: Long = 0L

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun resumeSession(sessionId: String, username: String?, uid: Long?) {
        this.sessionId = sessionId
        this.username = username
        this.uid = uid
    }

    fun currentSession(): String? = sessionId

    suspend fun login(username: String, password: String) = withContext(Dispatchers.IO) {
        val sign = md5Hex("$username/$password$SIGN_SALT")
        val body = mapOf(
            "username" to username,
            "password" to password,
            "pushToken" to "pushToken",
            "sign" to sign,
            "appSn" to appSn,
        ).toFormBody()

        val (status, raw) = httpRequest(
            method = "POST",
            url = "$BASE/app_group/login",
            body = body,
            contentType = "application/x-www-form-urlencoded",
        )
        if (status != 200) throw AiwitException("Login HTTP $status: ${raw.take(200)}")
        val env = json.decodeFromString<ApiEnvelope<LoginContent>>(raw)
        if (env.resultCode != 0 || env.content == null) {
            throw AiwitException("Login failed: ${env.msg}")
        }
        this@AiwitClient.sessionId = env.content.session_id
        this@AiwitClient.username = env.content.username
        this@AiwitClient.uid = env.content.uid
    }

    suspend fun listDevices(): List<Device> = withContext(Dispatchers.IO) {
        val sid = requireSession()
        val raw = getJsonRaw("$BASE/app_group/list_v2/$sid/$appSn")
        val env = json.decodeFromString<ApiEnvelope<DeviceListContent>>(raw)
        if (env.resultCode != 0 || env.content == null) {
            throw AiwitException("list_v2 failed: ${env.msg}")
        }
        env.content.config?.let { this@AiwitClient.lastConfig = it }
        env.content.list
    }

    suspend fun listRecordings(deviceSn: String, yyyymmdd: String): List<Recording> = withContext(Dispatchers.IO) {
        val sid = requireSession()
        val raw = getJsonRaw("$BASE/oss_get_files_v11/$sid/$yyyymmdd/$deviceSn/1/0/0")
        val env = json.decodeFromString<ApiEnvelope<RecordingsContent>>(raw)
        if (env.resultCode != 0 || env.content == null) {
            throw AiwitException("oss_get_files_v11 failed: ${env.msg}")
        }
        env.content.items
    }

    suspend fun ossToken(ossFromId: Int = 5): OssToken = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis() / 1000L
        ossTokenCache?.let { if (ossTokenExpiresAt > now + 60) return@withContext it }
        val sid = requireSession()
        val raw = getJsonRaw("$BASE/sts/token/$sid/oss-us-west-1.aliyuncs.com/$ossFromId")
        val env = json.decodeFromString<ApiEnvelope<OssToken>>(raw)
        val token = env.content ?: throw AiwitException("sts/token failed: ${env.msg}")
        ossTokenCache = token
        // "2026-05-25T19:58:25Z" -> epoch seconds, naive parse
        val parts = token.expiration.removeSuffix("Z").split("T")
        val date = parts[0].split("-").map { it.toInt() }
        val time = parts[1].split(":").map { it.toInt() }
        val cal = java.util.GregorianCalendar(java.util.TimeZone.getTimeZone("UTC"))
        cal.set(date[0], date[1] - 1, date[2], time[0], time[1], time[2])
        ossTokenExpiresAt = cal.timeInMillis / 1000L
        token
    }

    suspend fun signedDownloadUrl(recording: Recording, expiresIn: Long = 300L): String {
        val tok = ossToken(recording.ossFromId)
        return OssSigner.presignedGet(
            bucket = recording.bucket,
            endpoint = recording.endpoint,
            key = recording.fileName,
            token = tok,
            expiresInSeconds = expiresIn,
        )
    }

    // --- helpers ---

    private fun requireSession(): String =
        sessionId ?: throw AiwitException("Not logged in")

    private fun headers(): Map<String, String> = mapOf(
        "AppName" to "aiwit",
        "AppLang" to "en",
        "AppVersion" to APP_VERSION,
        "AppID" to appSn,
        "OS" to "2",
        "User-Agent" to USER_AGENT,
    )

    private fun getJsonRaw(url: String): String {
        val (status, raw) = httpRequest("GET", url, body = null, contentType = null)
        if (status != 200) throw AiwitException("HTTP $status on $url: ${raw.take(200)}")
        return raw
    }

    private fun httpRequest(
        method: String,
        url: String,
        body: ByteArray?,
        contentType: String?,
    ): Pair<Int, String> {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = false
            headers().forEach { (k, v) -> setRequestProperty(k, v) }
            if (contentType != null) setRequestProperty("Content-Type", contentType)
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Length", body.size.toString())
                outputStream.use { it.write(body) }
            }
        }
        val code = conn.responseCode
        val stream = if (code in 200..399) conn.inputStream else conn.errorStream
        val raw = stream?.bufferedReader()?.use { it.readText() } ?: ""
        return code to raw
    }

    private fun Map<String, String>.toFormBody(): ByteArray =
        entries.joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }.toByteArray()

    private fun md5Hex(s: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(s.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

class AiwitException(message: String) : RuntimeException(message)
