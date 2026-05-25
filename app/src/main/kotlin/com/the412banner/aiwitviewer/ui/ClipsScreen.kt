package com.the412banner.aiwitviewer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.the412banner.aiwitviewer.data.Recording

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipsScreen(
    deviceName: String,
    clips: List<Recording>,
    dayLabel: String,
    isLoading: Boolean,
    errorText: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSelectClip: (Recording) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(deviceName)
                        Text(dayLabel, style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                isLoading && clips.isEmpty() -> {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorText != null && clips.isEmpty() -> {
                    Column(
                        Modifier.padding(24.dp).fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    ) {
                        Text(errorText, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onRefresh) { Text("Retry") }
                    }
                }
                clips.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("No clips for this day.")
                    }
                }
                else -> {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(clips) { c -> ClipRow(c, onClick = { onSelectClip(c) }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClipRow(c: Recording, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(c.time.ifBlank { c.fileName.substringAfterLast('/') }) },
        supportingContent = {
            val seconds = c.duration / 1000.0
            val kib = c.length / 1024.0
            Text("%.1fs • %.0f KiB • %s".format(seconds, kib, triggerLabel(c.type)))
        },
    )
    HorizontalDivider()
}

private fun triggerLabel(type: String): String = when (type) {
    "pir" -> "motion"
    "wifi" -> "live"
    else -> type.ifBlank { "?" }
}
