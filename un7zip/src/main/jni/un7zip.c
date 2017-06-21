#include <jni.h>
#include "src/7zVersion.h"

int extract7z(const char *srcFile, const char *destDir);

int
extract7zFromAssets(JNIEnv *env, jobject jAssetsManager, const char *inFile, const char *destDir);

JNIEXPORT jstring JNICALL
Java_com_hzy_lib7z_Un7Zip_getLzmaVersion(JNIEnv *env, jclass type) {
    return (*env)->NewStringUTF(env, MY_VERSION);
}

JNIEXPORT jint JNICALL
Java_com_hzy_lib7z_Un7Zip_un7zip(JNIEnv *env, jclass type, jstring filePath_, jstring outPath_) {
    const char *filePath = (*env)->GetStringUTFChars(env, filePath_, 0);
    const char *outPath = (*env)->GetStringUTFChars(env, outPath_, 0);
    jint ret = extract7z(filePath, outPath);
    (*env)->ReleaseStringUTFChars(env, filePath_, filePath);
    (*env)->ReleaseStringUTFChars(env, outPath_, outPath);
    return ret;
}

JNIEXPORT jint JNICALL
Java_com_hzy_lib7z_Un7Zip_un7zipFromAssets(JNIEnv *env, jclass type, jobject assetManager,
                                           jstring filePath_, jstring outPath_) {
    const char *filePath = (*env)->GetStringUTFChars(env, filePath_, 0);
    const char *outPath = (*env)->GetStringUTFChars(env, outPath_, 0);
    jint ret = extract7zFromAssets(env, assetManager, filePath, outPath);
    (*env)->ReleaseStringUTFChars(env, filePath_, filePath);
    (*env)->ReleaseStringUTFChars(env, outPath_, outPath);
    return ret;
}