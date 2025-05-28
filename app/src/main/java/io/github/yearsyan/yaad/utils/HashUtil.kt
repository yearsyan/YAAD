package io.github.yearsyan.yaad.utils

import java.security.MessageDigest

fun String.sha512(): String {
    val bytes =
        MessageDigest.getInstance("SHA-512")
            .digest(this.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}