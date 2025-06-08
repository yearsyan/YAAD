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
import io.github.yearsyan.yaad.model.FFmpegInstallType
import io.github.yearsyan.yaad.ui.components.*
import io.github.yearsyan.yaad.utils.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FFmpegSettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val settings by settingsManager.settings.collectAsState()

    var customUrl by remember { mutableStateOf(settings.ffmpegCustomUrl) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部应用栏
        TopAppBar(
            title = { Text(text = "FFmpeg设置", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item { SettingsGroupTitle("安装方式") }

            item {
                RadioGroupSettingsItem(
                    title = "FFmpeg获取方式",
                    subtitle = "选择FFmpeg的安装来源",
                    icon = Icons.Default.VideoLibrary,
                    options =
                        listOf(
                            FFmpegInstallType.BUILTIN to "内置版本",
                            FFmpegInstallType.DOWNLOAD to "自动下载",
                            FFmpegInstallType.CUSTOM_URL to "自定义URL"
                        ),
                    selectedOption = settings.ffmpegInstallType,
                    onOptionSelected = { type ->
                        settingsManager.updateFFmpegSettings(type, customUrl)
                    }
                )
            }

            if (settings.ffmpegInstallType == FFmpegInstallType.CUSTOM_URL) {
                item { SettingsGroupTitle("自定义设置") }

                item {
                    TextFieldSettingsItem(
                        title = "下载链接",
                        subtitle = "输入FFmpeg的下载URL",
                        icon = Icons.Default.Link,
                        value = customUrl,
                        onValueChange = {
                            customUrl = it
                            settingsManager.updateFFmpegSettings(
                                settings.ffmpegInstallType,
                                it
                            )
                        },
                        placeholder = "https://example.com/ffmpeg.zip"
                    )
                }
            }

            item { SettingsGroupTitle("说明") }

            item {
                Card(
                    modifier =
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    elevation =
                        CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            text = "安装方式说明",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text =
                                "• 内置版本：使用应用内置的FFmpeg库\n" +
                                    "• 自动下载：从官方源自动下载最新版本\n" +
                                    "• 自定义URL：从指定链接下载FFmpeg",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
