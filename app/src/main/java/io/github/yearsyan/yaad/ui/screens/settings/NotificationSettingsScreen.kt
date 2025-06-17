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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kongzue.dialogx.dialogs.PopNotification
import io.github.yearsyan.yaad.MainActivity
import io.github.yearsyan.yaad.R
import io.github.yearsyan.yaad.ui.components.*
import io.github.yearsyan.yaad.utils.PermissionUtils
import io.github.yearsyan.yaad.utils.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
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
                hasNotificationPermission =
                    PermissionUtils.hasNotificationPermission(context)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部应用栏
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.notification_settings),
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
                SettingsGroupTitle(stringResource(R.string.notification_switch))
            }

            item {
                SwitchSettingsItem(
                    title = stringResource(R.string.enable_notification),
                    subtitle =
                        if (hasNotificationPermission) {
                            stringResource(R.string.notification_enabled_desc)
                        } else {
                            stringResource(
                                R.string.notification_permission_required
                            )
                        },
                    icon = Icons.Default.Notifications,
                    checked =
                        settings.enableNotifications &&
                            hasNotificationPermission,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            // 用户想要开启通知，检查权限
                            if (hasNotificationPermission) {
                                // 已有权限，直接保存设置
                                val newSettings =
                                    settings.copy(enableNotifications = true)
                                settingsManager.saveSettings(newSettings)
                            } else {
                                // 没有权限，申请权限
                                activity?.requestNotificationPermission()
                                    ?: PopNotification.show(
                                        R.string.notification_permission_error
                                    )
                            }
                        } else {
                            // 用户想要关闭通知，直接保存设置
                            val newSettings =
                                settings.copy(enableNotifications = false)
                            settingsManager.saveSettings(newSettings)
                        }
                    }
                )
            }

            // 如果没有通知权限，显示权限状态提示
            if (!hasNotificationPermission) {
                item {
                    Card(
                        modifier =
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme.errorContainer
                            ),
                        elevation =
                            CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint =
                                    MaterialTheme.colorScheme.onErrorContainer
                            )
                            Column {
                                Text(
                                    text =
                                        stringResource(
                                            R.string
                                                .notification_permission_missing
                                        ),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color =
                                        MaterialTheme.colorScheme
                                            .onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text =
                                        stringResource(
                                            R.string
                                                .notification_permission_missing_desc
                                        ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color =
                                        MaterialTheme.colorScheme
                                            .onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            item {
                SettingsGroupTitle(
                    stringResource(R.string.notification_instructions)
                )
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
                                    R.string.notification_instructions_title
                                ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text =
                                stringResource(
                                    R.string.notification_instructions_content
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
