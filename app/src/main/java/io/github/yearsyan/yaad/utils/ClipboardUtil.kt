package io.github.yearsyan.yaad.utils

import android.content.ClipboardManager
import android.content.Context

object ClipboardUtil {
    fun readText(context: Context): String {
        val clipboard =
            context.getSystemService(Context.CLIPBOARD_SERVICE)
                as ClipboardManager
        if (clipboard.hasPrimaryClip()) {
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val item = clipData.getItemAt(0)
                val text = item.text
                if (text != null) {
                    return text.toString()
                }
            }
        }
        return ""
    }
}
