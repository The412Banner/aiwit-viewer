package com.the412banner.aiwitviewer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.the412banner.aiwitviewer.data.Recording
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipsScreen(
    deviceName: String,
    clips: List<Recording>,
    selectedDateEpochMillis: Long,
    isLoading: Boolean,
    errorText: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onPickDate: (epochMillis: Long) -> Unit,
    onSelectClip: (Recording) -> Unit,
    onDownloadClip: (Recording) -> Unit,
) {
    val dayLabel = remember(selectedDateEpochMillis) {
        SimpleDateFormat("EEE, MMM d yyyy", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }.format(Date(selectedDateEpochMillis))
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val refreshState = rememberPullToRefreshState()
    if (refreshState.isRefreshing) {
        LaunchedEffect(Unit) {
            onRefresh()
        }
    }
    LaunchedEffect(isLoading) {
        if (!isLoading) refreshState.endRefresh()
    }

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
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.CalendarToday, contentDescription = "Pick date")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .nestedScroll(refreshState.nestedScrollConnection),
        ) {
            when {
                isLoading && clips.isEmpty() -> {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorText != null && clips.isEmpty() -> {
                    Column(
                        Modifier.padding(24.dp).fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(errorText, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onRefresh) { Text("Retry") }
                    }
                }
                clips.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No clips on $dayLabel.")
                    }
                }
                else -> {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(clips, key = { it.fileName }) { c ->
                            ClipRow(c, onClick = { onSelectClip(c) }, onDownload = { onDownloadClip(c) })
                        }
                    }
                }
            }

            PullToRefreshContainer(
                state = refreshState,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateEpochMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onPickDate(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun ClipRow(c: Recording, onClick: () -> Unit, onDownload: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(c.time.ifBlank { c.fileName.substringAfterLast('/') }) },
        supportingContent = {
            val seconds = c.duration / 1000.0
            val kib = c.length / 1024.0
            Text("%.1fs • %.0f KiB • %s".format(seconds, kib, triggerLabel(c.type)))
        },
        trailingContent = {
            IconButton(onClick = onDownload) {
                Icon(Icons.Filled.Download, contentDescription = "Download .ts2")
            }
        },
    )
    HorizontalDivider()
}

private fun triggerLabel(type: String): String = when (type) {
    "pir" -> "motion"
    "wifi" -> "live"
    else -> type.ifBlank { "?" }
}

/** Convert an epoch-millis (any tz) to a YYYYMMDD string in the device's local timezone. */
fun epochMillisToYyyymmdd(epochMillis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = epochMillis }
    return "%04d%02d%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
}
