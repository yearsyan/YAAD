package io.github.yearsyan.yaad.ui.screens.settings

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.yearsyan.yaad.BuildConfig
import io.github.yearsyan.yaad.BuildInfo
import io.github.yearsyan.yaad.R
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
            title = {
                Text(
                    text = stringResource(R.string.about),
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
            item { SettingsGroupTitle(stringResource(R.string.app_info)) }

            item {
                SettingsItem(
                    title = stringResource(R.string.version_info),
                    subtitle =
                        "YAAD ${BuildConfig.VERSION_NAME} #${BuildInfo.GIT_HASH}",
                    icon = Icons.Default.Info
                )
            }

            item {
                SettingsItem(
                    title = stringResource(R.string.check_update),
                    subtitle = stringResource(R.string.check_update_desc),
                    icon = Icons.Default.Update,
                    onClick = { /* TODO: Implement check update logic */}
                )
            }

            item {
                SettingsItem(
                    title = stringResource(R.string.build_date),
                    subtitle = BuildInfo.BUILD_TIME,
                    icon = Icons.Default.CalendarToday,
                    onClick = { showVersionDetail = true }
                )
            }

            item {
                SettingsGroupTitle(stringResource(R.string.open_source_info))
            }

            item {
                SettingsItem(
                    title = stringResource(R.string.open_source_license),
                    subtitle =
                        stringResource(R.string.view_open_source_license),
                    icon = Icons.Default.Description,
                    onClick = { showLicenseDetail = true }
                )
            }

            item {
                SettingsItem(
                    title = stringResource(R.string.source_code),
                    subtitle = stringResource(R.string.github_repo),
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
            onDismissRequest = { showVersionDetail = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            InfoTable(
                items =
                    listOf(
                        "Builder user" to BuildInfo.BUILD_MACHINE_USER,
                        "Builder actor" to BuildInfo.ACTOR,
                        "Builder uname" to BuildInfo.BUILD_MACHINE_UNAME
                    )
            )
        }
    }

    if (showLicenseDetail) {
        ModalBottomSheet(
            onDismissRequest = { showLicenseDetail = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            LicenseModalList()
        }
    }
}
