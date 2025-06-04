package io.github.yearsyan.yaad.utils

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentType
import kotlinx.coroutines.cancel
import java.net.URI
import java.net.URL
import java.net.URLDecoder

data class FileInfo(
    val fileName: String,
    val contentType: String,
    val contentLength: Long?,
    val acceptRanges: Boolean,
    val chunked: Boolean
)

fun parseOriginFromReferer(referer: String?): String? {
    if (referer.isNullOrBlank()) return null

    return try {
        val uri = URI(referer)
        // 构造 origin: scheme://host[:port]
        val scheme = uri.scheme ?: return null
        val host = uri.host ?: return null
        val port = uri.port
        if (port == -1 || (scheme == "http" && port == 80) || (scheme == "https" && port == 443)) {
            "$scheme://$host"
        } else {
            "$scheme://$host:$port"
        }
    } catch (e: Exception) {
        null
    }
}

suspend fun getFileInfo(url: String): FileInfo {
    val client = HttpClient(CIO) {
        // 你可以在这里根据需要添加超时、日志等配置
    }

    val response = client.get(url)
    val headers = response.headers
    val contentTypeStr: String = response.contentType()?.toString() ?: ""
    val contentLength: Long? = headers["Content-Length"]?.toLongOrNull()
    val acceptRanges: Boolean = headers["Accept-Ranges"]?.equals("bytes", ignoreCase = true) == true
    val chunked: Boolean = headers["Transfer-Encoding"]?.contains("chunked", ignoreCase = true) == true

    val fileName: String = run {
        // 先看 Content-Disposition
        val disposition: String? = headers["Content-Disposition"]
        if (!disposition.isNullOrEmpty()) {
            // 常见格式： Content-Disposition: attachment; filename="测试文档.pdf"
            // 也可能是 RFC5987 格式： filename*=UTF-8''%E6%B5%8B%E8%AF%95%E6%96%87%E6%A1%A3.pdf
            // 下面用一个简单的正则先匹配 filename*，再匹配 filename
            // filename* 情况（RFC5987）：UTF-8''<percent-encoded>
            val regexRfc5987 = Regex("""filename\*\s*=\s*UTF-8''([^;\"']+)""", RegexOption.IGNORE_CASE)
            val matchRfc = regexRfc5987.find(disposition)
            if (matchRfc != null) {
                val encoded = matchRfc.groupValues[1]
                try {
                    URLDecoder.decode(encoded, "UTF-8")
                } catch (_: Exception) {
                    // 若解码失败，退回到原始 encoded
                    encoded
                }
            } else {
                // 再尝试普通的 filename="..."
                val regexSimple = Regex("""filename\s*=\s*"([^"]+)"""")
                val matchSimple = regexSimple.find(disposition)
                if (matchSimple != null) {
                    matchSimple.groupValues[1]
                } else {
                    // 再尝试不带引号但不带分号的情形： filename=abc.pdf
                    val regexNoQuote = Regex("""filename\s*=\s*([^;]+)""")
                    val matchNoQuote = regexNoQuote.find(disposition)
                    if (matchNoQuote != null) {
                        matchNoQuote.groupValues[1]
                    } else {
                        // 如果都匹配不到，就 fallback 到 URL
                        null
                    }
                }
            }
        } else {
            null
        }
    } ?: run {
        try {
            val path = URL(url).path   // e.g. "/files/报告.pdf"
            val segs = path.split('/')
            val last = segs.lastOrNull().orEmpty()
            if (last.isNotBlank()) URLDecoder.decode(last, "UTF-8") else url
        } catch (_: Exception) {
            url
        }
    }

    response.call.cancel()
    client.close()
    return FileInfo(
        fileName = fileName,
        contentType = contentTypeStr,
        contentLength = contentLength,
        acceptRanges = acceptRanges,
        chunked = chunked
    )
}

suspend fun getResponseContentType(url: String): String {
    val client = HttpClient(CIO)
    val response = client.get(url)
    val contentType = response.contentType()?.contentType ?: ""
    response.call.cancel()
    return contentType
}