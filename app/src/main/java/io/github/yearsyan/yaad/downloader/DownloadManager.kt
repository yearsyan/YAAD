package io.github.yearsyan.yaad.downloader

import android.app.Application
import android.content.Intent
import android.os.Looper
import androidx.core.content.ContextCompat
import io.github.yaad.downloader_core.DownloadState
import io.github.yaad.downloader_core.HttpDownloadSession
import io.github.yaad.downloader_core.IDownloadListener
import io.github.yaad.downloader_core.IDownloadSession
import io.github.yearsyan.yaad.db.DownloadDatabaseHelper
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object DownloadManager : IDownloadListener {
    enum class DownloadType {
        SINGLE_HTTP,
        BT,
        EXTRACTED_MEDIA,
        CHILD_HTTP
    }

    open class DownloadSessionRecord(
        val sessionId: String,
        val downloadType: DownloadType,
        val originLink: String,
        val recoverFile: String,
        val savePath: String = ""
    ) {
        open fun isActiveForService(): Boolean = false
    }

    class SingleHttpDownloadSessionRecord(
        sessionId: String,
        originLink: String,
        recoverFile: String,
        savePath: String = ""
    ) :
        DownloadSessionRecord(
            sessionId,
            DownloadType.SINGLE_HTTP,
            originLink,
            recoverFile,
            savePath
        ) {
        var httpDownloadSession: HttpDownloadSession? = null
        var jobContext: Job? = null

        override fun isActiveForService(): Boolean {
            val session = httpDownloadSession ?: return false
            val state = session.getStatus().state
            return jobContext?.isActive == true &&
                (state == DownloadState.DOWNLOADING ||
                    state == DownloadState.VALIDATING ||
                    state == DownloadState.PENDING)
        }
    }

    class ChildHttpDownloadSessionRecord(
        sessionId: String,
        originLink: String,
        recoverFile: String,
        savePath: String = "",
        val parentSessionId: String
    ) :
        DownloadSessionRecord(
            sessionId,
            DownloadType.CHILD_HTTP,
            originLink,
            recoverFile,
            savePath
        ) {
        var httpDownloadSession: HttpDownloadSession? = null
        var jobContext: Job? = null

        override fun isActiveForService(): Boolean {
            val session = httpDownloadSession ?: return false
            val state = session.getStatus().state
            return jobContext?.isActive == true &&
                (state == DownloadState.DOWNLOADING ||
                    state == DownloadState.VALIDATING ||
                    state == DownloadState.PENDING)
        }
    }

    class ExtractedMediaDownloadSessionRecord(
        sessionId: String,
        originLink: String,
        recoverFile: String,
        val mediaUrls: List<String>,
    ) : DownloadSessionRecord(
        sessionId,
        DownloadType.EXTRACTED_MEDIA,
        originLink,
        recoverFile
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

    private var isServiceRunning = false
    private val serviceControlMutex = Mutex()

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
            downloadTasks.addAll(savedSessions)
            _tasksFlow.value = downloadTasks.toList()
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
        val fileName = try {
            java.net.URL(url).path.substringAfterLast('/').takeIf { it.isNotEmpty() } ?: "download_${System.currentTimeMillis()}"
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
        val job =
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
        record.jobContext = job

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
        originUrl: String,
        mediaUrls: List<String>,
        headers: Map<String, String> = emptyMap(),
        onAllComplete: (saves: List<String>) -> Unit = {}
    ): DownloadSessionRecord {
        val sessionId = UUID.randomUUID().toString()
        val recoverDir = File(context.filesDir, sessionId)
        if (!recoverDir.exists()) {
            recoverDir.mkdirs()
        }

        val record = ExtractedMediaDownloadSessionRecord(
            sessionId = sessionId,
            originLink = originUrl,
            recoverFile = recoverDir.absolutePath,
            mediaUrls = mediaUrls,
        )

        synchronized(downloadTasks) {
            downloadTasks.add(record)
            _tasksFlow.value = downloadTasks.toList()
        }

        dbHelper.saveDownloadSession(record)

        // 开始所有子任务的下载
        mediaUrls.forEach { url ->
            val childSessionId = UUID.randomUUID().toString()
            val fileName = try {
                java.net.URL(url).path.substringAfterLast('/').takeIf { it.isNotEmpty() } ?: "download_${System.currentTimeMillis()}"
            } catch (e: Exception) {
                "download_${url.sha512()}"
            }
            val fileDist = File(recoverDir, fileName)
            val httpSession = HttpDownloadSession(
                url = url,
                path = fileDist.absolutePath,
                headers = headers
            )

            httpSession.addDownloadListener(this)
            val job = downloadScope.launch(Dispatchers.IO) {
                try {
                    httpSession.start()
                    synchronized(record) {
                        record.completedCount++
                        if (record.completedCount == mediaUrls.size) {
                            onAllComplete(record.childSessions.map { it.savePath })
                        }
                    }
                } catch (e: Exception) {
                    // 处理错误情况
                } finally {
                    checkAndControlService()
                }
            }

            val childRecord = ChildHttpDownloadSessionRecord(
                sessionId = childSessionId,
                originLink = url,
                recoverFile = httpSession.recoverFilePath(),
                savePath = fileDist.absolutePath,
                parentSessionId = sessionId
            )
            childRecord.httpDownloadSession = httpSession
            childRecord.jobContext = job

            sessionMap[httpSession] = WeakReference(childRecord)
            record.childSessions.add(childRecord)

            synchronized(downloadTasks) {
                downloadTasks.add(childRecord)
                _tasksFlow.value = downloadTasks.toList()
            }

            dbHelper.saveDownloadSession(childRecord)

            synchronized(downloadListeners) {
                downloadListeners.forEach { it.onCreateTask(httpSession, childRecord) }
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
            }
            is ChildHttpDownloadSessionRecord -> {
                val session = record.httpDownloadSession
                synchronized(downloadTasks) {
                    session?.let { sessionMap.remove(it) }
                    downloadTasks.remove(record)
                    _tasksFlow.value = downloadTasks.toList()
                }
                session?.stop()
            }
            is ExtractedMediaDownloadSessionRecord -> {
                val sessionsToStop = mutableListOf<HttpDownloadSession>()
                synchronized(downloadTasks) {
                    record.childSessions.forEach { childRecord ->
                        childRecord.httpDownloadSession?.let { 
                            sessionMap.remove(it)
                            sessionsToStop.add(it)
                        }
                        downloadTasks.remove(childRecord)
                    }
                    downloadTasks.remove(record)
                    _tasksFlow.value = downloadTasks.toList()
                }
                sessionsToStop.forEach { it.stop() }
            }
        }

        dbHelper.deleteDownloadSession(sessionId)
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
            _tasksFlow.value = downloadTasks.toList()
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
        notifyListeners(session) { s, r -> updateState(s, r) }
        updateFlow()
        checkAndControlService()
    }

    override fun onPause(session: IDownloadSession) {
        super.onPause(session)
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
        notifyListeners(session) { s, r -> updateState(s, r) }
        updateFlow()
        checkAndControlService()
    }

    override fun onResume(session: IDownloadSession, savePath: String) {
        super.onResume(session, savePath)
        notifyListeners(session) { s, r -> updateState(s, r) }
        updateFlow()
        checkAndControlService()
    }
}
