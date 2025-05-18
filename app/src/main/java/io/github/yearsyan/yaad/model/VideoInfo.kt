package io.github.yearsyan.yaad.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class VideoInfo(
    val url: String,
    val title: String,
    val site: String,
    val streams: Map<String, StreamInfo>,
    val extra: Map<String, String>
) : Parcelable

@Parcelize
@Serializable
data class StreamInfo(
    val container: String,
    val quality: String,
    val src: List<List<String>>,
    val size: Long
) : Parcelable
