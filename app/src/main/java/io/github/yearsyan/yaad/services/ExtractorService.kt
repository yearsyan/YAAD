package io.github.yearsyan.yaad.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Process
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import io.github.yearsyan.yaad.IExtractorService
import io.github.yearsyan.yaad.model.MediaResult

class ExtractorService : Service() {

    private val binder =
        object : IExtractorService.Stub() {
            override fun extractDownloadMedia(
                url: String?,
                options: MutableMap<Any?, Any?>?
            ): MediaResult {

                val callingPid = Binder.getCallingPid()
                if (callingPid == Process.myPid()) {
                    return MediaResult(code = -1, msg = "callingPid error")
                }
                return MediaResult(code = 0, msg = "")
            }
        }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
    }
}
