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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.yearsyan.yaad.R
import io.github.yearsyan.yaad.ui.components.SettingsGroupTitle
import io.github.yearsyan.yaad.ui.components.SettingsItem
import io.github.yearsyan.yaad.utils.SettingsManager

@Composable
fun SettingsMainScreen(onNavigateToSubSetting: (SettingsRoute) -> Unit) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val settings by settingsManager.settings.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            // 页面标题
            Row(
                modifier =
                    Modifier.fillMaxWidth()
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
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item { SettingsGroupTitle(stringResource(R.string.media_processing)) }

        item {
            SettingsItem(
                title = stringResource(R.string.ffmpeg_settings),
                subtitle =
                    when (settings.ffmpegInstallType) {
                        io.github.yearsyan.yaad.model.FFmpegInstallType
                            .BUILTIN ->
                            stringResource(R.string.ffmpeg_builtin_version)
                        io.github.yearsyan.yaad.model.FFmpegInstallType
                            .DOWNLOAD ->
                            stringResource(R.string.ffmpeg_auto_download)
                        io.github.yearsyan.yaad.model.FFmpegInstallType
                            .CUSTOM_URL ->
                            stringResource(R.string.ffmpeg_custom_url)
                    },
                icon = Icons.Default.VideoLibrary,
                trailing = {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                },
                onClick = { onNavigateToSubSetting(SettingsRoute.FFmpeg) }
            )
        }

        item {
            SettingsItem(
                title = stringResource(R.string.cookie_settings),
                subtitle =
                    if (settings.cookieFilePath.isNotEmpty()) {
                        stringResource(
                            R.string.cookie_file_set,
                            settings.cookieFilePath.substringAfterLast("/")
                        )
                    } else {
                        stringResource(R.string.no_cookie_file)
                    },
                icon = Icons.Default.Cookie,
                trailing = {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                },
                onClick = { onNavigateToSubSetting(SettingsRoute.Cookie) }
            )
        }

        item { SettingsGroupTitle(stringResource(R.string.download_config)) }

        item {
            SettingsItem(
                title = stringResource(R.string.download_settings),
                subtitle =
                    buildString {
                        append(
                            stringResource(
                                R.string.download_threads_format,
                                settings.defaultDownloadThreads
                            )
                        )
                        append(" | ")
                        append(
                            stringResource(
                                R.string.download_speed_limit_format,
                                if (settings.btDownloadSpeedLimit == 0L)
                                    stringResource(R.string.no_limit)
                                else "${settings.btDownloadSpeedLimit} KB/s"
                            )
                        )
                    },
                icon = Icons.Default.CloudDownload,
                trailing = {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                },
                onClick = { onNavigateToSubSetting(SettingsRoute.Download) }
            )
        }

        item { SettingsGroupTitle(stringResource(R.string.app_settings)) }

        item {
            SettingsItem(
                title = stringResource(R.string.notification_settings),
                subtitle =
                    if (settings.enableNotifications)
                        stringResource(R.string.notification_enabled)
                    else stringResource(R.string.notification_disabled),
                icon = Icons.Default.Notifications,
                trailing = {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                },
                onClick = { onNavigateToSubSetting(SettingsRoute.Notification) }
            )
        }

        item { SettingsGroupTitle(stringResource(R.string.other)) }

        item {
            SettingsItem(
                title = stringResource(R.string.about),
                subtitle = stringResource(R.string.about_subtitle),
                icon = Icons.Default.Info,
                trailing = {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                },
                onClick = { onNavigateToSubSetting(SettingsRoute.About) }
            )
        }

        // 底部间距
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}
