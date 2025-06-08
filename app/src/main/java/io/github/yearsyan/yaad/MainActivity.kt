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
                PopNotification.show("存储权限已授予")
            } else {
                PopNotification.show("存储权限被拒绝，可能影响文件下载功能")
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

                PopNotification.show("通知权限已授予，通知功能已开启")
            } else {
                PopNotification.show("通知权限被拒绝，无法显示下载完成通知")
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
