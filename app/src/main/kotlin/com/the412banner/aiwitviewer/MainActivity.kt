package com.the412banner.aiwitviewer

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.the412banner.aiwitviewer.data.AiwitClient
import com.the412banner.aiwitviewer.data.CloudMessagingClient
import com.the412banner.aiwitviewer.data.CredentialStore
import com.the412banner.aiwitviewer.data.Device
import com.the412banner.aiwitviewer.data.LiveSession
import com.the412banner.aiwitviewer.data.LocalAliasStore
import com.the412banner.aiwitviewer.data.Recording
import com.the412banner.aiwitviewer.ui.CameraDetailScreen
import com.the412banner.aiwitviewer.ui.CameraListScreen
import com.the412banner.aiwitviewer.ui.ClipsScreen
import com.the412banner.aiwitviewer.ui.LoginScreen
import com.the412banner.aiwitviewer.ui.RenameDialog
import com.the412banner.aiwitviewer.ui.epochMillisToYyyymmdd
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface Screen {
    data object Login : Screen
    data object Cameras : Screen
    data class CameraDetail(val device: Device) : Screen
    data class Clips(val device: Device) : Screen
}

class MainActivity : ComponentActivity() {

    private lateinit var creds: CredentialStore
    private lateinit var aliases: LocalAliasStore
    private lateinit var client: AiwitClient
    private lateinit var live: LiveSession
    private val messaging = CloudMessagingClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        creds = CredentialStore(this)
        aliases = LocalAliasStore(this)
        val appSn = creds.appSn ?: "APK_${UUID.randomUUID()}".also { creds.appSn = it }
        client = AiwitClient(appSn)
        live = LiveSession(applicationContext)

        // Connect the diagnostic messaging client as soon as we have a session.
        // It logs everything to logcat under the "CloudMsg" tag so we can
        // characterize the protocol from the live server without needing
        // another mitmproxy pass.
        messaging.setOnMessage { obj ->
            val cmd = obj.optString("cmd")
            android.util.Log.i("CloudMsg", "parsed-msg cmd=$cmd keys=${obj.keys().asSequence().toList()}")
            // The server hands us session params via TWO different messages:
            //   - The reply to our own `wakeup` cmd (cmd="wakeup", err_no=0, pk, ip, ports)
            //   - An unsolicited `preview-start` notification (cmd="preview-start",
            //     msg_type="notification", state=1, same pk + ports)
            // Either is fine — both carry the same per-session pk. Use whichever
            // arrives first.
            if ((cmd == "preview-start" || cmd == "wakeup") && obj.optInt("err_no", -1) == 0) {
                val peer = obj.optString("peer", "")
                val pk = obj.optString("pk", "")
                if (peer.isNotBlank() && pk.isNotBlank()) {
                    live.onPreviewStartReply(peer, pk)
                }
            }
        }
        messaging.setOnState { state ->
            android.util.Log.i("CloudMsg", "state=$state")
            if (state == CloudMessagingClient.State.Connected) {
                // Send the real cmd-server handshake (matches m1/a.java line 148).
                val appSn = creds.appSn ?: return@setOnState
                val email = creds.email ?: return@setOnState
                messaging.sendAppLogin(appSn = appSn, username = email)
            }
        }

