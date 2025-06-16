package io.github.yearsyan.yaad.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.kongzue.dialogx.dialogs.PopTip
import io.github.yearsyan.yaad.R

object ClipboardUtils {
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

    fun writeText(
        context: Context,
        text: String,
        label: String = "yaad_clipboard_text",
        showResultToast: Boolean = true
    ) {
        try {
            val clipboard =
                context.getSystemService(Context.CLIPBOARD_SERVICE)
                    as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            if (showResultToast) {
                PopTip.show(R.string.copy_success).iconSuccess()
            }
        } catch (e: Exception) {
            if (showResultToast) {
                PopTip.show(R.string.copy_fail).iconError()
            }
        }
    }
}
