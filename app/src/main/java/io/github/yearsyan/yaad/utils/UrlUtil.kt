package io.github.yearsyan.yaad.utils

import java.net.URI

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
