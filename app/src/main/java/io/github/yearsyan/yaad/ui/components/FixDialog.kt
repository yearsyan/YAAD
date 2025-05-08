package io.github.yearsyan.yaad.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LifecycleCoroutineScope
import com.kongzue.dialogx.dialogs.BottomDialog
import io.github.yearsyan.yaad.utils.YtDlpUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun FixDialog(dialog: BottomDialog, fixYtDlp: Boolean, lifecycleScope: LifecycleCoroutineScope) {
    val context = LocalContext.current
    var fixYtDlpProcess by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        if (fixYtDlp) {
            if (!YtDlpUtil.isYtDlpExists(context)) {
                lifecycleScope.launch (Dispatchers.IO) {
                    val version = YtDlpUtil.getLatestVersion()?: ""
                    YtDlpUtil.downloadVersion(context, version, {
                        fixYtDlpProcess = it
                    })
                    lifecycleScope.launch (Dispatchers.Main) {
                        dialog.dismiss()
                    }
                }
            } else {
                dialog.dismiss()
            }
        }
    }

    Column (
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress = { fixYtDlpProcess },
        )
    }
}