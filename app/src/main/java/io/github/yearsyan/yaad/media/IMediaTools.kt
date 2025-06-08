package io.github.yearsyan.yaad.media

interface IMediaTools {
    fun mergeAV(video: String, audio: String, out: String): Int

    fun mergeSplice(filePathList: Array<String>, out: String)
}
