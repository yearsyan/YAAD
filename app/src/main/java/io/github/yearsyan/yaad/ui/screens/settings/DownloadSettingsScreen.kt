package io.github.yearsyan.yaad.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.yearsyan.yaad.R
import io.github.yearsyan.yaad.ui.components.*
import io.github.yearsyan.yaad.utils.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadSettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val settings by settingsManager.settings.collectAsState()

    var showThreadDialog by remember { mutableStateOf(false) }
    var showSpeedLimitDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部应用栏
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.download_settings),
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                SettingsGroupTitle(stringResource(R.string.thread_settings))
            }

            item {
                SettingsItem(
                    title = stringResource(R.string.default_download_threads),
                    subtitle =
                        stringResource(
                            R.string.current_threads,
                            settings.defaultDownloadThreads
                        ),
                    icon = Icons.Default.Speed,
                    trailing = {
                        Row {
                            Text(
                                text = "${settings.defaultDownloadThreads}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null
                            )
                        }
                    },
                    onClick = { showThreadDialog = true }
                )
            }

            item { SettingsGroupTitle(stringResource(R.string.speed_limit)) }

            item {
                SettingsItem(
                    title = stringResource(R.string.bt_download_speed_limit),
                    subtitle =
                        if (settings.btDownloadSpeedLimit == 0L)
                            stringResource(R.string.no_limit)
                        else
                            stringResource(
                                R.string.speed_limit_format,
                                settings.btDownloadSpeedLimit
                            ),
                    icon = Icons.Default.Download,
                    trailing = {
                        Row {
                            Text(
                                text =
                                    if (settings.btDownloadSpeedLimit == 0L)
                                        stringResource(R.string.no_limit)
                                    else
                                        stringResource(
                                            R.string.speed_limit_format,
                                            settings.btDownloadSpeedLimit
                                        ),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null
                            )
                        }
                    },
                    onClick = { showSpeedLimitDialog = true }
                )
            }

            item {
                SettingsItem(
                    title = stringResource(R.string.bt_upload_speed_limit),
                    subtitle =
                        if (settings.btUploadSpeedLimit == 0L)
                            stringResource(R.string.no_limit)
                        else
                            stringResource(
                                R.string.speed_limit_format,
                                settings.btUploadSpeedLimit
                            ),
                    icon = Icons.Default.Upload,
                    trailing = {
                        Row {
                            Text(
                                text =
                                    if (settings.btUploadSpeedLimit == 0L)
                                        stringResource(R.string.no_limit)
                                    else
                                        stringResource(
                                            R.string.speed_limit_format,
                                            settings.btUploadSpeedLimit
                                        ),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null
                            )
                        }
                    },
                    onClick = { showSpeedLimitDialog = true }
                )
            }

            item { SettingsGroupTitle(stringResource(R.string.other_settings)) }

            item {
                SliderSettingsItem(
                    title = stringResource(R.string.auto_retry_count),
                    subtitle = stringResource(R.string.auto_retry_count_desc),
                    icon = Icons.Default.Refresh,
                    value = settings.autoRetryCount.toFloat(),
                    valueRange = 0f..10f,
                    steps = 10,
                    onValueChange = { value ->
                        val newSettings =
                            settings.copy(autoRetryCount = value.toInt())
                        settingsManager.saveSettings(newSettings)
                    },
                    valueFormatter = {
                        context.getString(
                            R.string.retry_count_format,
                            it.toInt()
                        )
                    }
                )
            }

            item { SettingsGroupTitle(stringResource(R.string.instructions)) }

            item {
                Card(
                    modifier =
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    elevation =
                        CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            text =
                                stringResource(
                                    R.string.download_settings_instructions
                                ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text =
                                stringResource(
                                    R.string
                                        .download_settings_instructions_content
                                ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // 线程数设置对话框
    if (showThreadDialog) {
        ThreadCountDialog(
            currentValue = settings.defaultDownloadThreads,
            onDismiss = { showThreadDialog = false },
            onConfirm = { threads ->
                settingsManager.updateDownloadThreads(threads)
            }
        )
    }

    // BT限速设置对话框
    if (showSpeedLimitDialog) {
        SpeedLimitDialog(
            currentDownloadSpeed = settings.btDownloadSpeedLimit,
            currentUploadSpeed = settings.btUploadSpeedLimit,
            onDismiss = { showSpeedLimitDialog = false },
            onConfirm = { downloadSpeed, uploadSpeed ->
                settingsManager.updateBtDownloadSpeedLimit(downloadSpeed)
                settingsManager.updateBtUploadSpeedLimit(uploadSpeed)
            }
        )
    }
}
