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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.yearsyan.yaad.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSettingsScreen(
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部应用栏
        TopAppBar(
            title = { 
                Text(
                    text = "关于",
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
                SettingsGroupTitle("应用信息")
            }
            
            item {
                SettingsItem(
                    title = "版本信息",
                    subtitle = "YAAD v1.0.0",
                    icon = Icons.Default.Info
                )
            }
            
            item {
                SettingsItem(
                    title = "构建日期",
                    subtitle = "2024年12月",
                    icon = Icons.Default.CalendarToday
                )
            }
            
            item {
                SettingsGroupTitle("开源信息")
            }
            
            item {
                SettingsItem(
                    title = "开源许可",
                    subtitle = "查看开源许可证",
                    icon = Icons.Default.Description,
                    trailing = {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null
                        )
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
                    }
                )
            }
            
            item {
                SettingsGroupTitle("技术栈")
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
                            text = "主要技术",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Kotlin & Jetpack Compose\n" +
                                    "• Material Design 3\n" +
                                    "• FFmpeg for Android\n" +
                                    "• MMKV for data storage\n" +
                                    "• Ktor for networking\n" +
                                    "• Python integration (Chaquopy)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            item {
                SettingsGroupTitle("感谢")
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
                            text = "特别感谢",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• FFmpeg项目\n" +
                                    "• you-get项目\n" +
                                    "• Android开源社区\n" +
                                    "• 所有贡献者和用户",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
} 