package io.github.yearsyan.yaad

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import com.kongzue.dialogx.DialogX
import com.kongzue.dialogx.dialogs.PopNotification
import com.kongzue.dialogx.style.MaterialStyle
import com.tencent.mmkv.MMKV
import io.github.yearsyan.yaad.downloader.DownloadManager
import io.github.yearsyan.yaad.services.ExtractorClient
import io.github.yearsyan.yaad.utils.RepoRelease
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (isMainProcess()) {
            System.loadLibrary("media")
            initForMainProcess()
        }
    }

    private fun initForMainProcess() {
        // 初始化MMKV
        MMKV.initialize(this)

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
            try {
                val youGetRepo =
                    object : RepoRelease() {
                        override val repo = "yearsyan/you-get-zipimport"
                        override val assetFileName: String
                            get() = "you-get.zip"
                    }
                if (!youGetRepo.exist(this@AppApplication)) {
                    val version = youGetRepo.getLatestVersion() ?: return@launch
                    val downloadTask =
                        youGetRepo.downloadVersion(
                            scope = GlobalScope,
                            context = this@AppApplication,
                            version = version
                        )
                    downloadTask.join()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    PopNotification.show(e.message)
                }
            }
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
