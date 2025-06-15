package io.github.yearsyan.yaad.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.io.ByteArrayOutputStream

data class WifiInfo(
    val ssid: String,
    val password: String?,
    val encryption: String, // WPA, WEP, nopass
    val hidden: Boolean
)


fun yuvToBitmap(image: Image): Bitmap {
    val yBuffer = image.planes[0].buffer
    val uBuffer = image.planes[1].buffer
    val vBuffer = image.planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    // 注意：NV21 格式是 Y + V + U（不是 Y + U + V）
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage =
        YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 90, out)

    val jpegBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
}

fun decodeQRCode(reader: MultiFormatReader, bitmap: Bitmap): String? {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val source = RGBLuminanceSource(width, height, pixels)
    val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

    return try {
        val result = reader.decode(binaryBitmap)
        result.text
    } catch (e: NotFoundException) {
        null
    }
}

fun String.toWifiOrNull(): WifiInfo? {
    if (!this.startsWith("WIFI:") || !this.endsWith(";")) return null

    // ; for xiaomi
    val content = this.removePrefix("WIFI:").removeSuffix(";;").removeSuffix(";")
    val parts = content.split(";").mapNotNull {
        if (it.contains(":")) {
            val (key, value) = it.split(":", limit = 2)
            key to value
        } else null
    }.toMap()

    val type = parts["T"] ?: return null
    val ssid = parts["S"] ?: return null
    val password = parts["P"]
    val hidden = parts["H"]?.lowercase() == "true"

    return WifiInfo(
        ssid = ssid,
        password = if (type.lowercase() == "nopass") null else password,
        encryption = type,
        hidden = hidden
    )
}