package io.github.yearsyan.yaad.utils

import android.graphics.Bitmap
import kotlin.math.min

fun scaleBitmapToFit(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height

    if (width <= maxWidth && height <= maxHeight) {
        return bitmap // 无需缩放
    }

    val ratio = min(maxWidth / width.toFloat(), maxHeight / height.toFloat())
    val newWidth = (width * ratio).toInt()
    val newHeight = (height * ratio).toInt()

    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}