package io.github.yearsyan.yaad.ui.screens.settings

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kongzue.dialogx.dialogs.PopNotification
import io.github.yearsyan.yaad.MainActivity
import io.github.yearsyan.yaad.ui.components.*
import io.github.yearsyan.yaad.utils.PermissionUtils
import io.github.yearsyan.yaad.utils.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val settings by settingsManager.settings.collectAsState()
    
    // 使用 mutableStateOf 来跟踪权限状态，这样在权限变化时界面会自动更新
    var hasNotificationPermission by remember {
        mutableStateOf(PermissionUtils.hasNotificationPermission(context))
    }
    
    // 监听生命周期变化，当 Activity 恢复时重新检查权限状态
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // 当 Activity 恢复时（比如从权限设置页面返回），重新检查权限状态
                hasNotificationPermission = PermissionUtils.hasNotificationPermission(context)
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部应用栏
        TopAppBar(
            title = { 
                Text(
                    text = "通知设置",
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
                SettingsGroupTitle("通知开关")
            }
            
            item {
                SwitchSettingsItem(
                    title = "启用通知",
                    subtitle = if (hasNotificationPermission) {
                        "下载完成时显示通知"
                    } else {
                        "需要通知权限才能显示通知"
                    },
                    icon = Icons.Default.Notifications,
                    checked = settings.enableNotifications && hasNotificationPermission,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            // 用户想要开启通知，检查权限
                            if (hasNotificationPermission) {
                                // 已有权限，直接保存设置
                                val newSettings = settings.copy(enableNotifications = true)
                                settingsManager.saveSettings(newSettings)
                            } else {
                                // 没有权限，申请权限
                                activity?.requestNotificationPermission()
                                    ?: PopNotification.show("无法申请权限，请手动在系统设置中开启")
                            }
                        } else {
                            // 用户想要关闭通知，直接保存设置
                            val newSettings = settings.copy(enableNotifications = false)
                            settingsManager.saveSettings(newSettings)
                        }
                    }
                )
            }
            
            // 如果没有通知权限，显示权限状态提示
            if (!hasNotificationPermission) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Column {
                                Text(
                                    text = "缺少通知权限",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "应用需要通知权限才能在下载完成时显示通知。请在系统设置中手动开启通知权限。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
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
                            text = "通知设置说明",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• 开启后，下载完成时会显示系统通知\n" +
                                    "• 通知包含下载文件名和状态信息\n" +
                                    "• 可以通过通知快速访问下载的文件\n" +
                                    "• Android 13+ 需要手动授予通知权限\n" +
                                    "• 如果系统禁用了应用通知权限，此设置无效",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
} 