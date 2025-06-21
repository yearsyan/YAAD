package io.github.yaad.downloader_core.torrent

class TorrentService {

    init {
        System.loadLibrary("downloader-core")
    }

    companion object {
        private var instance_: TorrentService? = null
        val instance get()  = {
            if (instance_ == null) {
                instance_ = TorrentService()
            }
            instance_!!
        }
    }

    private var ptr: Long = 0;
    constructor() {
        initService()
    }
    external fun initService();
    external fun addTaskByLink(link: String, save: String): String
    external fun getTaskStatus(hash: String): String
}