package io.github.yearsyan.yaad.downloader

import android.app.Application
import android.content.Context
import android.os.Looper
import io.github.yaad.downloader_core.HttpDownloadSession
import io.github.yaad.downloader_core.IDownloadListener
import io.github.yaad.downloader_core.IDownloadSession
import io.github.yearsyan.yaad.utils.sha512
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object DownloadManager: IDownloadListener {

    enum class DownloadType {
        SINGLE_HTTP,
        BT,
        EXTRACTED_MEDIA,
        CHILD_HTTP
    }

    open class DownloadSession(
        val sessionId: String,
        val downloadType: DownloadType,
        val originLink: String,
        val recoverFile: String
    )

    class SingleHttpDownloadSession(
        sessionId: String,
        originLink: String,
        recoverFile: String,
        val httpDownloadSession: HttpDownloadSession,
        val job: Job
    ) :
        DownloadSession(
            sessionId,
            DownloadType.SINGLE_HTTP,
            originLink,
            recoverFile
        )

    private lateinit var context: Application
    private val downloaderDispatcher = ThreadPoolExecutor(
        4,
        32,
        30L,
        TimeUnit.SECONDS,
        LinkedBlockingQueue(),
    ).asCoroutineDispatcher()
    private val downloadCoScope = CoroutineScope(downloaderDispatcher + SupervisorJob())
    private val downloadTasks = mutableListOf<DownloadSession>()

    fun addHttpDownloadTask(
        url: String,
        headers: Map<String, String>,
        useCookie: Boolean = false
    ): DownloadSession {
        val sessionId = createSessionId()
        val fileDist = File(context.filesDir, url.sha512())
        val httpSession = HttpDownloadSession(
            url = url,
            path = fileDist.absolutePath,
            headers = headers
        )

        httpSession.addDownloadListener(this)
        val job = downloadCoScope.launch {
            httpSession.start()
        }
        val downloadSession = SingleHttpDownloadSession(sessionId, url, httpSession.recoverFilePath(), httpSession, job)
        synchronized(downloadTasks) {
            downloadTasks.add(downloadSession)
        }
        return downloadSession
    }

    private fun createSessionId(): String {
        return UUID.randomUUID().toString()
    }

    fun initByApplication(app: Application) {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw RuntimeException("Must be called from the main thread")
        }
        context = app
    }

    override fun onComplete(session: IDownloadSession) {
        super.onComplete(session)
    }

    override fun onPause(session: IDownloadSession) {
        super.onPause(session)
    }

    override fun onProgress(session: IDownloadSession) {
        super.onProgress(session)
    }

    override fun onError(session: IDownloadSession, reason: Exception) {
        super.onError(session, reason)
    }

    override fun onResume(session: IDownloadSession, savePath: String) {
        super.onResume(session, savePath)
    }

}
