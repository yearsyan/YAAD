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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.yearsyan.yaad.ui.components.*
import io.github.yearsyan.yaad.utils.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val settings by settingsManager.settings.collectAsState()
    
    var showThreadDialog by remember { mutableStateOf(false) }
    var showSpeedLimitDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部应用栏
        TopAppBar(
            title = { 
                Text(
                    text = "下载设置",
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            }
        )
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                SettingsGroupTitle("线程设置")
            }
            
            item {
                SettingsItem(
                    title = "默认下载线程数",
                    subtitle = "当前: ${settings.defaultDownloadThreads} 线程",
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
            
            item {
                SettingsGroupTitle("速度限制")
            }
            
            item {
                SettingsItem(
                    title = "BT下载限速",
                    subtitle = if (settings.btDownloadSpeedLimit == 0L) "无限制" else "${settings.btDownloadSpeedLimit} KB/s",
                    icon = Icons.Default.Download,
                    trailing = {
                        Row {
                            Text(
                                text = if (settings.btDownloadSpeedLimit == 0L) "无限制" else "${settings.btDownloadSpeedLimit} KB/s",
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
                    title = "BT上传限速",
                    subtitle = if (settings.btUploadSpeedLimit == 0L) "无限制" else "${settings.btUploadSpeedLimit} KB/s",
                    icon = Icons.Default.Upload,
                    trailing = {
                        Row {
                            Text(
                                text = if (settings.btUploadSpeedLimit == 0L) "无限制" else "${settings.btUploadSpeedLimit} KB/s",
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
                SettingsGroupTitle("其他设置")
            }
            
            item {
                SliderSettingsItem(
                    title = "自动重试次数",
                    subtitle = "下载失败时的自动重试次数",
                    icon = Icons.Default.Refresh,
                    value = settings.autoRetryCount.toFloat(),
                    valueRange = 0f..10f,
                    steps = 10,
                    onValueChange = { value ->
                        val newSettings = settings.copy(autoRetryCount = value.toInt())
                        settingsManager.saveSettings(newSettings)
                    },
                    valueFormatter = { "${it.toInt()} 次" }
                )
            }
            
            item {
                SettingsGroupTitle("说明")
            }
            
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "下载设置说明",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• 线程数：建议设置为1-16，过多线程可能影响性能\n" +
                                    "• 限速设置：0表示无限制，单位为KB/s\n" +
                                    "• 重试次数：网络不稳定时建议设置3-5次",
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