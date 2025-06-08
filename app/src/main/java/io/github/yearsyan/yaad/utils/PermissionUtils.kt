package io.github.yearsyan.yaad.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionUtils {

    /** 获取当前Android版本需要的存储权限 */
    fun getRequiredStoragePermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ 需要READ_MEDIA_VIDEO权限
                arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10-12 不需要特殊权限，使用MediaStore API
                emptyArray()
            }
            else -> {
                // Android 9及以下需要WRITE_EXTERNAL_STORAGE权限
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
        }
    }

    /** 检查是否有存储权限 */
    fun hasStoragePermission(context: Context): Boolean {
        val requiredPermissions = getRequiredStoragePermissions()

        if (requiredPermissions.isEmpty()) {
            // Android 10-12 不需要特殊权限
            return true
        }

        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    /** 检查是否有通知权限 */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 需要POST_NOTIFICATIONS权限
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 12及以下默认有通知权限
            true
        }
    }
}
