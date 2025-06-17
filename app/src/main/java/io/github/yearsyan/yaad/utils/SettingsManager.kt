package io.github.yearsyan.yaad.utils

import android.content.Context
import com.tencent.mmkv.MMKV
import io.github.yearsyan.yaad.model.AppSettings
import io.github.yearsyan.yaad.model.CookieFileInfo
import io.github.yearsyan.yaad.model.FFmpegInstallType
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** 设置管理器 使用MMKV进行数据持久化 */
class SettingsManager private constructor(context: Context) {

    companion object {
        @Volatile private var INSTANCE: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return INSTANCE
                ?: synchronized(this) {
                    INSTANCE
                        ?: SettingsManager(context.applicationContext).also {
                            INSTANCE = it
                        }
                }
        }

        private const val SETTINGS_KEY = "app_settings"
        private const val COOKIE_FILES_KEY = "cookie_files"
    }

    private val mmkv: MMKV = MMKV.defaultMMKV()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _cookieFiles = MutableStateFlow(loadCookieFiles())
    val cookieFiles: StateFlow<List<CookieFileInfo>> =
        _cookieFiles.asStateFlow()

    private val appContext = context.applicationContext

    /** 加载设置 */
    private fun loadSettings(): AppSettings {
        val settingsJson = mmkv.decodeString(SETTINGS_KEY, "") ?: ""
        return if (settingsJson.isNotEmpty()) {
            try {
                json.decodeFromString<AppSettings>(settingsJson)
            } catch (e: Exception) {
                AppSettings()
            }
        } else {
            AppSettings()
        }
    }

    /** 保存设置 */
    fun saveSettings(settings: AppSettings) {
        try {
            val settingsJson = json.encodeToString(settings)
            mmkv.encode(SETTINGS_KEY, settingsJson)
            _settings.value = settings
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** 更新FFmpeg设置 */
    fun updateFFmpegSettings(type: FFmpegInstallType, customUrl: String = "") {
        val currentSettings = _settings.value
        val newSettings =
            currentSettings.copy(
                ffmpegInstallType = type,
                ffmpegCustomUrl = customUrl
            )
        saveSettings(newSettings)
    }

    /** 更新下载线程数 */
    fun updateDownloadThreads(threads: Int) {
        val currentSettings = _settings.value
        val newSettings = currentSettings.copy(defaultDownloadThreads = threads)
        saveSettings(newSettings)
    }

    /** 更新BT下载限速设置 */
    fun updateBtDownloadSpeedLimit(speedLimit: Long) {
        val currentSettings = _settings.value
        val newSettings =
            currentSettings.copy(btDownloadSpeedLimit = speedLimit)
        saveSettings(newSettings)
    }

    /** 更新BT上传限速设置 */
    fun updateBtUploadSpeedLimit(speedLimit: Long) {
        val currentSettings = _settings.value
        val newSettings = currentSettings.copy(btUploadSpeedLimit = speedLimit)
        saveSettings(newSettings)
    }

    /** 更新下载路径 */
    fun updateDownloadPath(path: String) {
        val currentSettings = _settings.value
        val newSettings = currentSettings.copy(downloadPath = path)
        saveSettings(newSettings)
    }

    /** 加载Cookie文件列表 */
    private fun loadCookieFiles(): List<CookieFileInfo> {
        val cookieFilesJson = mmkv.decodeString(COOKIE_FILES_KEY, "") ?: ""
        return if (cookieFilesJson.isNotEmpty()) {
            try {
                json.decodeFromString<List<CookieFileInfo>>(cookieFilesJson)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    /** 保存Cookie文件列表 */
    private fun saveCookieFiles(cookieFiles: List<CookieFileInfo>) {
        try {
            val cookieFilesJson = json.encodeToString(cookieFiles)
            mmkv.encode(COOKIE_FILES_KEY, cookieFilesJson)
            _cookieFiles.value = cookieFiles
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** 添加Cookie文件 */
    fun addCookieFile(filePath: String): Boolean {
        val file = File(filePath)
        if (!file.exists() || !file.isFile) {
            return false
        }

        val cookieInfo = CookieFileInfo(name = file.name, path = filePath)

        val currentFiles = _cookieFiles.value.toMutableList()
        // 检查是否已存在相同路径的文件
        val existingIndex = currentFiles.indexOfFirst { it.path == filePath }
        if (existingIndex >= 0) {
            currentFiles[existingIndex] = cookieInfo
        } else {
            currentFiles.add(cookieInfo)
        }

        saveCookieFiles(currentFiles)
        return true
    }

    /** 删除Cookie文件 */
    fun removeCookieFile(filePath: String) {
        val currentFiles = _cookieFiles.value.toMutableList()
        currentFiles.removeAll { it.path == filePath }
        saveCookieFiles(currentFiles)
    }

    /** 设置当前使用的Cookie文件 */
    fun setCurrentCookieFile(filePath: String) {
        val currentSettings = _settings.value
        val newSettings = currentSettings.copy(cookieFilePath = filePath)
        saveSettings(newSettings)
    }

    /**
     * 获取当前使用的Cookie文件
     *
     * @return 当前Cookie文件信息，如果未设置或文件不存在则返回null
     */
    fun getCurrentCookieFile(): CookieFileInfo? {
        val currentCookiePath = _settings.value.cookieFilePath
        if (currentCookiePath.isEmpty()) {
            return null
        }

        // 检查文件是否存在
        val file = File(currentCookiePath)
        if (!file.exists() || !file.isFile) {
            return null
        }

        // 从Cookie文件列表中查找对应的文件信息
        return _cookieFiles.value.find { it.path == currentCookiePath }
            ?: CookieFileInfo(name = file.name, path = currentCookiePath)
    }

    /** 获取应用内部存储的Cookie目录 */
    fun getCookieDirectory(): File {
        val cookieDir = File(appContext.filesDir, "cookies")
        if (!cookieDir.exists()) {
            cookieDir.mkdirs()
        }
        return cookieDir
    }

    /** 刷新Cookie文件列表（检查文件是否仍然存在） */
    fun refreshCookieFiles() {
        val currentFiles = _cookieFiles.value
        val validFiles = currentFiles.filter { File(it.path).exists() }
        if (validFiles.size != currentFiles.size) {
            saveCookieFiles(validFiles)
        }
    }

    fun getThreadCount(): Int {
        return _settings.value.defaultDownloadThreads
    }
}
