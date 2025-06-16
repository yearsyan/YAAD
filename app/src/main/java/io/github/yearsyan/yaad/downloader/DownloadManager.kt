package io.github.yearsyan.yaad.downloader

import android.app.Application
import android.os.Environment
import android.os.Looper
import io.github.yaad.downloader_core.DownloadState
import io.github.yaad.downloader_core.HttpDownloadSession
import io.github.yaad.downloader_core.IDownloadListener
import io.github.yaad.downloader_core.IDownloadSession
import io.github.yaad.downloader_core.getAppContext
import io.github.yearsyan.yaad.db.DownloadDatabaseHelper
import io.github.yearsyan.yaad.media.FFmpegTools
import io.github.yearsyan.yaad.utils.sha512
import java.io.File
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object DownloadManager : IDownloadListener {
    enum class DownloadType {
        SINGLE_HTTP,
        BT,
        EXTRACTED_MEDIA,
        CHILD_HTTP
    }

    open class DownloadSessionRecord(
        val title: String,
        val sessionId: String,
        val downloadType: DownloadType,
        val originLink: String,
        val recoverFile: String,
        val savePath: String = "",
        val downloadState: DownloadState
    ) {
        open fun isActiveForService(): Boolean = false
    }

    class SingleHttpDownloadSessionRecord(
        title: String,
        sessionId: String,
        originLink: String,
        recoverFile: String,
        savePath: String = "",
        downloadState: DownloadState = DownloadState.PENDING
    ) :
        DownloadSessionRecord(
            title,
            sessionId,
            DownloadType.SINGLE_HTTP,
            originLink,
            recoverFile,
            savePath,
            downloadState
        ) {
        var httpDownloadSession: HttpDownloadSession? = null

        override fun isActiveForService(): Boolean {
            val session = httpDownloadSession ?: return false
            val state = session.getStatus().state
            return (state == DownloadState.DOWNLOADING ||
                state == DownloadState.VALIDATING ||
                state == DownloadState.PENDING)
        }
    }

    class ChildHttpDownloadSessionRecord(
        title: String,
        sessionId: String,
        originLink: String,
        recoverFile: String,
        savePath: String = "",
        val parentSessionId: String,
        downloadState: DownloadState = DownloadState.PENDING
    ) :
        DownloadSessionRecord(
            title,
            sessionId,
            DownloadType.CHILD_HTTP,
            originLink,
            recoverFile,
            savePath,
            downloadState
        ) {
        var httpDownloadSession: HttpDownloadSession? = null

        override fun isActiveForService(): Boolean {
            val session = httpDownloadSession ?: return false
            val state = session.getStatus().state
            return (state == DownloadState.DOWNLOADING ||
                state == DownloadState.VALIDATING ||
                state == DownloadState.PENDING)
        }
    }

    class ExtractedMediaDownloadSessionRecord(
        title: String,
        sessionId: String,
        originLink: String,
        recoverFile: String,
        val mediaUrls: List<String>,
        savePath: String = "",
        downloadState: DownloadState = DownloadState.PENDING
    ) :
        DownloadSessionRecord(
            title,
            sessionId,
            DownloadType.EXTRACTED_MEDIA,
            originLink,
            recoverFile,
            savePath,
            downloadState
        ) {
        val childSessions = mutableListOf<ChildHttpDownloadSessionRecord>()
        var completedCount = 0

        override fun isActiveForService(): Boolean {
            return childSessions.any { it.isActiveForService() }
        }
    }

    private lateinit var context: Application
    private lateinit var dbHelper: DownloadDatabaseHelper
    private val dispatcher =
        ThreadPoolExecutor(4, 32, 30L, TimeUnit.SECONDS, LinkedBlockingQueue())
            .asCoroutineDispatcher()
    private val downloadScope = CoroutineScope(dispatcher + SupervisorJob())

    private val _tasksFlow =
        MutableStateFlow<List<DownloadSessionRecord>>(emptyList())
    val tasksFlow: StateFlow<List<DownloadSessionRecord>> = _tasksFlow

    private val downloadListeners = mutableSetOf<IDownloadTaskListener>()
    private val downloadTasks = mutableListOf<DownloadSessionRecord>()
    private val sessionMap =
        ConcurrentHashMap<
            IDownloadSession, WeakReference<DownloadSessionRecord>
        >()

    fun initByApplication(app: Application) {
        if (Looper.getMainLooper() != Looper.myLooper())
            throw RuntimeException("Must be called from main thread")
        context = app
        dbHelper = DownloadDatabaseHelper(app)
        loadSavedSessions()
    }

    private fun loadSavedSessions() {
        val savedSessions = dbHelper.getAllDownloadSessions()
        synchronized(downloadTasks) {
            downloadTasks.clear()
            savedSessions.forEach { record ->
                when (record) {
                    is SingleHttpDownloadSessionRecord -> {
                        // 重新创建 HttpDownloadSession
                        val httpSession =
                            HttpDownloadSession(
                                url = record.originLink,
                                path = record.savePath
                            )
                        httpSession.addDownloadListener(this)

                        // 检查目标文件是否已存在
                        val targetFile = File(record.savePath)
                        if (targetFile.exists() && targetFile.length() > 0) {
                            // 如果文件已存在，直接设置为完成状态
                            httpSession.setState(DownloadState.COMPLETED)
                            dbHelper.updateDownloadState(
                                record.sessionId,
                                DownloadState.COMPLETED
                            )
                        } else if (File(record.recoverFile).exists()) {
                            downloadScope.launch(Dispatchers.IO) {
                                httpSession.start()
                            }
                        }

                        record.httpDownloadSession = httpSession
                        sessionMap[httpSession] = WeakReference(record)
                    }
                    is ExtractedMediaDownloadSessionRecord -> {
                        // 对于每个子会话，重新创建 HttpDownloadSession
                        record.childSessions.forEach { childRecord ->
                            val httpSession =
                                HttpDownloadSession(
                                    url = childRecord.originLink,
                                    path = childRecord.savePath
                                )
                            httpSession.addDownloadListener(this)

                            // 检查目标文件是否已存在
                            val targetFile = File(childRecord.savePath)
                            if (
                                childRecord.downloadState !=
                                    DownloadState.COMPLETED &&
                                    targetFile.exists() &&
                                    targetFile.length() > 0
                            ) {
                                // 如果文件已存在，直接设置为完成状态
                                httpSession.setState(DownloadState.COMPLETED)
                                dbHelper.updateDownloadState(
                                    childRecord.sessionId,
                                    DownloadState.COMPLETED
                                )
                            } else if (File(childRecord.recoverFile).exists()) {
                                downloadScope.launch(Dispatchers.IO) {
                                    httpSession.start()
                                }
                            }

                            childRecord.httpDownloadSession = httpSession
                            sessionMap[httpSession] = WeakReference(childRecord)
                        }

                        // 更新完成计数
                        record.completedCount =
                            record.childSessions.count { childRecord ->
                                childRecord.httpDownloadSession
                                    ?.getStatus()
                                    ?.state == DownloadState.COMPLETED
                            }
                    }
                }
                downloadTasks.add(record)
            }
            _tasksFlow.value =
                downloadTasks.toList().filter {
                    it !is ChildHttpDownloadSessionRecord
                }
        }
    }

    fun registerDownloadTaskListener(listener: IDownloadTaskListener) {
        synchronized(downloadListeners) { downloadListeners.add(listener) }
    }

    fun unregisterDownloadTaskListener(listener: IDownloadTaskListener) {
        synchronized(downloadListeners) { downloadListeners.remove(listener) }
    }

    fun addHttpDownloadTask(
        url: String,
        headers: Map<String, String> = emptyMap(),
        startResultListener: (e: Exception?) -> Unit = {}
    ): DownloadSessionRecord {
        val sessionId = UUID.randomUUID().toString()
        val fileName =
            try {
                java.net.URL(url).path.substringAfterLast('/').takeIf {
                    it.isNotEmpty()
                } ?: "download_${System.currentTimeMillis()}"
            } catch (e: Exception) {
                "download_${System.currentTimeMillis()}"
            }
        val fileDist = File(context.filesDir, fileName)
        val httpSession =
            HttpDownloadSession(
                url = url,
                path = fileDist.absolutePath,
                headers = headers
            )

        httpSession.addDownloadListener(this)
        downloadScope.launch(Dispatchers.IO) {
            httpSession.start({
                startResultListener(it)
                checkAndControlService()
            })
        }

        val record =
            SingleHttpDownloadSessionRecord(
                sessionId,
                url,
                httpSession.recoverFilePath(),
                fileDist.absolutePath
            )
        record.httpDownloadSession = httpSession

        sessionMap[httpSession] = WeakReference(record)

        synchronized(downloadTasks) {
            downloadTasks.add(record)
            _tasksFlow.value = downloadTasks.toList()
        }

        dbHelper.saveDownloadSession(record)

        synchronized(downloadListeners) {
            downloadListeners.forEach { it.onCreateTask(httpSession, record) }
        }
        return record
    }

    fun addExtractedMediaDownloadTask(
        title: String,
        originUrl: String,
        mediaUrls: List<String>,
        headers: Map<String, String> = emptyMap(),
        onAllComplete:
            suspend (session: DownloadSessionRecord, saves: String) -> Unit =
            { i, v ->
            }
    ): DownloadSessionRecord {
        val sessionId = UUID.randomUUID().toString()
        val recoverDir = File(context.filesDir, sessionId)
        if (!recoverDir.exists()) {
            recoverDir.mkdirs()
        }

        val record =
            ExtractedMediaDownloadSessionRecord(
                title = title,
                sessionId = sessionId,
                originLink = originUrl,
                recoverFile = recoverDir.absolutePath,
                mediaUrls = mediaUrls,
                savePath =
                    "${Environment. getExternalStorageDirectory().path}/Download/${title}.mp4"
            )

        synchronized(downloadTasks) {
            downloadTasks.add(record)
            _tasksFlow.value = downloadTasks.toList()
        }

        dbHelper.saveDownloadSession(record)

        // 开始所有子任务的下载
        mediaUrls.forEachIndexed { mediaIndex, url ->
            val childSessionId = UUID.randomUUID().toString()
            val fileName =
                try {
                    java.net.URL(url).path.substringAfterLast('/').takeIf {
                        it.isNotEmpty()
                    } ?: "download_${System.currentTimeMillis()}"
                } catch (e: Exception) {
                    "download_${url.sha512()}"
                }
            val fileDist = File(recoverDir, fileName)
            val httpSession =
                HttpDownloadSession(
                    url = url,
                    path = fileDist.absolutePath,
                    headers = headers
                )

            httpSession.addDownloadListener(this)
            downloadScope.launch(Dispatchers.IO) {
                httpSession.start(
                    {},
                    {
                        synchronized(record) {
                            record.completedCount++
                            if (record.completedCount == mediaUrls.size) {
                                downloadScope.launch(Dispatchers.Default) {
                                    val medias =
                                        record.childSessions.map { it.savePath }
                                    val media1 = File(medias[0])
                                    val media2 = File(medias[1])
                                    val mergeAt =
                                        File(
                                                getAppContext()?.filesDir,
                                                "${title}.mp4"
                                            )
                                            .absolutePath
                                    FFmpegTools.mergeAV(
                                        medias[0],
                                        medias[1],
                                        mergeAt
                                    )
                                    withContext(Dispatchers.IO) {
                                        media1.delete()
                                        media2.delete()
                                    }
                                    dbHelper.updateDownloadState(
                                        record.sessionId,
                                        DownloadState.COMPLETED
                                    )
                                    onAllComplete(record, mergeAt)
                                    checkAndControlService()
                                }
                            }
                        }
                    }
                )
            }

            val childRecord =
                ChildHttpDownloadSessionRecord(
                    title = "$title-$mediaIndex",
                    sessionId = childSessionId,
                    originLink = url,
                    recoverFile = httpSession.recoverFilePath(),
                    savePath = fileDist.absolutePath,
                    parentSessionId = sessionId
                )
            childRecord.httpDownloadSession = httpSession

            sessionMap[httpSession] = WeakReference(childRecord)
            record.childSessions.add(childRecord)

            synchronized(downloadTasks) {
                downloadTasks.add(childRecord)
                _tasksFlow.value = downloadTasks.toList()
            }

            dbHelper.saveDownloadSession(childRecord)

            synchronized(downloadListeners) {
                downloadListeners.forEach {
                    it.onCreateTask(httpSession, childRecord)
                }
            }
        }

        return record
    }

    suspend fun deleteDownloadTask(sessionId: String) {
        val record =
            synchronized(downloadTasks) {
                downloadTasks.find { it.sessionId == sessionId } ?: return
            }

        when (record) {
            is SingleHttpDownloadSessionRecord -> {
                val session = record.httpDownloadSession
                synchronized(downloadTasks) {
                    session?.let { sessionMap.remove(it) }
                    downloadTasks.remove(record)
                    _tasksFlow.value = downloadTasks.toList()
                }
                session?.stop()
                dbHelper.deleteDownloadSession(sessionId)
            }
            is ChildHttpDownloadSessionRecord -> {
                val session = record.httpDownloadSession
                synchronized(downloadTasks) {
                    session?.let { sessionMap.remove(it) }
                    downloadTasks.remove(record)
                    _tasksFlow.value = downloadTasks.toList()
                }
                session?.stop()
                dbHelper.deleteDownloadSession(sessionId)
            }
            is ExtractedMediaDownloadSessionRecord -> {
                val sessionsToStop = mutableListOf<HttpDownloadSession>()
                val childSessionIds = mutableListOf<String>()
                synchronized(downloadTasks) {
                    record.childSessions.forEach { childRecord ->
                        childRecord.httpDownloadSession?.let {
                            sessionMap.remove(it)
                            sessionsToStop.add(it)
                        }
                        downloadTasks.remove(childRecord)
                        childSessionIds.add(childRecord.sessionId)
                    }
                    downloadTasks.remove(record)
                    _tasksFlow.value = downloadTasks.toList()
                }
                sessionsToStop.forEach { it.stop() }

                // 删除主记录
                dbHelper.deleteDownloadSession(sessionId)
                // 删除所有子记录
                childSessionIds.forEach { childSessionId ->
                    dbHelper.deleteDownloadSession(childSessionId)
                }
            }
        }
    }

    private fun notifyListeners(
        session: IDownloadSession,
        callback:
            IDownloadTaskListener.(
                IDownloadSession, DownloadSessionRecord
            ) -> Unit
    ) {
        val record = sessionMap[session]?.get() ?: return
        synchronized(downloadListeners) {
            downloadListeners.forEach { listener ->
                callback(listener, session, record)
            }
        }
    }

    private fun updateFlow() {
        synchronized(downloadTasks) {
            _tasksFlow.value = downloadTasks.toList().filter {
                it is SingleHttpDownloadSessionRecord || it is ExtractedMediaDownloadSessionRecord
            }.reversed()
        }
    }

    private fun hasActiveTasks(): Boolean {
        synchronized(downloadTasks) {
            return downloadTasks.any { record ->
                when (record) {
                    is SingleHttpDownloadSessionRecord -> {
                        val session = record.httpDownloadSession
                        val state = session?.getStatus()?.state
                        session != null &&
                            state != null &&
                            (state == DownloadState.DOWNLOADING ||
                                state == DownloadState.PENDING)
                    }
                    is ChildHttpDownloadSessionRecord -> {
                        val session = record.httpDownloadSession
                        val state = session?.getStatus()?.state
                        session != null &&
                            state != null &&
                            (state == DownloadState.DOWNLOADING ||
                                state == DownloadState.PENDING)
                    }
                    else -> false
                }
            }
        }
    }

    private fun checkAndControlService() {
        //        downloadScope.launch {
        //            serviceControlMutex.withLock {
        //                val shouldServiceRun = hasActiveTasks()
        //                if (shouldServiceRun && !isServiceRunning) {
        //                    val serviceIntent =
        //                        Intent(context, DownloadForegroundService::class.java)
        //                            .apply {
        //                                action =
        //                                    DownloadServiceConstants
        //                                        .ACTION_START_FOREGROUND_SERVICE
        //                            }
        //                    ContextCompat.startForegroundService(context, serviceIntent)
        //                    isServiceRunning = true
        //                } else if (!shouldServiceRun && isServiceRunning) {
        //                    val serviceIntent =
        //                        Intent(context, DownloadForegroundService::class.java)
        //                            .apply {
        //                                action =
        //                                    DownloadServiceConstants
        //                                        .ACTION_STOP_FOREGROUND_SERVICE
        //                            }
        //                    context.stopService(
        //                        serviceIntent
        //                    ) // stopService is fine, it will call onDestroy in service
        //                    isServiceRunning = false
        //                }
        //            }
        //        }
    }

    override fun onComplete(session: IDownloadSession) {
        super.onComplete(session)
        val record = sessionMap[session]?.get() ?: return
        dbHelper.updateDownloadState(record.sessionId, DownloadState.COMPLETED)
        notifyListeners(session) { s, r -> updateState(s, r) }
        updateFlow()
        checkAndControlService()
    }

    override fun onPause(session: IDownloadSession) {
        super.onPause(session)
        val record = sessionMap[session]?.get() ?: return
        dbHelper.updateDownloadState(record.sessionId, DownloadState.PAUSED)
        notifyListeners(session) { s, r -> updateState(s, r) }
        updateFlow()
        checkAndControlService()
    }

    override fun onProgress(session: IDownloadSession) {
        super.onProgress(session)
        notifyListeners(session) { s, r -> progress(s, r) }
        updateFlow()
    }

    override fun onError(session: IDownloadSession, reason: Exception) {
        super.onError(session, reason)
        val record = sessionMap[session]?.get() ?: return
        dbHelper.updateDownloadState(record.sessionId, DownloadState.ERROR)
        notifyListeners(session) { s, r -> updateState(s, r) }
        updateFlow()
        checkAndControlService()
    }

    override fun onResume(session: IDownloadSession, savePath: String) {
        super.onResume(session, savePath)
        val record = sessionMap[session]?.get() ?: return
        dbHelper.updateDownloadState(
            record.sessionId,
            DownloadState.DOWNLOADING
        )
        notifyListeners(session) { s, r -> updateState(s, r) }
        updateFlow()
        checkAndControlService()
    }
}
