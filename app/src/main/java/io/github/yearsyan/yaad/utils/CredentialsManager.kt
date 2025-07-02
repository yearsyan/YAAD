package io.github.yearsyan.yaad.utils

import android.content.Context
import com.tencent.mmkv.MMKV
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** 凭据类型 */
enum class CredentialType {
    SMB,
    FTP,
    HTTP,
    SFTP,
    WEBDAV
}

/** 通用凭据信息 */
@Serializable
data class Credential(
    val type: CredentialType,
    val host: String,
    val port: Int? = null,
    val username: String,
    val password: String,
    val domain: String? = null,
    val path: String? = null
)

/** 通用凭据管理器 使用MMKV进行数据持久化 */
class CredentialsManager private constructor(context: Context) {

    companion object {
        @Volatile private var INSTANCE: CredentialsManager? = null

        fun getInstance(context: Context): CredentialsManager {
            return INSTANCE
                ?: synchronized(this) {
                    INSTANCE
                        ?: CredentialsManager(context.applicationContext).also {
                            INSTANCE = it
                        }
                }
        }

        private const val CREDENTIALS_KEY = "credentials"
    }

    private val mmkv: MMKV = MMKV.defaultMMKV()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val appContext = context.applicationContext

    /** 保存凭据 */
    fun saveCredential(credential: Credential) {
        try {
            val key = generateKey(credential.type, credential.host, credential.port)
            val credentialJson = json.encodeToString(credential)
            mmkv.encode(key, credentialJson)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** 保存凭据（简化版本） */
    fun saveCredential(type: CredentialType, host: String, username: String, password: String, port: Int? = null, domain: String? = null, path: String? = null) {
        val credential = Credential(type, host, port, username, password, domain, path)
        saveCredential(credential)
    }

    /** 加载凭据 */
    fun loadCredential(type: CredentialType, host: String, port: Int? = null): Credential? {
        try {
            val key = generateKey(type, host, port)
            val credentialJson = mmkv.decodeString(key, "") ?: ""
            return if (credentialJson.isNotEmpty()) {
                json.decodeFromString<Credential>(credentialJson)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /** 删除凭据 */
    fun removeCredential(type: CredentialType, host: String, port: Int? = null) {
        try {
            val key = generateKey(type, host, port)
            mmkv.removeValueForKey(key)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** 检查是否有保存的凭据 */
    fun hasCredential(type: CredentialType, host: String, port: Int? = null): Boolean {
        return loadCredential(type, host, port) != null
    }

    /** 获取指定类型的所有凭据 */
    fun getCredentialsByType(type: CredentialType): List<Credential> {
        val credentials = mutableListOf<Credential>()
        val allKeys = mmkv.allKeys()
        allKeys?.forEach { key ->
            if (key.startsWith("$CREDENTIALS_KEY:$type:")) {
                try {
                    val credentialJson = mmkv.decodeString(key, "") ?: ""
                    if (credentialJson.isNotEmpty()) {
                        val credential = json.decodeFromString<Credential>(credentialJson)
                        credentials.add(credential)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return credentials
    }

    /** 获取所有凭据 */
    fun getAllCredentials(): List<Credential> {
        val credentials = mutableListOf<Credential>()
        val allKeys = mmkv.allKeys()
        allKeys?.forEach { key ->
            if (key.startsWith("$CREDENTIALS_KEY:")) {
                try {
                    val credentialJson = mmkv.decodeString(key, "") ?: ""
                    if (credentialJson.isNotEmpty()) {
                        val credential = json.decodeFromString<Credential>(credentialJson)
                        credentials.add(credential)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return credentials
    }

    /** 获取所有主机列表（按类型分组） */
    fun getAllHostsByType(): Map<CredentialType, List<String>> {
        val hostsByType = mutableMapOf<CredentialType, MutableList<String>>()
        getAllCredentials().forEach { credential ->
            hostsByType.getOrPut(credential.type) { mutableListOf() }.add(credential.host)
        }
        return hostsByType
    }

    /** 清除指定类型的所有凭据 */
    fun clearCredentialsByType(type: CredentialType) {
        try {
            val allKeys = mmkv.allKeys()
            allKeys?.forEach { key ->
                if (key.startsWith("$CREDENTIALS_KEY:$type:")) {
                    mmkv.removeValueForKey(key)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** 清除所有凭据 */
    fun clearAllCredentials() {
        try {
            val allKeys = mmkv.allKeys()
            allKeys?.forEach { key ->
                if (key.startsWith("$CREDENTIALS_KEY:")) {
                    mmkv.removeValueForKey(key)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** 生成存储键 */
    private fun generateKey(type: CredentialType, host: String, port: Int?): String {
        return if (port != null) {
            "$CREDENTIALS_KEY:$type:$host:$port"
        } else {
            "$CREDENTIALS_KEY:$type:$host"
        }
    }

    // 便捷方法 - SMB 相关
    fun saveSmbCredential(host: String, username: String, password: String) {
        saveCredential(CredentialType.SMB, host, username, password)
    }

    fun loadSmbCredential(host: String): Credential? {
        return loadCredential(CredentialType.SMB, host)
    }

    fun hasSmbCredential(host: String): Boolean {
        return hasCredential(CredentialType.SMB, host)
    }

    fun removeSmbCredential(host: String) {
        removeCredential(CredentialType.SMB, host)
    }

    // 便捷方法 - FTP 相关
    fun saveFtpCredential(host: String, username: String, password: String, port: Int = 21) {
        saveCredential(CredentialType.FTP, host, username, password, port)
    }

    fun loadFtpCredential(host: String, port: Int = 21): Credential? {
        return loadCredential(CredentialType.FTP, host, port)
    }

    // 便捷方法 - HTTP 相关
    fun saveHttpCredential(host: String, username: String, password: String, port: Int? = null) {
        saveCredential(CredentialType.HTTP, host, username, password, port)
    }

    fun loadHttpCredential(host: String, port: Int? = null): Credential? {
        return loadCredential(CredentialType.HTTP, host, port)
    }
} 