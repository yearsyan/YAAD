package io.github.yearsyan.yaad.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import io.github.yaad.downloader_core.DownloadState
import io.github.yearsyan.yaad.downloader.DownloadManager.DownloadSessionRecord
import io.github.yearsyan.yaad.downloader.DownloadManager.DownloadType
import io.github.yearsyan.yaad.downloader.DownloadManager.SingleHttpDownloadSessionRecord
import io.github.yearsyan.yaad.downloader.DownloadManager.ExtractedMediaDownloadSessionRecord
import io.github.yearsyan.yaad.downloader.DownloadManager.ChildHttpDownloadSessionRecord
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class DownloadDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "download.db"
        private const val DATABASE_VERSION = 2

        private const val TABLE_DOWNLOAD_SESSIONS = "download_sessions"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_SESSION_ID = "session_id"
        private const val COLUMN_DOWNLOAD_TYPE = "download_type"
        private const val COLUMN_ORIGIN_LINK = "origin_link"
        private const val COLUMN_RECOVER_FILE = "recover_file"
        private const val COLUMN_SAVE_PATH = "save_path"
        private const val COLUMN_MEDIA_URLS = "media_urls"
        private const val COLUMN_PARENT_SESSION_ID = "parent_session_id"
        private const val COLUMN_DOWNLOAD_STATE = "download_state"

        private val json = Json { ignoreUnknownKeys = true }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_DOWNLOAD_SESSIONS (
                $COLUMN_SESSION_ID TEXT PRIMARY KEY,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_DOWNLOAD_TYPE TEXT NOT NULL,
                $COLUMN_ORIGIN_LINK TEXT NOT NULL,
                $COLUMN_RECOVER_FILE TEXT NOT NULL,
                $COLUMN_SAVE_PATH TEXT NOT NULL,
                $COLUMN_MEDIA_URLS TEXT NOT NULL,
                $COLUMN_PARENT_SESSION_ID TEXT NOT NULL,
                $COLUMN_DOWNLOAD_STATE TEXT NOT NULL
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // 添加下载状态字段
            db.execSQL("ALTER TABLE $TABLE_DOWNLOAD_SESSIONS ADD COLUMN $COLUMN_DOWNLOAD_STATE TEXT NOT NULL DEFAULT 'PENDING'")
        }
    }

    fun saveDownloadSession(record: DownloadSessionRecord) {
        val db = this.writableDatabase
        val values =
            ContentValues().apply {
                put(COLUMN_SESSION_ID, record.sessionId)
                put(COLUMN_DOWNLOAD_TYPE, record.downloadType.name)
                put(COLUMN_ORIGIN_LINK, record.originLink)
                put(COLUMN_RECOVER_FILE, record.recoverFile)
                put(COLUMN_SAVE_PATH, record.savePath)
                put(COLUMN_TITLE, record.title)
                
                // 根据不同的记录类型获取下载状态
                val downloadState = when (record) {
                    is SingleHttpDownloadSessionRecord -> record.httpDownloadSession?.getStatus()?.state?.name ?: "PENDING"
                    is ChildHttpDownloadSessionRecord -> record.httpDownloadSession?.getStatus()?.state?.name ?: "PENDING"
                    is ExtractedMediaDownloadSessionRecord -> {
                        // 对于 ExtractedMediaDownloadSessionRecord，检查所有子会话的状态
                        if (record.childSessions.isEmpty()) "PENDING"
                        else if (record.childSessions.all { it.httpDownloadSession?.getStatus()?.state == DownloadState.COMPLETED }) "COMPLETED"
                        else if (record.childSessions.any { it.httpDownloadSession?.getStatus()?.state == DownloadState.DOWNLOADING }) "DOWNLOADING"
                        else if (record.childSessions.any { it.httpDownloadSession?.getStatus()?.state == DownloadState.ERROR }) "ERROR"
                        else if (record.childSessions.any { it.httpDownloadSession?.getStatus()?.state == DownloadState.PAUSED }) "PAUSED"
                        else "PENDING"
                    }
                    else -> "PENDING"
                }
                put(COLUMN_DOWNLOAD_STATE, downloadState)
                
                when (record) {
                    is ExtractedMediaDownloadSessionRecord -> {
                        put(COLUMN_MEDIA_URLS, json.encodeToString(record.mediaUrls))
                        put(COLUMN_PARENT_SESSION_ID, "")
                    }
                    is ChildHttpDownloadSessionRecord -> {
                        put(COLUMN_MEDIA_URLS, "")
                        put(COLUMN_PARENT_SESSION_ID, record.parentSessionId)
                    }
                    else -> {
                        put(COLUMN_MEDIA_URLS, "")
                        put(COLUMN_PARENT_SESSION_ID, "")
                    }
                }
            }
        db.insertWithOnConflict(
            TABLE_DOWNLOAD_SESSIONS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun updateDownloadState(sessionId: String, state: DownloadState) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_DOWNLOAD_STATE, state.name)
        }
        db.update(
            TABLE_DOWNLOAD_SESSIONS,
            values,
            "$COLUMN_SESSION_ID = ?",
            arrayOf(sessionId)
        )
    }

    fun deleteDownloadSession(sessionId: String) {
        val db = this.writableDatabase
        db.delete(
            TABLE_DOWNLOAD_SESSIONS,
            "$COLUMN_SESSION_ID = ?",
            arrayOf(sessionId)
        )
    }

    fun getAllDownloadSessions(): List<DownloadSessionRecord> {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_DOWNLOAD_SESSIONS,
            null,
            null,
            null,
            null,
            null,
            null
        )

        val records = mutableListOf<DownloadSessionRecord>()
        cursor.use {
            while (it.moveToNext()) {
                val sessionId = it.getString(it.getColumnIndexOrThrow(COLUMN_SESSION_ID))
                val title = it.getString(it.getColumnIndexOrThrow(COLUMN_TITLE))
                val downloadType = it.getString(it.getColumnIndexOrThrow(COLUMN_DOWNLOAD_TYPE))
                val originLink = it.getString(it.getColumnIndexOrThrow(COLUMN_ORIGIN_LINK))
                val recoverFile = it.getString(it.getColumnIndexOrThrow(COLUMN_RECOVER_FILE))
                val savePath = it.getString(it.getColumnIndexOrThrow(COLUMN_SAVE_PATH))
                val mediaUrls = it.getString(it.getColumnIndexOrThrow(COLUMN_MEDIA_URLS))
                val parentSessionId = it.getString(it.getColumnIndexOrThrow(COLUMN_PARENT_SESSION_ID))
                val downloadState = DownloadState.valueOf(it.getString(it.getColumnIndexOrThrow(COLUMN_DOWNLOAD_STATE)))

                val record = when (DownloadType.valueOf(downloadType)) {
                    DownloadType.SINGLE_HTTP -> {
                        SingleHttpDownloadSessionRecord(
                            title = title,
                            sessionId = sessionId,
                            originLink = originLink,
                            recoverFile = recoverFile,
                            savePath = savePath,
                            downloadState = downloadState
                        )
                    }
                    DownloadType.EXTRACTED_MEDIA -> {
                        ExtractedMediaDownloadSessionRecord(
                            title = title,
                            sessionId = sessionId,
                            originLink = originLink,
                            recoverFile = recoverFile,
                            mediaUrls = if (mediaUrls.isNotEmpty()) json.decodeFromString(mediaUrls) else emptyList(),
                            downloadState
                        )
                    }
                    DownloadType.CHILD_HTTP -> {
                        ChildHttpDownloadSessionRecord(
                            title = title,
                            sessionId = sessionId,
                            originLink = originLink,
                            recoverFile = recoverFile,
                            savePath = savePath,
                            parentSessionId = parentSessionId,
                            downloadState
                        )
                    }
                    else -> null
                }
                record?.let { records.add(it) }
            }
        }
        return records
    }

    fun getDownloadSession(sessionId: String): DownloadSessionRecord? {
        val db = this.readableDatabase
        val cursor =
            db.query(
                TABLE_DOWNLOAD_SESSIONS,
                null,
                "$COLUMN_SESSION_ID = ?",
                arrayOf(sessionId),
                null,
                null,
                null
            )

        cursor.use {
            if (it.moveToFirst()) {
                val downloadType =
                    DownloadType.valueOf(
                        it.getString(
                            it.getColumnIndexOrThrow(COLUMN_DOWNLOAD_TYPE)
                        )
                    )
                val title =
                    it.getString(it.getColumnIndexOrThrow(COLUMN_TITLE))
                val originLink =
                    it.getString(it.getColumnIndexOrThrow(COLUMN_ORIGIN_LINK))
                val recoverFile =
                    it.getString(it.getColumnIndexOrThrow(COLUMN_RECOVER_FILE))
                val savePath = 
                    it.getString(it.getColumnIndexOrThrow(COLUMN_SAVE_PATH)) ?: ""
                val mediaUrlsJson = 
                    it.getString(it.getColumnIndexOrThrow(COLUMN_MEDIA_URLS)) ?: ""
                val parentSessionId = 
                    it.getString(it.getColumnIndexOrThrow(COLUMN_PARENT_SESSION_ID)) ?: ""
                val downloadState =
                    DownloadState.valueOf(it.getString(it.getColumnIndexOrThrow(COLUMN_DOWNLOAD_STATE)))

                return when (downloadType) {
                    DownloadType.SINGLE_HTTP ->
                        SingleHttpDownloadSessionRecord(
                            sessionId,
                            originLink,
                            recoverFile,
                            savePath,
                            "",
                            downloadState
                        )
                    DownloadType.EXTRACTED_MEDIA -> {
                        val mediaUrls = try {
                            if (mediaUrlsJson.isNotEmpty()) {
                                json.decodeFromString<List<String>>(mediaUrlsJson)
                            } else {
                                emptyList()
                            }
                        } catch (e: Exception) {
                            emptyList<String>()
                        }
                        ExtractedMediaDownloadSessionRecord(
                            title,
                            sessionId,
                            originLink,
                            recoverFile,
                            mediaUrls,
                            downloadState
                        )
                    }
                    DownloadType.CHILD_HTTP ->
                        ChildHttpDownloadSessionRecord(
                            title,
                            sessionId,
                            originLink,
                            recoverFile,
                            savePath,
                            parentSessionId,
                            downloadState
                        )
                    else ->
                        DownloadSessionRecord(
                            title,
                            sessionId,
                            downloadType,
                            originLink,
                            recoverFile,
                            savePath,
                            downloadState
                        )
                }
            }
        }
        return null
    }
}