        setContent {
            val scheme = remember { darkColorScheme() }
            MaterialTheme(colorScheme = scheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }

    @Composable
    private fun AppRoot() {
        var screen: Screen by remember {
            mutableStateOf(if (creds.isLoggedIn()) Screen.Cameras else Screen.Login)
        }

        // Login state
        var loginError by remember { mutableStateOf<String?>(null) }
        var loginBusy by remember { mutableStateOf(false) }

        // Cameras
        var devices by remember { mutableStateOf<List<Device>>(emptyList()) }
        var devicesLoading by remember { mutableStateOf(false) }
        var devicesError by remember { mutableStateOf<String?>(null) }
        var selectedDeviceSn by remember { mutableStateOf<String?>(null) }
        var renameTarget by remember { mutableStateOf<Device?>(null) }
        // Bump this to force the list to re-resolve display names after a rename.
        var aliasRevision by remember { mutableIntStateOf(0) }

        // Clips
        var clips by remember { mutableStateOf<List<Recording>>(emptyList()) }
        var clipsLoading by remember { mutableStateOf(false) }
        var clipsError by remember { mutableStateOf<String?>(null) }
        var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

        // Auto-load camera list whenever we land on the Cameras screen with an empty list.
        // Covers both fresh-app-start-with-saved-creds AND the first transition after
        // a fresh login (the previous LaunchedEffect(Unit) only fired once).
        LaunchedEffect(screen) {
            if (screen == Screen.Cameras && devices.isEmpty() && !devicesLoading) {
                refreshDevices(
                    onStart = { devicesLoading = true; devicesError = null },
                    onResult = { result, err ->
                        devicesLoading = false
                        if (err == null) devices = result else devicesError = err
                    },
                )
            }
        }

        when (val s = screen) {
            Screen.Login -> LoginScreen(
                initialEmail = creds.email.orEmpty(),
                isWorking = loginBusy,
                errorText = loginError,
                onSubmit = { email, password ->
                    loginBusy = true
                    loginError = null
                    lifecycleScope.launch {
                        try {
                            client.login(email, password)
                            creds.email = email
                            creds.password = password
                            screen = Screen.Cameras
                            // LaunchedEffect(screen) will pick up the device fetch.
                        } catch (e: Exception) {
                            loginError = e.message ?: "Login failed"
                        } finally {
                            loginBusy = false
                        }
                    }
                },
            )

            Screen.Cameras -> {
                aliasRevision  // observe so rename writes recompose the list
                CameraListScreen(
                    devices = devices,
                    selectedDeviceSn = selectedDeviceSn,
                    displayNameFor = { aliases.displayName(it) },
                    isLoading = devicesLoading,
                    errorText = devicesError,
                    onRefresh = {
                        refreshDevices(
                            onStart = { devicesLoading = true; devicesError = null },
                            onResult = { result, err ->
                                devicesLoading = false
                                if (err == null) devices = result else devicesError = err
                            },
                        )
                    },
                    onLogout = {
                        creds.clear()
                        devices = emptyList()
                        selectedDeviceSn = null
                        screen = Screen.Login
                    },
                    onSelectDevice = { d ->
                        selectedDeviceSn = d.device_sn
                        screen = Screen.CameraDetail(d)
                        startLiveViewFlow(
                            d,
                            getSelectedSn = { selectedDeviceSn },
                            onDevicesRefreshed = { devices = it },
                        )
                    },
                    onOpenClips = { d ->
                        selectedDeviceSn = d.device_sn
                        selectedDateMillis = System.currentTimeMillis()
                        screen = Screen.Clips(d)
                        refreshClips(
                            device = d,
                            yyyymmdd = epochMillisToYyyymmdd(selectedDateMillis),
                            onStart = { clipsLoading = true; clipsError = null; clips = emptyList() },
                            onResult = { result, err ->
                                clipsLoading = false
                                if (err == null) clips = result else clipsError = err
                            },
                        )
                    },
                    onSnapshot = { d ->
                        // Diagnostic: ask the cloud to wake this camera and reply with
                        // a preview-start push containing the live-session params. Watch
                        // logcat tag CloudMsg for the response.
                        val appSn = creds.appSn
                        if (appSn != null) {
                            messaging.sendPreviewStart(appSn, d.device_sn)
                            Toast.makeText(
                                this@MainActivity,
                                "preview-start sent for ${d.name.ifBlank { d.device_sn }} — watch logcat",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                    onRequestRename = { d -> renameTarget = d },
                    liveFrameFlow = { sn -> live.frames(sn) },
                )
            }

            is Screen.CameraDetail -> {
                // Use the latest device from `devices` so state updates flow through.
                val current = devices.firstOrNull { it.device_sn == s.device.device_sn } ?: s.device
                CameraDetailScreen(
                    device = current,
                    displayName = aliases.displayName(current),
                    liveFrame = live.frames(current.device_sn),
                    onBack = { screen = Screen.Cameras },
                    onOpenClips = {
                        selectedDateMillis = System.currentTimeMillis()
                        refreshClips(
                            device = current,
                            yyyymmdd = epochMillisToYyyymmdd(selectedDateMillis),
                            onStart = { clipsLoading = true; clipsError = null; clips = emptyList() },
                            onResult = { result, err ->
                                clipsLoading = false
                                if (err == null) clips = result else clipsError = err
                            },
                        )
                        screen = Screen.Clips(current)
                    },
                    onSnapshot = { snapshotPlaceholder(current) },
                    onRequestRename = { renameTarget = current },
                    onSwitchCamera = {
                        if (devices.isNotEmpty()) {
                            val idx = devices.indexOfFirst { it.device_sn == current.device_sn }
                            val next = devices[(idx + 1) % devices.size]
                            selectedDeviceSn = next.device_sn
                            screen = Screen.CameraDetail(next)
                            startLiveViewFlow(
                                next,
                                getSelectedSn = { selectedDeviceSn },
                                onDevicesRefreshed = { devices = it },
                            )
                        }
                    },
                )
            }

            is Screen.Clips -> ClipsScreen(
                deviceName = aliases.displayName(s.device),
                isDeviceOnline = s.device.state == 1,
                clips = clips,
                selectedDateEpochMillis = selectedDateMillis,
                isLoading = clipsLoading,
                errorText = clipsError,
                cacheDirPath = cacheDir.absolutePath,
                signedUrlProvider = { rec -> client.signedDownloadUrl(rec, expiresIn = 600) },
                onBack = { screen = Screen.Cameras },
                onRefresh = {
                    refreshClips(
                        s.device, epochMillisToYyyymmdd(selectedDateMillis),
                        onStart = { clipsLoading = true; clipsError = null },
                        onResult = { result, err ->
                            clipsLoading = false
                            if (err == null) clips = result else clipsError = err
                        },
                    )
                },
                onPickDate = { newMillis ->
                    selectedDateMillis = newMillis
                    refreshClips(
                        s.device, epochMillisToYyyymmdd(newMillis),
                        onStart = { clipsLoading = true; clipsError = null; clips = emptyList() },
                        onResult = { result, err ->
                            clipsLoading = false
                            if (err == null) clips = result else clipsError = err
                        },
                    )
                },
                onDownloadClip = { clip -> enqueueDownload(clip) },
            )
        }

        // Rename dialog — shown from either the Cameras screen's long-press
        // menu or the CameraDetail screen's "Rename (local)" button.
        renameTarget?.let { target ->
            RenameDialog(
                initialName = aliases.displayName(target),
                onDismiss = { renameTarget = null },
                onConfirm = { newName ->
                    val typed = newName.trim()
                    val alias = typed.takeIf { it.isNotBlank() && it != target.name }
                    aliases.setAlias(target.device_sn, alias)
                    aliasRevision++
                    renameTarget = null
                },
            )
        }
    }

    /**
     * Replays AIWIT's MITM-captured live-view start sequence:
     *   preview-finish (clear any stale viewer slot) ->
     *   wakeup -> devices-state -> wakeup retry -> ping/10s
     */
    private fun startLiveViewFlow(
        d: Device,
        getSelectedSn: () -> String?,
        onDevicesRefreshed: (List<Device>) -> Unit,
    ) {
        val sn = creds.appSn ?: return
        val em = creds.email ?: return
        lifecycleScope.launch {
            messaging.sendPreviewFinish(sn, d.device_sn)
            kotlinx.coroutines.delay(500)
            messaging.sendWakeup(sn, d.device_sn)
            kotlinx.coroutines.delay(1500)
            messaging.sendDevicesState(sn)
            kotlinx.coroutines.delay(1500)
            messaging.sendWakeup(sn, d.device_sn)

            while (getSelectedSn() == d.device_sn) {
                messaging.sendPing(sn, d.device_sn, em)
                kotlinx.coroutines.delay(10_000)
            }
            messaging.sendPreviewFinish(sn, d.device_sn)
        }
        lifecycleScope.launch {
            repeat(10) {
                kotlinx.coroutines.delay(3000)
                if (getSelectedSn() != d.device_sn) return@launch
                try { onDevicesRefreshed(client.listDevices()) } catch (_: Exception) {}
            }
        }
    }

    /** Snapshot stub for now — wired through to UI so the button works. */
    private fun snapshotPlaceholder(d: Device) {
        Toast.makeText(
            this@MainActivity,
            "Snapshot will work once live view is wired up.",
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun enqueueDownload(clip: Recording) {
        lifecycleScope.launch {
            try {
                val url = client.signedDownloadUrl(clip, expiresIn = 600)
                val displayName = clip.fileName.substringAfterLast('/')
                val request = DownloadManager.Request(Uri.parse(url))
                    .setTitle(displayName)
                    .setDescription("AIWIT clip from ${clip.deviceName.ifBlank { clip.deviceSn }}")
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, displayName)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setAllowedOverMetered(true)
                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)
                Toast.makeText(this@MainActivity, "Downloading $displayName", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Download failed: ${e.message}",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    private fun refreshDevices(
        onStart: () -> Unit,
        onResult: (List<Device>, String?) -> Unit,
    ) {
        onStart()
        lifecycleScope.launch {
            try {
                if (client.currentSession() == null) {
                    val u = creds.email ?: throw IllegalStateException("No saved email")
                    val p = creds.password ?: throw IllegalStateException("No saved password")
                    client.login(u, p)
                }
                val devs = client.listDevices()
                onResult(devs, null)
                // Bootstrap the live P2P stack with the config block we just got.
                client.lastConfig?.let { cfg ->
                    val sn = creds.appSn ?: return@let
                    live.bootstrap(sn, cfg)
                }
                // After we have a session, kick the diagnostic messaging client.
                messaging.connect()
            } catch (e: Exception) {
                onResult(emptyList(), e.message ?: e.javaClass.simpleName)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        messaging.close()
    }

    private fun refreshClips(
        device: Device,
        yyyymmdd: String,
        onStart: () -> Unit,
        onResult: (List<Recording>, String?) -> Unit,
    ) {
        onStart()
        lifecycleScope.launch {
            try {
                onResult(client.listRecordings(device.device_sn, yyyymmdd), null)
            } catch (e: Exception) {
                onResult(emptyList(), e.message ?: e.javaClass.simpleName)
            }
        }
    }
}
