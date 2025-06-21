//
// Created by yan on 2025/6/19.
//

#ifndef YAAD_BT_H
#define YAAD_BT_H
#include <jni.h>
#include <libtorrent/libtorrent.hpp>

namespace yaad {


    class BtService {
    public:
        BtService();
        std::string add_task_by_magnet_uri(const char* uri, const char* path);
        std::unique_ptr<libtorrent::torrent_status> get_task_info_by_hash(std::string & hash);
    private:
        std::unique_ptr<libtorrent::session> session_ = nullptr;
    };

    int register_bt(JNIEnv* env);
}

#endif //YAAD_BT_H
