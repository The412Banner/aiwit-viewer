package com.the412banner.aiwitviewer

import android.os.Bundle
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed interface Screen {
    data object Login : Screen
    data object Cameras : Screen
    data class Clips(val device: Device) : Screen
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
                    screen = Screen.Clips(d)
                    val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                    refreshClips(
                        device = d,
                        yyyymmdd = today,
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
                clips = clips,
                dayLabel = SimpleDateFormat("EEE, MMM d", Locale.US).format(Date()),
                isLoading = clipsLoading,
                errorText = clipsError,
                onBack = { screen = Screen.Cameras },
                onRefresh = {
                    val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                    refreshClips(s.device, today,
                        onStart = { clipsLoading = true; clipsError = null },
                        onResult = { result, err ->
                            clipsLoading = false
                            if (err == null) clips = result else clipsError = err
                        },
                    )
                },
                onSelectClip = { clip ->
                    // Playback wiring is the next session's job.
                    clipsError = "Playback coming next session — clip URL ready, native player not wired yet."
                },
            )
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
