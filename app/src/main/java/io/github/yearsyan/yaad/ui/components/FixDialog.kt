package io.github.yearsyan.yaad.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleCoroutineScope
import com.kongzue.dialogx.dialogs.BottomDialog
import io.github.yearsyan.yaad.utils.YouGetUtil
import io.github.yearsyan.yaad.utils.YtDlpUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun FixDialog(dialog: BottomDialog, fixYtDlp: Boolean, fixYouGet: Boolean, lifecycleScope: LifecycleCoroutineScope) {
    val context = LocalContext.current
    var fixYtDlpProcess by remember { mutableFloatStateOf(0f) }
    var fixYouGetProcess by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        if (fixYtDlp) {
            if (!YtDlpUtil.isYtDlpExists(context)) {
                lifecycleScope.launch (Dispatchers.IO) {
                    val version = YtDlpUtil.getLatestVersion()?: ""
                    YtDlpUtil.downloadVersion(context, version, {
                        fixYtDlpProcess = it
                    })
                    lifecycleScope.launch (Dispatchers.Main) {
                        fixYtDlpProcess = 1.0f
                        if (fixYouGetProcess == 1.0f || !fixYouGet) {
                            dialog.dismiss()
                        }
                    }
                }
            } else {
                dialog.dismiss()
            }
        }
        if (fixYouGet) {
            if (!YouGetUtil.isYouGetExists(context)) {
                lifecycleScope.launch (Dispatchers.IO) {
                    val version = YouGetUtil.getLatestVersion()?: ""
                    YouGetUtil.downloadVersion(context, version, {
                        fixYouGetProcess = it
                    })
                    lifecycleScope.launch (Dispatchers.Main) {
                        fixYouGetProcess = 1.0f
                        if (fixYtDlpProcess == 1.0f || !fixYtDlp) {
                            dialog.dismiss()
                        }
                    }
                }
            }
        }
    }

    Column (
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(20.dp)
    ) {
        LinearProgressIndicator(
            progress = { fixYtDlpProcess },
        )
        LinearProgressIndicator(
            progress = { fixYouGetProcess },
        )
    }
}