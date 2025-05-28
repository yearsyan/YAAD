package io.github.yearsyan.yaad.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.yearsyan.yaad.ui.components.SettingsGroupTitle
import io.github.yearsyan.yaad.ui.components.SettingsItem
import io.github.yearsyan.yaad.utils.SettingsManager

@Composable
fun SettingsMainScreen(
    onNavigateToSubSetting: (SettingsRoute) -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val settings by settingsManager.settings.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            // 页面标题
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "设置",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        item {
            SettingsGroupTitle("媒体处理")
        }
        
        item {
            SettingsItem(
                title = "FFmpeg设置",
                subtitle = when (settings.ffmpegInstallType) {
                    io.github.yearsyan.yaad.model.FFmpegInstallType.BUILTIN -> "内置版本"
                    io.github.yearsyan.yaad.model.FFmpegInstallType.DOWNLOAD -> "自动下载"
                    io.github.yearsyan.yaad.model.FFmpegInstallType.CUSTOM_URL -> "自定义URL"
                },
                icon = Icons.Default.VideoLibrary,
                trailing = {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null
                    )
                },
                onClick = { onNavigateToSubSetting(SettingsRoute.FFmpeg) }
            )
        }
        
        item {
            SettingsItem(
                title = "Cookie设置",
                subtitle = if (settings.cookieFilePath.isNotEmpty()) {
                    "已设置: ${settings.cookieFilePath.substringAfterLast("/")}"
                } else {
                    "未设置Cookie文件"
                },
                icon = Icons.Default.Cookie,
                trailing = {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null
                    )
                },
                onClick = { onNavigateToSubSetting(SettingsRoute.Cookie) }
            )
        }
        
        item {
            SettingsGroupTitle("下载配置")
        }
        
        item {
            SettingsItem(
                title = "下载设置",
                subtitle = buildString {
                    append("线程数: ${settings.defaultDownloadThreads}")
                    append(" | 下载限速: ")
                    append(if (settings.btDownloadSpeedLimit == 0L) "无限制" else "${settings.btDownloadSpeedLimit} KB/s")
                },
                icon = Icons.Default.CloudDownload,
                trailing = {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null
                    )
                },
                onClick = { onNavigateToSubSetting(SettingsRoute.Download) }
            )
        }
        
        item {
            SettingsGroupTitle("应用设置")
        }
        
        item {
            SettingsItem(
                title = "通知设置",
                subtitle = if (settings.enableNotifications) "已开启" else "已关闭",
                icon = Icons.Default.Notifications,
                trailing = {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null
                    )
                },
                onClick = { onNavigateToSubSetting(SettingsRoute.Notification) }
            )
        }
        
        item {
            SettingsGroupTitle("其他")
        }
        
        item {
            SettingsItem(
                title = "关于",
                subtitle = "版本信息和开源许可",
                icon = Icons.Default.Info,
                trailing = {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null
                    )
                },
                onClick = { onNavigateToSubSetting(SettingsRoute.About) }
            )
        }
        
        // 底部间距
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
} 