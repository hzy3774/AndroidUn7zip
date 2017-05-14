#include <jni.h>
#include <android/log.h>

#include "src/Types.h"

#ifdef __cplusplus
extern "C" {
#endif

#define LOG_TAG "jniLog"
#undef LOG

#ifdef NDK_DEBUG
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL,LOG_TAG,__VA_ARGS__)
#else
#define LOGD(...) do{}while(0)
#define LOGI(...) do{}while(0)
#define LOGW(...) do{}while(0)
#define LOGE(...) do{}while(0)
#define LOGF(...) do{}while(0)
#endif
#include <android/asset_manager.h>


int extract7z(const char *srcFile, const char *dstPath);

int extract7zFromAssets(JNIEnv* env, jobject jAssetsManager,  const char *srcFile, const char *dstPath);

/*
 * Class:     com_hzy_lib7z_Un7Zip
 * Method:    un7zip
 * Signature: (Ljava/lang/String;Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_hzy_lib7z_Un7Zip_un7zip
        (JNIEnv *env, jclass thiz, jstring filePath, jstring outPath) {
    const char *cfilePath = (const char *) env->GetStringUTFChars(filePath, NULL);
    const char *coutPath = (const char *) env->GetStringUTFChars(outPath, NULL);
    LOGD("start extract filePath[%s], outPath[%s]", cfilePath, coutPath);
    jint ret = extract7z(cfilePath, coutPath);
    LOGD("end extract");
    env->ReleaseStringUTFChars(filePath, cfilePath);
    env->ReleaseStringUTFChars(outPath, coutPath);
    return ret;
}

/*
 * Class:     com_hzy_lib7z_Un7Zip
 * Method:    un7zip
 * Signature: (Ljava/lang/String;Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_hzy_lib7z_Un7Zip_un7zipFromAssets
        (JNIEnv *env, jclass thiz, jobject assetsManager, jstring filePath, jstring outPath) {
    const char *cfilePath = (const char *) env->GetStringUTFChars(filePath, NULL);
    const char *coutPath = (const char *) env->GetStringUTFChars(outPath, NULL);
    LOGD("start extract filePath[%s] from assets, outPath[%s]", cfilePath, coutPath);
    jint ret = extract7zFromAssets(env, assetsManager, cfilePath, coutPath);
    LOGD("end extract assets file");
    env->ReleaseStringUTFChars(filePath, cfilePath);
    env->ReleaseStringUTFChars(outPath, coutPath);
    return ret;
}

#ifdef __cplusplus
}
#endif
