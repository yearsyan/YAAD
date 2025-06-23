#include "bt.h"
#include <libtorrent/libtorrent.hpp>
#include <android/log.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "BT_DOWNLOAD", __VA_ARGS__)

#define TORRENT_SERVICE_CLASS ("io/github/yaad/downloader_core/torrent/TorrentService")

namespace lt = libtorrent;
namespace yaad {

    static jfieldID ptr_file_id = 0;
    static jmethodID update_method_id = 0;

    BtService* get_service(JNIEnv *env, jobject obj) {
        if (ptr_file_id == 0) {
            ptr_file_id = env->GetFieldID(env->GetObjectClass(obj), "ptr", "J");
        }
        auto service = reinterpret_cast<yaad::BtService*>(env->GetLongField(obj, ptr_file_id));
        return service;
    }

    jobject create_state_java_obj(JNIEnv *env, lt::torrent_status& status) {
        auto clazz = env->FindClass(TORRENT_SERVICE_CLASS);
        auto methodId = env->GetStaticMethodID(clazz, "createDownloadStatus", "(IJDDJ)Lio/github/yaad/downloader_core/torrent/TorrentDownloadStatus;");
        auto obj = env->CallStaticObjectMethod(
                clazz,
                methodId,
                (int) status.progress,
                status.total_done,
                (jdouble) status.download_rate,
                (jdouble) status.upload_rate,
                status.total_wanted
                );
        return obj;
    }

    extern "C" jlong JNICALL native_add_task_link(JNIEnv *env, jobject thiz,
                                            jstring link, jstring save_at) {
        auto service = get_service(env, thiz);
        if (service == nullptr)  {
            return -1;
        }
        auto link_c_str = env->GetStringUTFChars(link, nullptr);
        auto path_c_str = env->GetStringUTFChars(save_at, nullptr);
        auto task_id = service->add_task_by_magnet_uri(link_c_str, path_c_str);
        env->ReleaseStringUTFChars(link, link_c_str);
        env->ReleaseStringUTFChars(save_at, path_c_str);
        return task_id;
    }

    extern "C"  void JNICALL native_init_service(JNIEnv *env, jobject thiz) {
        auto service = new yaad::BtService();
        auto field_id = env->GetFieldID(env->GetObjectClass(thiz), "ptr", "J");
        env->SetLongField(thiz, field_id, reinterpret_cast<jlong>(service));
    }

    extern "C" jobject JNICALL native_get_task_status(JNIEnv *env, jobject thiz,
                                                                              jlong task_id) {
        auto service = get_service(env, thiz);
        if (service == nullptr)  {
            return nullptr;
        }

        auto status = service->get_task_info(task_id);
        if (status == nullptr) {
            return nullptr;
        }
        std::ostringstream oss;
        oss << "progress: " << status->progress << " rate: " <<  status->download_rate;
        auto str = oss.str();
        LOGI("%s", str.c_str());

        return create_state_java_obj(env, *status);
    }

    extern "C" void JNICALL native_torrent_update(JNIEnv *env, jobject thiz) {
        auto service = get_service(env, thiz);
        if (service == nullptr)  {
            return;
        }
        if (update_method_id == 0) {
            update_method_id =  env->GetMethodID(env->GetObjectClass(thiz), "onTaskUpdate", "(JLio/github/yaad/downloader_core/torrent/TorrentDownloadStatus;)V");
        }
        service->update_torrent([thiz,env](task_id_t task_id, auto st){
            jobject status_obj = create_state_java_obj(env, st);
            env->CallVoidMethod(thiz,  update_method_id, task_id, status_obj);
            env->DeleteLocalRef(status_obj);
        });
    }

    extern "C" void JNICALL native_task_pause(JNIEnv *env, jobject thiz, jlong task_id) {
        auto service = get_service(env, thiz);
        if (service == nullptr)  {
            return;
        }
        service->task_pause(task_id);
    }

    extern "C" void JNICALL native_task_resume(JNIEnv *env, jobject thiz, jlong task_id) {
        auto service = get_service(env, thiz);
        if (service == nullptr)  {
            return;
        }
        service->task_resume(task_id);
    }

    extern "C" void JNICALL native_task_remove(JNIEnv *env, jobject thiz, jlong task_id) {
        auto service = get_service(env, thiz);
        if (service == nullptr)  {
            return;
        }
        service->task_remove(task_id);
    }

    static JNINativeMethod methods[] = {
            {"initService",  "()V",  (void*) native_init_service},
            {"addTaskByLink","(Ljava/lang/String;Ljava/lang/String;)J", (void*) native_add_task_link},
            {"getTaskStatus",  "(J)Lio/github/yaad/downloader_core/torrent/TorrentDownloadStatus;", (void*) native_get_task_status},
            {"torrentUpdate", "()V", (void*) native_torrent_update},
            {"taskPause", "(J)V", (void*) native_task_pause},
            {"taskResume", "(J)V", (void*) native_task_resume},
            {"taskRemove", "(J)V", (void*) native_task_remove}
    };

    int register_bt(JNIEnv* env) {
        auto clazz = env->FindClass(TORRENT_SERVICE_CLASS);
        if (!clazz) {
            return JNI_ERR;
        }
        if (env->RegisterNatives(clazz, methods, sizeof(methods)/sizeof(methods[0])) < 0) {
            return JNI_ERR;
        }
        return 0;
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

    task_id_t BtService::add_task_by_magnet_uri(const char* uri, const char* path) {
        lt::error_code ec;
        lt::add_torrent_params atp = lt::parse_magnet_uri(uri, ec);
        if (ec) {
            std::cerr << "解析 magnet 失败: " << ec.message() << "\n";
            return -1;
        }
        atp.save_path = path;  // 下载保存路径
        atp.flags |= lt::torrent_flags::seed_mode;

        return put_handle(session_->add_torrent(atp, ec));
    }

    std::unique_ptr<libtorrent::torrent_status> BtService::get_task_info(task_id_t task_id) {
        auto handle = get_handle(task_id);
        if (handle == nullptr) {
            return nullptr;
        }
        return std::make_unique<lt::torrent_status>(handle->status());
    }

    void BtService::update_torrent(std::function<void (task_id_t task,const lt::torrent_status& status)> cb) {
        session_->post_torrent_updates(); // 请求状态更新
        std::vector<lt::alert*> alerts;
        session_->pop_alerts(&alerts);
        for (lt::alert* a : alerts) {
            if (auto* sua = lt::alert_cast<lt::state_update_alert>(a)) {
                for (const auto& st : sua->status) {
                    auto h = st.handle;
                    auto task_id = get_handle_id(h);
                    if (task_id < 0) {
                        continue;
                    }
                    cb(task_id, st);
                }
            }
        }
    }

    void BtService::task_pause(task_id_t task_id) {
        auto handle = get_handle(task_id);
        if (handle == nullptr) {
            return;
        }
        handle->pause();
    }

    void BtService::task_resume(task_id_t task_id) {
        auto handle = get_handle(task_id);
        if (handle == nullptr) {
            return;
        }
        handle->resume();
    }

    void BtService::task_remove(task_id_t task_id) {
        auto handle = get_handle(task_id);
        if (handle == nullptr) {
            return;
        }
        handle->pause();
        session_->remove_torrent(*handle);
        tasks_table_.erase(task_id);
    }

}