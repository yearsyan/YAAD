package io.github.yearsyan.yaad.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Parcelize
@Serializable
data class MediaResult(
    @SerialName("result") val result: VideoInfo? = null,
    @SerialName("code") val code: Int,
    @SerialName("msg") val msg: String
) : Parcelable

@Parcelize
@Serializable
data class MediaItem(
    @SerialName("headers") val headers: Map<String, String>,
    @SerialName("url") val url: String,
    @SerialName("ext") val ext: String,
    @SerialName("format_id") val formatId: String,
    @SerialName("format") val format: String
) : Parcelable
