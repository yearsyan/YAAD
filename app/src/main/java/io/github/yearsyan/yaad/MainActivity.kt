package io.github.yearsyan.yaad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.kongzue.dialogx.dialogs.PopNotification
import io.github.yearsyan.yaad.ui.screens.MainScreen
import io.github.yearsyan.yaad.utils.PermissionUtils
import io.github.yearsyan.yaad.utils.SettingsManager

class MainActivity : ComponentActivity() {

    // 存储权限申请器
    private val storagePermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                PopNotification.show(R.string.storage_permission_granted)
            } else {
                PopNotification.show(R.string.storage_permission_denied)
            }
        }

    // 通知权限申请器
    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                // 权限被授予，自动开启通知设置
                val settingsManager = SettingsManager.getInstance(this)
                val currentSettings = settingsManager.settings.value
                val newSettings =
                    currentSettings.copy(enableNotifications = true)
                settingsManager.saveSettings(newSettings)

                PopNotification.show(R.string.notification_permission_granted)
            } else {
                PopNotification.show(R.string.notification_permission_denied)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MainScreen(lifecycleScope = lifecycleScope) }
    }

    /** 申请存储权限 */
    fun requestStoragePermission() {
        val requiredPermissions =
            PermissionUtils.getRequiredStoragePermissions()
        if (requiredPermissions.isNotEmpty()) {
            storagePermissionLauncher.launch(requiredPermissions)
        }
    }

    /** 申请通知权限 */
    fun requestNotificationPermission() {
        if (
            android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.TIRAMISU
        ) {
            notificationPermissionLauncher.launch(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS)
            )
        }
    }
}
