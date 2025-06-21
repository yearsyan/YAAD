package io.github.yearsyan.yaad.media

object FFmpegTools : IMediaTools {

    external override fun mergeAV(
        video: String,
        audio: String,
        out: String
    ): Int

    external fun configuration(): String

    override fun mergeSplice(filePathList: Array<String>, out: String) {}
}
