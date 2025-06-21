#include "bt.h"
#include <libtorrent/libtorrent.hpp>
#include <android/log.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "BT_DOWNLOAD", __VA_ARGS__)

namespace lt = libtorrent;
namespace yaad {

    extern "C" jstring JNICALL native_add_task_link(JNIEnv *env, jobject thiz,
                                            jstring link, jstring save_at) {
        auto field_id = env->GetFieldID(env->GetObjectClass(thiz), "ptr", "J");
        auto service = reinterpret_cast<yaad::BtService*>(env->GetLongField(thiz, field_id));
        if (service == nullptr)  {
            return nullptr;
        }
        auto link_c_str = env->GetStringUTFChars(link, nullptr);
        auto path_c_str = env->GetStringUTFChars(save_at, nullptr);
        auto hash = service->add_task_by_magnet_uri(link_c_str, path_c_str);
        env->ReleaseStringUTFChars(link, link_c_str);
        env->ReleaseStringUTFChars(save_at, path_c_str);
        return env->NewStringUTF(hash.c_str());
    }

    extern "C"  void JNICALL native_init_service(JNIEnv *env, jobject thiz) {
        auto service = new yaad::BtService();
        auto field_id = env->GetFieldID(env->GetObjectClass(thiz), "ptr", "J");
        env->SetLongField(thiz, field_id, reinterpret_cast<jlong>(service));
    }

    extern "C" jstring JNICALL native_get_task_status(JNIEnv *env, jobject thiz,
                                                                              jstring hash) {
        auto field_id = env->GetFieldID(env->GetObjectClass(thiz), "ptr", "J");
        auto service = reinterpret_cast<yaad::BtService*>(env->GetLongField(thiz, field_id));
        if (service == nullptr)  {
            return nullptr;
        }
        auto hash_c_str = env->GetStringUTFChars(hash, nullptr);
        std::string hash_str = hash_c_str;
        auto status = service->get_task_info_by_hash(hash_str);
        if (status == nullptr) {
            return nullptr;
        }
        std::ostringstream oss;
        oss << "progress: " << status->progress << " rate: " <<  status->download_rate;
        auto str = oss.str();
        LOGI("%s", str.c_str());
        return env->NewStringUTF(str.c_str());
    }

    static JNINativeMethod methods[] = {
            {"initService",  "()V",  (void*) native_init_service},
            {"addTaskByLink","(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", (void*) native_add_task_link},
            {"getTaskStatus",  "(Ljava/lang/String;)Ljava/lang/String;", (void*) native_get_task_status}
    };

    int register_bt(JNIEnv* env) {
        auto clazz = env->FindClass("io/github/yaad/downloader_core/torrent/TorrentService");
        if (!clazz) {
            return JNI_ERR;
        }
        if (env->RegisterNatives(clazz, methods, sizeof(methods)/sizeof(methods[0])) < 0) {
            return JNI_ERR;
        }
        return 0;
    }

    std::unique_ptr<lt::info_hash_t> parse_info_hash_from_hex(const std::string& hex)
    {
        if (hex.size() == 40) { // SHA-1 / v1
            std::array<char, 20> buf;
            if (!lt::aux::from_hex(lt::span<char const>{hex.data(), static_cast<ptrdiff_t>(hex.size())}, buf.data()))
                return nullptr;
            return std::make_unique<lt::info_hash_t>(lt::sha1_hash(buf));
        }
        else if (hex.size() == 64) { // SHA-256 / v2
            std::array<char, 32> buf;
            if (!lt::aux::from_hex(lt::span<char const>{hex.data(), static_cast<ptrdiff_t>(hex.size())}, buf.data()))
                return nullptr;
            return std::make_unique<lt::info_hash_t>(lt::sha256_hash(buf));
        }

        return nullptr;
    }

    BtService::BtService() {
        lt::settings_pack settings;
        settings.set_int(lt::settings_pack::alert_mask,
                         lt::alert::status_notification |
                         lt::alert::error_notification |
                         lt::alert::storage_notification);

        settings.set_bool(lt::settings_pack::enable_dht, true);
        settings.set_bool(lt::settings_pack::enable_lsd, true);
        settings.set_bool(lt::settings_pack::enable_upnp, true);
        settings.set_bool(lt::settings_pack::enable_natpmp, true);
        settings.set_bool(lt::settings_pack::enable_outgoing_utp, true);
        settings.set_bool(lt::settings_pack::enable_incoming_utp, true);
        settings.set_bool(lt::settings_pack::enable_ip_notifier, true);

        session_ = std::make_unique<lt::session>(settings);
    }

    std::string BtService::add_task_by_magnet_uri(const char* uri, const char* path) {
        lt::error_code ec;
        lt::add_torrent_params atp = lt::parse_magnet_uri(uri, ec);
        if (ec) {
            std::cerr << "解析 magnet 失败: " << ec.message() << "\n";
            return "";
        }
        atp.save_path = path;  // 下载保存路径
        atp.flags |= lt::torrent_flags::seed_mode;

        auto handle = session_->add_torrent(atp, ec);
        if (handle.info_hashes().has_v2()) {
            return lt::aux::to_hex(handle.info_hashes().v2 );
        } else {
            return lt::aux::to_hex(handle.info_hashes().v1 );
        }
        return "";
    }

    std::unique_ptr<libtorrent::torrent_status> BtService::get_task_info_by_hash(std::string & hash) {
        auto info_hash = parse_info_hash_from_hex(hash);
        if (info_hash == nullptr) {
            return nullptr;
        }

        std::unique_ptr<lt::torrent_handle>  handle = nullptr;
        if (info_hash->has_v2()) {
            handle = std::make_unique<lt::torrent_handle>(session_->find_torrent(info_hash->v2));
        } else {
            handle = std::make_unique<lt::torrent_handle>(session_->find_torrent(info_hash->v1));
        }
        if (handle != nullptr && handle->is_valid()) {
            return std::make_unique<lt::torrent_status>(handle->status());
        }
        return nullptr;
    }

}