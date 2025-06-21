package io.github.yearsyan.yaad.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yearsyan.yaad.R
import io.github.yearsyan.yaad.media.FFmpegTools
import io.github.yearsyan.yaad.model.FFmpegInstallType
import io.github.yearsyan.yaad.ui.components.*
import io.github.yearsyan.yaad.utils.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FFmpegSettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val settings by settingsManager.settings.collectAsState()
    val ffmpegConfig by remember { mutableStateOf(FFmpegTools.configuration()) }
    var showConfigPopup by remember { mutableStateOf(false) }

    var customUrl by remember { mutableStateOf(settings.ffmpegCustomUrl) }
    val bottomScrollState = rememberScrollState()

    if (showConfigPopup && ffmpegConfig.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { showConfigPopup = false }
        ) {
            Column(
                modifier = Modifier.padding(16.dp).height((configuration.screenHeightDp * 0.6f).dp).verticalScroll(bottomScrollState)
            ) {
                Text(
                    text = ffmpegConfig,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部应用栏
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.ffmpeg_settings),
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
                SettingsGroupTitle(
                    stringResource(R.string.ffmpeg_install_method)
                )
            }

            item {
                RadioGroupSettingsItem(
                    title = stringResource(R.string.ffmpeg_source),
                    subtitle = stringResource(R.string.ffmpeg_source_desc),
                    icon = Icons.Default.VideoLibrary,
                    options =
                        listOf(
                            FFmpegInstallType.BUILTIN to
                                stringResource(R.string.ffmpeg_builtin),
                            //                            FFmpegInstallType.DOWNLOAD to "自动下载",
                            //                            FFmpegInstallType.CUSTOM_URL to "自定义URL"
                        ),
                    selectedOption = settings.ffmpegInstallType,
                    onOptionSelected = { type ->
                        settingsManager.updateFFmpegSettings(type, customUrl)
                    }
                )
            }

            if (settings.ffmpegInstallType == FFmpegInstallType.CUSTOM_URL) {
                item {
                    SettingsGroupTitle(
                        stringResource(R.string.ffmpeg_custom_settings)
                    )
                }

                item {
                    TextFieldSettingsItem(
                        title = stringResource(R.string.ffmpeg_download_url),
                        subtitle =
                            stringResource(R.string.ffmpeg_download_url_desc),
                        icon = Icons.Default.Link,
                        value = customUrl,
                        onValueChange = {
                            customUrl = it
                            settingsManager.updateFFmpegSettings(
                                settings.ffmpegInstallType,
                                it
                            )
                        },
                        placeholder =
                            stringResource(
                                R.string.ffmpeg_download_url_placeholder
                            )
                    )
                }
            }

            item {
                SettingsGroupTitle(
                    stringResource(R.string.ffmpeg_configuration)
                )
            }

            item {
                Card(
                    modifier =
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    elevation =
                        CardDefaults.cardElevation(defaultElevation = 2.dp),
                    onClick = { showConfigPopup = true }
                ) {
                    Text(
                        text = ffmpegConfig,
                        modifier = Modifier.padding(16.dp),
                        maxLines = 5,
                        style = MaterialTheme.typography.bodySmall,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            item {
                SettingsGroupTitle(stringResource(R.string.ffmpeg_instructions))
            }

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
                                    R.string.ffmpeg_install_instructions_title
                                ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text =
                                stringResource(
                                    R.string.ffmpeg_install_instructions
                                ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
