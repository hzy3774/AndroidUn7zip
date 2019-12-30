#include "ndk-helper.h"
#include "7zVersion.h"
#include "7zExtractor.h"
#include "un7zApi.h"


JNIEXPORT jstring JNICALL
FUNC(nGetLzmaVersion)(JNIEnv *env, jclass type) {
    return env->NewStringUTF(MY_VERSION_COPYRIGHT_DATE);
}


JNIEXPORT jint JNICALL
FUNC(nExtractFile)(JNIEnv *env, jclass type, jstring filePath_,
                   jstring outPath_, jobject callback, jlong inBufSize) {
    const char *filePath = env->GetStringUTFChars(filePath_, nullptr);
    const char *outPath = env->GetStringUTFChars(outPath_, nullptr);

    jint res = extractFile(env, filePath, outPath, callback, inBufSize);

    env->ReleaseStringUTFChars(filePath_, filePath);
    env->ReleaseStringUTFChars(outPath_, outPath);

    return res;
}


JNIEXPORT jint JNICALL
FUNC(nExtractAsset)(JNIEnv *env, jclass type, jobject assetManager,
                    jstring fileName_, jstring outPath_, jobject callback,
                    jlong inBufSize) {
    const char *fileName = env->GetStringUTFChars(fileName_, nullptr);
    const char *outPath = env->GetStringUTFChars(outPath_, nullptr);

    jint res = extractAsset(env, assetManager, fileName, outPath, callback, inBufSize);

    env->ReleaseStringUTFChars(fileName_, fileName);
    env->ReleaseStringUTFChars(outPath_, outPath);

    return res;
}