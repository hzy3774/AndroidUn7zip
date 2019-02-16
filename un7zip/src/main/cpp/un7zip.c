#include "ndk-helper.h"
#include "src/7zVersion.h"
#include "7zExtracter.h"

#define FUNC(f) Java_com_hzy_lib7z_Z7Extractor_##f


JNIEXPORT jstring JNICALL
FUNC(nGetLzmaVersion)(JNIEnv *env, jclass type) {
    return (*env)->NewStringUTF(env, MY_VERSION_COPYRIGHT_DATE);
}

JNIEXPORT jint JNICALL
FUNC(nExtractFile)(JNIEnv *env, jclass type, jstring filePath_,
                   jstring outPath_, jobject callback, jlong inBufSize) {
    const char *filePath = (*env)->GetStringUTFChars(env, filePath_, 0);
    const char *outPath = (*env)->GetStringUTFChars(env, outPath_, 0);
    jint res = extractFile(env, filePath, outPath, callback, inBufSize);
    (*env)->ReleaseStringUTFChars(env, filePath_, filePath);
    (*env)->ReleaseStringUTFChars(env, outPath_, outPath);
    return res;
}

JNIEXPORT jint JNICALL
FUNC(nExtractAsset)(JNIEnv *env, jclass type, jobject assetManager,
                    jstring fileName_, jstring outPath_, jobject callback,
                    jlong inBufSize) {
    const char *fileName = (*env)->GetStringUTFChars(env, fileName_, 0);
    const char *outPath = (*env)->GetStringUTFChars(env, outPath_, 0);
    jint res = extractAsset(env, assetManager, fileName, outPath, callback, inBufSize);
    (*env)->ReleaseStringUTFChars(env, fileName_, fileName);
    (*env)->ReleaseStringUTFChars(env, outPath_, outPath);
    return res;
}