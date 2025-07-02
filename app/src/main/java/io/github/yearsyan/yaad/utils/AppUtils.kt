package io.github.yearsyan.yaad.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import java.net.Inet6Address
import java.net.NetworkInterface

data class ComponentInfo(val name: String, val icon: Drawable)

fun ComponentName.getComponentInfo(context: Context): ComponentInfo? {
    try {
        val pm = context.packageManager
        val activityInfo: ActivityInfo = pm.getActivityInfo(this, 0)
        val icon = activityInfo.loadIcon(pm)
        val label = activityInfo.loadLabel(pm)
        val appName = label.toString()
        return ComponentInfo(name = appName, icon = icon)
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }
    return null
}

fun isGlobalIPv6Available(): Boolean {
    val interfaces = NetworkInterface.getNetworkInterfaces()

    for (iface in interfaces) {
        if (!iface.isUp || iface.isLoopback) continue

        val addresses = iface.inetAddresses
        for (addr in addresses) {
            if (addr is Inet6Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                // 找到一个全局 IPv6 地址
                return true
            }
        }
    }
    return false
}