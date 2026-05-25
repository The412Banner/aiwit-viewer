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
import com.the412banner.aiwitviewer.data.CredentialStore
import com.the412banner.aiwitviewer.data.Device
import com.the412banner.aiwitviewer.data.Recording
import com.the412banner.aiwitviewer.ui.CameraListScreen
import com.the412banner.aiwitviewer.ui.ClipsScreen
import com.the412banner.aiwitviewer.ui.LoginScreen
import com.the412banner.aiwitviewer.ui.PlayerScreen
import com.the412banner.aiwitviewer.ui.epochMillisToYyyymmdd
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface Screen {
    data object Login : Screen
    data object Cameras : Screen
    data class Clips(val device: Device) : Screen
    data class Player(val clip: Recording, val device: Device) : Screen
}

class MainActivity : ComponentActivity() {

    private lateinit var creds: CredentialStore
    private lateinit var client: AiwitClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        creds = CredentialStore(this)
        val appSn = creds.appSn ?: "APK_${UUID.randomUUID()}".also { creds.appSn = it }
        client = AiwitClient(appSn)

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

        // Clips
        var clips by remember { mutableStateOf<List<Recording>>(emptyList()) }
        var clipsLoading by remember { mutableStateOf(false) }
        var clipsError by remember { mutableStateOf<String?>(null) }
        var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

        // Auto-login on first composition if creds saved
        LaunchedEffect(Unit) {
            if (screen == Screen.Cameras && client.currentSession() == null) {
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
                        } catch (e: Exception) {
                            loginError = e.message ?: "Login failed"
                        } finally {
                            loginBusy = false
                        }
                    }
                },
            )

            Screen.Cameras -> CameraListScreen(
                devices = devices,
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
                    screen = Screen.Login
                },
                onSelectDevice = { d ->
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
            )

            is Screen.Clips -> ClipsScreen(
                deviceName = s.device.name.ifBlank { s.device.device_sn },
                isDeviceOnline = s.device.state == 1,
                clips = clips,
                selectedDateEpochMillis = selectedDateMillis,
                isLoading = clipsLoading,
                errorText = clipsError,
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
                onSelectClip = { clip ->
                    screen = Screen.Player(clip, s.device)
                },
                onDownloadClip = { clip ->
                    enqueueDownload(clip)
                },
            )

            is Screen.Player -> PlayerScreen(
                clip = s.clip,
                cacheDirPath = cacheDir.absolutePath,
                signedUrlProvider = { rec -> client.signedDownloadUrl(rec, expiresIn = 600) },
                onBack = { screen = Screen.Clips(s.device) },
            )
        }
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
                onResult(client.listDevices(), null)
            } catch (e: Exception) {
                onResult(emptyList(), e.message ?: e.javaClass.simpleName)
            }
        }
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
