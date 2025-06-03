#include <jni.h>
#include <android/log.h>
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/avutil.h>
#include <libavutil/mathematics.h>
#include <libavutil/time.h>

#define LOG_TAG "FFmpegMerge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/* Helper function to throw Java exception */
void throwJavaException(JNIEnv *env, const char *message) {
    jclass exceptionClass = (*env)->FindClass(env, "java/lang/RuntimeException");
    if (exceptionClass != NULL) {
        (*env)->ThrowNew(env, exceptionClass, message);
    }
}

int isVideoFile(AVFormatContext *ctx) {
    unsigned int i;
    for (i = 0; i < ctx->nb_streams; i++) {
        if (ctx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            return 1;
        }
    }
    return 0;
}

jint mergeAV(JNIEnv *env, jobject thiz, jstring file1, jstring file2, jstring out) {
    const char *file1Path = (*env)->GetStringUTFChars(env, file1, NULL);
    const char *file2Path = (*env)->GetStringUTFChars(env, file2, NULL);
    const char *outputPath = (*env)->GetStringUTFChars(env, out, NULL);

    LOGI("Starting audio-video merge: %s + %s -> %s", file1Path, file2Path, outputPath);

    AVFormatContext *ctx1 = NULL;
    AVFormatContext *ctx2 = NULL;
    AVFormatContext *outCtx = NULL;
    AVStream *videoStream = NULL;
    AVStream *audioStream = NULL;
    AVStream *inAudioStream = NULL;
    int videoStreamIndex = -1;
    int audioStreamIndex = -1;
    int64_t videoDuration = 0;
    AVPacket *packet = NULL;
    int64_t videoStartPts = AV_NOPTS_VALUE;
    int64_t audioStartPts = AV_NOPTS_VALUE;
    jint ret = 0;

    /* Open first file */
    int ret_code = avformat_open_input(&ctx1, file1Path, NULL, NULL);
    if (ret_code < 0) {
        char errbuf[AV_ERROR_MAX_STRING_SIZE];
        av_strerror(ret_code, errbuf, AV_ERROR_MAX_STRING_SIZE);
        LOGE("Failed to open first input file: %s, Error: %s", file1Path, errbuf);
        
        /* Try to get file format information */
        AVProbeData probe_data = {0};
        probe_data.filename = file1Path;
        probe_data.buf = NULL;
        probe_data.buf_size = 0;
        
        const AVInputFormat *fmt = av_probe_input_format(&probe_data, 1);
        if (fmt) {
            LOGE("Detected format: %s, Long name: %s", fmt->name, fmt->long_name);
        } else {
            LOGE("Could not detect file format");
        }
        
        throwJavaException(env, "Failed to open first input file");
        ret = -1;
        goto end;
    }
    if (avformat_find_stream_info(ctx1, NULL) < 0) {
        LOGE("Failed to find stream info for first file: %s", file1Path);
        throwJavaException(env, "Failed to find stream info for first file");
        ret = -2;
        goto end;
    }

    /* Open second file */
    if (avformat_open_input(&ctx2, file2Path, NULL, NULL) < 0) {
        LOGE("Failed to open second input file: %s", file2Path);
        throwJavaException(env, "Failed to open second input file");
        ret = -3;
        goto end;
    }
    if (avformat_find_stream_info(ctx2, NULL) < 0) {
        LOGE("Failed to find stream info for second file: %s", file2Path);
        throwJavaException(env, "Failed to find stream info for second file");
        ret = -4;
        goto end;
    }

    /* Determine which is video file and which is audio file */
    AVFormatContext *videoCtx, *audioCtx;
    const char *videoPath, *audioPath;
    
    if (isVideoFile(ctx1)) {
        videoCtx = ctx1;
        audioCtx = ctx2;
        videoPath = file1Path;
        audioPath = file2Path;
    } else {
        videoCtx = ctx2;
        audioCtx = ctx1;
        videoPath = file2Path;
        audioPath = file1Path;
    }

    /* Allocate packet */
    packet = av_packet_alloc();
    if (!packet) {
        LOGE("Failed to allocate AVPacket memory");
        throwJavaException(env, "Failed to allocate memory for packet");
        ret = -5;
        goto end;
    }

    /* Create output context */
    avformat_alloc_output_context2(&outCtx, NULL, NULL, outputPath);
    if (!outCtx) {
        LOGE("Failed to create output context: %s", outputPath);
        throwJavaException(env, "Failed to create output context");
        ret = -6;
        goto end;
    }

    /* Find video and audio stream indices */
    for (int i = 0; i < videoCtx->nb_streams; i++) {
        if (videoCtx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            videoStreamIndex = i;
            videoDuration = videoCtx->streams[i]->duration;
            break;
        }
    }

    for (int i = 0; i < audioCtx->nb_streams; i++) {
        if (audioCtx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            audioStreamIndex = i;
            break;
        }
    }

    if (videoStreamIndex == -1 || audioStreamIndex == -1) {
        LOGE("Video or audio stream not found - Video stream index: %d, Audio stream index: %d", videoStreamIndex, audioStreamIndex);
        throwJavaException(env, "Required video or audio stream not found in input files");
        ret = -7;
        goto end;
    }

    LOGI("Found video stream index: %d, audio stream index: %d", videoStreamIndex, audioStreamIndex);

    /* Add video streams */
    for (int i = 0; i < videoCtx->nb_streams; i++) {
        AVStream *inStream = videoCtx->streams[i];
        if (inStream->codecpar->codec_type != AVMEDIA_TYPE_AUDIO) {
            AVStream *outStream = avformat_new_stream(outCtx, NULL);
            if (!outStream) {
                LOGE("Failed to create output video stream");
                throwJavaException(env, "Failed to create output video stream");
                ret = -8;
                goto end;
            }
            if (avcodec_parameters_copy(outStream->codecpar, inStream->codecpar) < 0) {
                LOGE("Failed to copy video stream parameters");
                throwJavaException(env, "Failed to copy video stream parameters");
                ret = -9;
                goto end;
            }
            if (inStream->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
                videoStream = outStream;
                /* Copy timebase */
                outStream->time_base = inStream->time_base;
            }
        }
    }

    /* Add audio stream */
    inAudioStream = audioCtx->streams[audioStreamIndex];
    audioStream = avformat_new_stream(outCtx, NULL);
    if (!audioStream) {
        LOGE("Failed to create output audio stream");
        throwJavaException(env, "Failed to create output audio stream");
        ret = -10;
        goto end;
    }
    if (avcodec_parameters_copy(audioStream->codecpar, inAudioStream->codecpar) < 0) {
        LOGE("Failed to copy audio stream parameters");
        throwJavaException(env, "Failed to copy audio stream parameters");
        ret = -11;
        goto end;
    }
    audioStream->time_base = inAudioStream->time_base;

    /* Open output file */
    if (!(outCtx->oformat->flags & AVFMT_NOFILE)) {
        if (avio_open(&outCtx->pb, outputPath, AVIO_FLAG_WRITE) < 0) {
            LOGE("Failed to open output file: %s", outputPath);
            throwJavaException(env, "Failed to open output file for writing");
            ret = -12;
            goto end;
        }
    }

    /* Write file header */
    if (avformat_write_header(outCtx, NULL) < 0) {
        LOGE("Failed to write file header");
        throwJavaException(env, "Failed to write output file header");
        ret = -13;
        goto end;
    }
    
    /* Write video data first */
    while (av_read_frame(videoCtx, packet) >= 0) {
        if (packet->stream_index == videoStreamIndex) {
            /* Record PTS of the first video frame */
            if (videoStartPts == AV_NOPTS_VALUE) {
                videoStartPts = packet->pts;
            }
            
            /* Adjust PTS and DTS */
            packet->pts -= videoStartPts;
            if (packet->dts != AV_NOPTS_VALUE) {
                packet->dts -= videoStartPts;
            }
            
            packet->stream_index = videoStream->index;
            av_write_frame(outCtx, packet);
        }
        av_packet_unref(packet);
    }

    /* Write audio data */
    while (av_read_frame(audioCtx, packet) >= 0) {
        if (packet->stream_index == audioStreamIndex) {
            /* Record PTS of the first audio frame */
            if (audioStartPts == AV_NOPTS_VALUE) {
                audioStartPts = packet->pts;
            }
            
            /* Adjust audio PTS and DTS */
            packet->pts -= audioStartPts;
            if (packet->dts != AV_NOPTS_VALUE) {
                packet->dts -= audioStartPts;
            }
            
            /* Ensure audio duration doesn't exceed video duration */
            if (packet->pts * av_q2d(audioStream->time_base) > 
                videoDuration * av_q2d(videoCtx->streams[videoStreamIndex]->time_base)) {
                break;
            }
            
            packet->stream_index = audioStream->index;
            av_write_frame(outCtx, packet);
        }
        av_packet_unref(packet);
    }

    /* Write file trailer */
    if (av_write_trailer(outCtx) < 0) {
        LOGE("Failed to write file trailer");
        ret = -14;
    } else if (ret == 0) {
        LOGI("Audio-video merge completed successfully: %s", outputPath);
    }

end:
    if (ret != 0) {
        LOGE("Audio-video merge failed with error code: %d", ret);
    }
    /* Cleanup resources */
    if (packet) av_packet_free(&packet);
    if (ctx1) avformat_close_input(&ctx1);
    if (ctx2) avformat_close_input(&ctx2);
    if (outCtx) {
        if (!(outCtx->oformat->flags & AVFMT_NOFILE)) {
            avio_closep(&outCtx->pb);
        }
        avformat_free_context(outCtx);
    }

    (*env)->ReleaseStringUTFChars(env, file1, file1Path);
    (*env)->ReleaseStringUTFChars(env, file2, file2Path);
    (*env)->ReleaseStringUTFChars(env, out, outputPath);
    return ret;
}

static JNINativeMethod methods[] = {
    {"mergeAV", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I", (void*)mergeAV}
};

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if ((*vm)->GetEnv(vm, (void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        LOGE("Failed to get JNI environment");
        return JNI_ERR;
    }

    jclass clazz = (*env)->FindClass(env, "io/github/yearsyan/yaad/media/FFmpegTools");
    if (clazz == NULL) {
        LOGE("Failed to find FFmpegTools class");
        return JNI_ERR;
    }

    if ((*env)->RegisterNatives(env, clazz, methods, sizeof(methods) / sizeof(methods[0])) < 0) {
        LOGE("Failed to register native methods");
        return JNI_ERR;
    }

    LOGI("FFmpegTools JNI library loaded successfully");
    return JNI_VERSION_1_6;
}