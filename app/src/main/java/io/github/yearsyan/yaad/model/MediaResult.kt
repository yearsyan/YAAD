package io.github.yearsyan.yaad.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class MediaResult(
    val result: VideoInfo? = null,
    val code: Int,
    val msg: String
) : Parcelable

@Parcelize
@Serializable
data class MediaItem(
    val headers: Map<String, String>,
    val url: String,
    val ext: String,
    val formatId: String,
    val format: String
) : Parcelable
