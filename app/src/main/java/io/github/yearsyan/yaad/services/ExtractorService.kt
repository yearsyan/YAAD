package io.github.yearsyan.yaad.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Process
import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import io.github.yearsyan.yaad.IExtractorService
import io.github.yearsyan.yaad.model.MediaResult
import io.github.yearsyan.yaad.model.VideoInfo
import kotlinx.serialization.encodeToString
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.json.JSONObject

class ExtractorService : Service() {

    val json = Json { ignoreUnknownKeys = true }

    private val binder =
        object : IExtractorService.Stub() {
            override fun extractDownloadMedia(
                url: String?,
                options: Map<Any?, Any?>
            ): MediaResult {

                val callingPid = Binder.getCallingPid()
                if (callingPid == Process.myPid()) {
                    return MediaResult(code = -1, msg = "callingPid error")
                }

                val py = Python.getInstance()
                val pyModule = py.getModule("you_get_extractor")
                val absPath = File(filesDir, "you-get.zip").absolutePath
                pyModule.callAttr("init_env", absPath)
                val jsonStr =
                    pyModule.callAttr("extract", absPath, url, JSONObject(options).toString().ifEmpty { "{}" }).toString()
                Log.d("ExtractorService", "extract: $jsonStr")
                val videoInfo = json.decodeFromString<VideoInfo>(jsonStr)

                return MediaResult(code = 0, msg = "ok", result = videoInfo)
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
