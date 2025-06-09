#include <jni.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/mman.h>
#include <android/log.h>
#include <libtorrent/session.hpp>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "JNI", __VA_ARGS__)

const long page_size = sysconf(_SC_PAGESIZE);


jint native_openFile(JNIEnv* env, jobject thiz, jstring path) {
    const char* c_path = env->GetStringUTFChars(path, nullptr);
    int fd = open(c_path, O_RDWR | O_CREAT, 0666);
    env->ReleaseStringUTFChars(path, c_path);
    return fd;
}


jint native_resizeFile(JNIEnv* env, jobject thiz, jint fd, jlong size) {
    if (fd < 0) return -1;
    return ftruncate(fd, size);
}


jlong native_mmapFile(JNIEnv* env, jobject thiz, jint fd, jlong size) {
    if (fd < 0 || size <= 0) return 0;

    // 对 size 向上对齐
    jlong aligned_size = (size + page_size - 1) & ~(page_size - 1);

    // 确保文件大小 >= 对齐后的 size
    if (ftruncate(fd, aligned_size) != 0) {
        return 0; // ftruncate 失败
    }

    // 映射文件
    void* ptr = mmap(nullptr, static_cast<size_t>(aligned_size), PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
    if (ptr == MAP_FAILED) {
        return 0;
    }

    return reinterpret_cast<jlong>(ptr);
}


void native_writeByte(JNIEnv* env, jobject thiz, jlong ptr, jlong offset, jbyte value) {
    if (ptr == 0 || offset < 0) return;
    uint8_t* base = reinterpret_cast<uint8_t*>(ptr);
    base[offset] = static_cast<uint8_t>(value);
}

void native_writeByteArray(JNIEnv* env, jobject thiz, jlong ptr, jlong offset, jbyteArray array, jint start, jint len) {
    if (ptr == 0 || offset < 0) return;
    uint8_t* base = reinterpret_cast<uint8_t*>(ptr);
    env->GetByteArrayRegion(array, start, len, reinterpret_cast<jbyte*>(base + offset));
}

void native_closeFile(JNIEnv* env, jobject thiz, jint fd) {
    if (fd >= 0) {
        close(fd);
    }
}

void native_msync(JNIEnv* env, jobject thiz, jlong ptr, jlong size) {
    if (ptr == 0 || size <= 0) return;

    auto addr = static_cast<uintptr_t>(ptr);
    uintptr_t aligned_addr = addr & ~(page_size - 1);  // 向下对齐地址

    size_t offset = addr - aligned_addr;
    size_t aligned_size = static_cast<size_t>(size + offset + page_size - 1) & ~(page_size - 1); // 向上对齐 size + 偏移

    void* aligned_ptr = reinterpret_cast<void*>(aligned_addr);
    msync(aligned_ptr, aligned_size, MS_SYNC);
}

void native_munmap(JNIEnv* env, jobject thiz, jlong ptr, jlong size) {
    if (ptr == 0 || size <= 0) return;

    uintptr_t addr = static_cast<uintptr_t>(ptr);
    uintptr_t aligned_addr = addr & ~(page_size - 1);  // 向下对齐地址

    size_t offset = addr - aligned_addr;
    size_t aligned_size = static_cast<size_t>(size + offset + page_size - 1) & ~(page_size - 1); // 向上对齐 size + 偏移

    void* aligned_ptr = reinterpret_cast<void*>(aligned_addr);
    munmap(aligned_ptr, aligned_size);
}

jint native_getSystemPageSize(JNIEnv* env, jobject thiz) {
    return static_cast<jint>(page_size > 0 ? page_size : 4096); // fallback to 4096 if error
}

static JNINativeMethod methods[] = {
        {"openFile",  "(Ljava/lang/String;)I",  (void*)native_openFile},
        {"resizeFile","(IJ)I",                  (void*)native_resizeFile},
        {"mmapFile",  "(IJ)J",                  (void*)native_mmapFile},
        {"writeByte", "(JJB)V",                 (void*)native_writeByte},
        {"writeByteArray", "(JJ[BII)V", (void*)native_writeByteArray},
        {"closeFile", "(I)V",                   (void*)native_closeFile},
        {"msync",     "(JJ)V",                  (void*)native_msync},
        {"munmap", "(JJ)V", (void*)native_munmap},
        {"getSystemPageSize", "()I", (void*)native_getSystemPageSize},

};

jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    jclass clazz = env->FindClass("io/github/yaad/downloader_core/NativeBridge");
    if (!clazz) {
        LOGI("Failed to find class NativeBridge");
        return JNI_ERR;
    }

    if (env->RegisterNatives(clazz, methods, sizeof(methods)/sizeof(methods[0])) < 0) {
        LOGI("Failed to register native methods");
        return JNI_ERR;
    }
    libtorrent::session session;
    return JNI_VERSION_1_6;
}
