//
// Created by yan on 2025/6/19.
//

#ifndef YAAD_BT_H
#define YAAD_BT_H
#include <jni.h>
#include <libtorrent/libtorrent.hpp>

namespace yaad {

    typedef long long task_id_t;

    class BtService {
    public:
        BtService();
        task_id_t add_task_by_magnet_uri(const char* uri, const char* path);
        std::unique_ptr<libtorrent::torrent_status> get_task_info(task_id_t task_id);
        void update_torrent(std::function<void (task_id_t task,const libtorrent::torrent_status& status)> cb);
        void task_pause(task_id_t task_id);
        void task_resume(task_id_t task_id);
        void task_remove(task_id_t task_id);
    private:
        inline task_id_t create_id() {
            return _id.fetch_add(1);
        }
        inline task_id_t put_handle(libtorrent::torrent_handle&& handle) {
            auto id = create_id();
            tasks_table_[id] = std::move(handle);
            return id;
        }
        inline std::unique_ptr<libtorrent::torrent_handle> get_handle(task_id_t id) {
            auto it = tasks_table_.find(id);
            if (it == tasks_table_.end() || !it->second.is_valid()) {
                return nullptr;
            }
            return std::make_unique<libtorrent::torrent_handle>(it->second);
        }
        inline task_id_t get_handle_id(libtorrent::torrent_handle& handle) {
            for (auto& it : tasks_table_) {
                if (it.second == handle) {
                    return it.first;
                }
            }
            return -1;
        }
        std::atomic_llong _id = 0;
        std::unique_ptr<libtorrent::session> session_ = nullptr;
        std::map<long, libtorrent::torrent_handle> tasks_table_ = {};
    };

    int register_bt(JNIEnv* env);
}

#endif //YAAD_BT_H
