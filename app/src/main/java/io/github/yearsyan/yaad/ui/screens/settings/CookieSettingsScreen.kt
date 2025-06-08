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
fun CookieSettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val settings by settingsManager.settings.collectAsState()

    var showCookieSelector by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部应用栏
        TopAppBar(
            title = { Text(text = "Cookie设置", fontWeight = FontWeight.Bold) },
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
            item { SettingsGroupTitle("Cookie文件") }

            item {
                SettingsItem(
                    title = "当前Cookie文件",
                    subtitle =
                        if (settings.cookieFilePath.isNotEmpty()) {
                            settings.cookieFilePath.substringAfterLast("/")
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
                    onClick = { showCookieSelector = true }
                )
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
                            text = "Cookie文件说明",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text =
                                "• Cookie文件用于访问需要登录的网站\n" +
                                    "• 支持从浏览器导出的cookies.txt格式\n" +
                                    "• 文件会被复制到应用内部存储中\n" +
                                    "• 可以选择不使用Cookie文件",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { SettingsGroupTitle("获取Cookie文件") }

            item {
                Card(
                    modifier =
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    elevation =
                        CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            text = "如何获取Cookie文件",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text =
                                "1. 使用浏览器扩展（如Get cookies.txt）\n" +
                                    "2. 从浏览器开发者工具导出\n" +
                                    "3. 使用专门的Cookie导出工具\n" +
                                    "4. 确保文件格式为Netscape格式",
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
