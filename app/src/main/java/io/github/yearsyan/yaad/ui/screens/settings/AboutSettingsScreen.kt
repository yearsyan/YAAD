package io.github.yearsyan.yaad.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.yearsyan.yaad.BuildConfig
import io.github.yearsyan.yaad.BuildInfo
import io.github.yearsyan.yaad.ui.components.*
import io.github.yearsyan.yaad.web.WebViewActivity


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var showVersionDetail by remember { mutableStateOf(false) }
    var showLicenseDetail by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部应用栏
        TopAppBar(
            title = { Text(text = "关于", fontWeight = FontWeight.Bold) },
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
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item { SettingsGroupTitle("应用信息") }

            item {
                SettingsItem(
                    title = "版本信息",
                    subtitle = "YAAD ${BuildConfig.VERSION_NAME} #${BuildInfo.GIT_HASH}",
                    icon = Icons.Default.Info,
                    onClick = {
                        showVersionDetail = true
                    }
                )
            }

            item {
                SettingsItem(
                    title = "构建日期",
                    subtitle = BuildInfo.BUILD_TIME,
                    icon = Icons.Default.CalendarToday
                )
            }

            item { SettingsGroupTitle("开源信息") }

            item {
                SettingsItem(
                    title = "开源许可",
                    subtitle = "查看开源许可证",
                    icon = Icons.Default.Description,
                    onClick = {
                        showLicenseDetail = true
                    }
                )
            }

            item {
                SettingsItem(
                    title = "源代码",
                    subtitle = "GitHub仓库",
                    icon = Icons.Default.Code,
                    trailing = {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        WebViewActivity.start(
                            context = context,
                            url = "https://github.com/yearsyan/YAAD"
                        )
                    }
                )
            }
        }
    }


    if (showVersionDetail) {
        ModalBottomSheet(
            onDismissRequest = {
                showVersionDetail = false
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.background(MaterialTheme.colorScheme.surface).padding(12.dp)
            ) {
                Text("Builder user: ${BuildInfo.BUILD_MACHINE_USER}")
                Text("Builder actor: ${BuildInfo.ACTOR}")
                Text("Builder host: ${BuildInfo.BUILD_MACHINE_HOST}")
                Text("Builder uname: ${BuildInfo.BUILD_MACHINE_UNAME}")
            }
        }
    }

    if (showLicenseDetail) {
        ModalBottomSheet(
            onDismissRequest = {
                showLicenseDetail = false
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Box (modifier = Modifier.padding(12.dp)){
                Text(BuildInfo.LICENSE_STR)
            }
        }
    }
}
