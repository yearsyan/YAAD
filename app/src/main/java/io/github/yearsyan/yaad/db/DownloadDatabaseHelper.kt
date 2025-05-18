package io.github.yearsyan.yaad.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import io.github.yearsyan.yaad.downloader.DownloadManager.DownloadSessionRecord
import io.github.yearsyan.yaad.downloader.DownloadManager.DownloadType
import io.github.yearsyan.yaad.downloader.DownloadManager.SingleHttpDownloadSessionRecord

class DownloadDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "downloads.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_DOWNLOAD_SESSIONS = "download_sessions"
        private const val COLUMN_SESSION_ID = "session_id"
        private const val COLUMN_DOWNLOAD_TYPE = "download_type"
        private const val COLUMN_ORIGIN_LINK = "origin_link"
        private const val COLUMN_RECOVER_FILE = "recover_file"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable =
            """
            CREATE TABLE $TABLE_DOWNLOAD_SESSIONS (
                $COLUMN_SESSION_ID TEXT PRIMARY KEY,
                $COLUMN_DOWNLOAD_TYPE TEXT NOT NULL,
                $COLUMN_ORIGIN_LINK TEXT NOT NULL,
                $COLUMN_RECOVER_FILE TEXT NOT NULL
            )
        """
                .trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DOWNLOAD_SESSIONS")
        onCreate(db)
    }

    fun saveDownloadSession(record: DownloadSessionRecord) {
        val db = this.writableDatabase
        val values =
            ContentValues().apply {
                put(COLUMN_SESSION_ID, record.sessionId)
                put(COLUMN_DOWNLOAD_TYPE, record.downloadType.name)
                put(COLUMN_ORIGIN_LINK, record.originLink)
                put(COLUMN_RECOVER_FILE, record.recoverFile)
            }
        db.insertWithOnConflict(
            TABLE_DOWNLOAD_SESSIONS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
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
        val records = mutableListOf<DownloadSessionRecord>()
        val db = this.readableDatabase
        val cursor =
            db.query(
                TABLE_DOWNLOAD_SESSIONS,
                null,
                null,
                null,
                null,
                null,
                null
            )

        cursor.use {
            while (it.moveToNext()) {
                val sessionId =
                    it.getString(it.getColumnIndexOrThrow(COLUMN_SESSION_ID))
                val downloadType =
                    DownloadType.valueOf(
                        it.getString(
                            it.getColumnIndexOrThrow(COLUMN_DOWNLOAD_TYPE)
                        )
                    )
                val originLink =
                    it.getString(it.getColumnIndexOrThrow(COLUMN_ORIGIN_LINK))
                val recoverFile =
                    it.getString(it.getColumnIndexOrThrow(COLUMN_RECOVER_FILE))

                val record =
                    when (downloadType) {
                        DownloadType.SINGLE_HTTP ->
                            SingleHttpDownloadSessionRecord(
                                sessionId,
                                originLink,
                                recoverFile
                            )
                        else ->
                            DownloadSessionRecord(
                                sessionId,
                                downloadType,
                                originLink,
                                recoverFile
                            )
                    }
                records.add(record)
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
                val originLink =
                    it.getString(it.getColumnIndexOrThrow(COLUMN_ORIGIN_LINK))
                val recoverFile =
                    it.getString(it.getColumnIndexOrThrow(COLUMN_RECOVER_FILE))

                return when (downloadType) {
                    DownloadType.SINGLE_HTTP ->
                        SingleHttpDownloadSessionRecord(
                            sessionId,
                            originLink,
                            recoverFile
                        )
                    else ->
                        DownloadSessionRecord(
                            sessionId,
                            downloadType,
                            originLink,
                            recoverFile
                        )
                }
            }
        }
        return null
    }
}
