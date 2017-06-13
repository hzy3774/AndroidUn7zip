#include "src/7zTypes.h"
#include "src/NdkHelper.h"

#ifdef __cplusplus
extern "C" {
#endif

int extract7z(const char *srcFile, const char *destDir);
int extract7zFromAssets(JNIEnv* env, jobject jAssetsManager, const char* inFile, const char* destDir);

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
