package io.github.yearsyan.yaad

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import com.kongzue.dialogx.DialogX
import com.kongzue.dialogx.style.MaterialStyle
import io.github.yearsyan.yaad.downloader.DownloadManager
import io.github.yearsyan.yaad.services.ExtractorClient
import io.github.yearsyan.yaad.utils.AssetUtil
import io.github.yearsyan.yaad.utils.YtDlpUtil.YT_DLP_FILE_NAME
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AppApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (isMainProcess()) {
            initForMainProcess()
        }
    }

    private fun initForMainProcess() {
        DialogX.init(this)
        DialogX.globalStyle = MaterialStyle()
        DialogX.globalTheme = DialogX.THEME.AUTO
        initZip()
        ExtractorClient.initialize(this)
        ExtractorClient.getInstance().connect()
        DownloadManager.initByApplication(this)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun initZip() {
        GlobalScope.launch(Dispatchers.IO) {
            AssetUtil.copyAssetIfNeeded(this@AppApplication, YT_DLP_FILE_NAME)
        }
    }

    private fun isMainProcess(): Boolean {
        val pid = android.os.Process.myPid()
        val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        am?.runningAppProcesses?.forEach { processInfo ->
            if (processInfo.pid == pid) {
                return packageName == processInfo.processName
            }
        }
        return false
    }
}
