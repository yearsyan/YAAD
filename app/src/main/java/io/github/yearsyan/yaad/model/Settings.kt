package io.github.yearsyan.yaad.model

import kotlinx.serialization.Serializable

/** FFmpeg安装类型 */
enum class FFmpegInstallType {
    BUILTIN, // 内置
    DOWNLOAD, // 下载
    CUSTOM_URL // 自定义URL
}

/** 应用设置数据类 */
@Serializable
data class AppSettings(
    val ffmpegInstallType: FFmpegInstallType = FFmpegInstallType.BUILTIN,
    val ffmpegCustomUrl: String = "",
    val cookieFilePath: String = "",
    val defaultDownloadThreads: Int = 8,
    val btDownloadSpeedLimit: Long = 0L, // 0表示无限制，单位：KB/s
    val btUploadSpeedLimit: Long = 0L, // 0表示无限制，单位：KB/s
    val downloadPath: String = "",
    val enableNotifications: Boolean = true,
    val autoRetryCount: Int = 3
)

/** Cookie文件信息 */
@Serializable data class CookieFileInfo(val name: String, val path: String)
