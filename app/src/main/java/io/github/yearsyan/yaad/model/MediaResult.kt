package io.github.yearsyan.yaad.model

import kotlinx.serialization.Serializable

@Serializable
data class MediaResult(
    val result: List<MediaItem>? = null,
    val code: Int,
    val msg: String
)

@Serializable
data class MediaItem(
    val headers: Map<String, String>,
    val url: String,
    val ext: String,
    val formatId: String,
    val format: String
)
