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
fun CookieSettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val settings by settingsManager.settings.collectAsState()

    var showCookieSelector by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部应用栏
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.cookie_settings),
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
                SettingsGroupTitle(stringResource(R.string.cookie_settings))
            }

            item {
                SettingsItem(
                    title = stringResource(R.string.current_cookie_file),
                    subtitle =
                        if (settings.cookieFilePath.isNotEmpty()) {
                            settings.cookieFilePath.substringAfterLast("/")
                        } else {
                            stringResource(R.string.no_cookie_file)
                        },
                    icon = Icons.Default.Cookie,
                    trailing = {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null
                        )
                    },
                    onClick = { showCookieSelector = true }
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
                                    R.string.cookie_file_description
                                ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text =
                                stringResource(
                                    R.string.cookie_file_description_content
                                ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                SettingsGroupTitle(stringResource(R.string.how_to_get_cookie))
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
                            text = stringResource(R.string.how_to_get_cookie),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text =
                                stringResource(
                                    R.string.how_to_get_cookie_content
                                ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Cookie选择器
    if (showCookieSelector) {
        SimpleCookieSelector(
            onDismiss = { showCookieSelector = false },
            settingsManager = settingsManager,
            currentCookieFile = settings.cookieFilePath
        )
    }
}
