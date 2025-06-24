package io.github.yearsyan.yaad.utils

import java.util.Locale

/** 格式化工具类 */
object FormatUtils {
    /**
     * 格式化下载速度
     *
     * @param speedKbps 速度（KB/s）
     * @return 格式化后的速度字符串
     */
    fun formatSpeed(speedKbps: Double): String {
        if (speedKbps < 0) return "0 B/s"
        var value = speedKbps * 1024.0
        val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s", "TB/s")
        var i = 0
        while (value >= 1024 && i < units.size - 1) {
            value /= 1024.0
            i++
        }
        return String.format(Locale.getDefault(), "%.2f %s", value, units[i])
    }

    fun formatSize(size: Long): String {
        if (size < 0) return "unknown size"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var i = 0
        var value = size.toDouble()
        while (value >= 1024 && i < units.size - 1) {
            value /= 1024.0
            i++
        }
        return String.format(Locale.getDefault(), "%.2f %s", value, units[i])
    }
}
