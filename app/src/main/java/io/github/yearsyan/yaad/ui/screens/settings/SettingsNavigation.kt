package io.github.yearsyan.yaad.ui.screens.settings

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 设置页面路由
 */
@Parcelize
sealed class SettingsRoute(val route: String, val title: String) : Parcelable {
    object Main : SettingsRoute("settings_main", "设置")
    object FFmpeg : SettingsRoute("settings_ffmpeg", "FFmpeg设置")
    object Cookie : SettingsRoute("settings_cookie", "Cookie设置")
    object Download : SettingsRoute("settings_download", "下载设置")
    object Notification : SettingsRoute("settings_notification", "通知设置")
    object About : SettingsRoute("settings_about", "关于")
} 