package io.github.yearsyan.yaad.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kongzue.dialogx.dialogs.PopTip
import io.github.yearsyan.yaad.R
import io.github.yearsyan.yaad.utils.YtDlpUtil

suspend fun handleYtDlpResult(context: Context) {
    val version = YtDlpUtil.getLatestVersion() ?: "2025.04.30"
    if (!YtDlpUtil.downloadVersion(context, version)) {
        PopTip.show(
            com.kongzue.dialogx.R.mipmap.ico_dialogx_error,
            context.getString(R.string.net_error)
        )
        return
    }
    if (!YtDlpUtil.isYtDlpExists(context)) {
        PopTip.show(
            com.kongzue.dialogx.R.mipmap.ico_dialogx_error,
            context.getString(R.string.download_fail)
        )
        return
    }
}

@Composable
fun EnvDoctorResult(ytDlpExist: Boolean, youGetExist: Boolean) {
    Column(
        modifier =
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 0.dp)
    ) {
        Text("yt-dlp：$ytDlpExist")
        Text("you-get: $youGetExist")
    }
}
