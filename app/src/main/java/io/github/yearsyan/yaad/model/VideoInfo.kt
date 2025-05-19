package io.github.yearsyan.yaad.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class VideoInfo(
    @SerialName("url") val url: String,
    @SerialName("title") val title: String,
    @SerialName("site") val site: String,
    @SerialName("streams") val streams: Map<String, StreamInfo>,
    @SerialName("extra") val extra: Map<String, String>,
    @SerialName("request_headers") val requestHeaders: Map<String,String>
) : Parcelable

@Parcelize
@Serializable
data class StreamInfo(
    @SerialName("container") val container: String,
    @SerialName("quality") val quality: String,
    @SerialName("src") val src: List<List<String>>,
    @SerialName("size") val size: Long
) : Parcelable
