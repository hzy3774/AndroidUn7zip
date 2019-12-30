//
// Created by huzongyao on 2019/12/30.
//

#ifndef ANDROIDUN7ZIP_UN7ZAPI_H
#define ANDROIDUN7ZIP_UN7ZAPI_H

#define FUNC(f) Java_com_hzy_lib7z_Z7Extractor_##f

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jstring JNICALL
FUNC(nGetLzmaVersion)(JNIEnv *env, jclass type);

JNIEXPORT jint JNICALL
FUNC(nExtractFile)(JNIEnv *env, jclass type, jstring filePath_,
                   jstring outPath_, jobject callback, jlong inBufSize);

JNIEXPORT jint JNICALL
FUNC(nExtractAsset)(JNIEnv *env, jclass type, jobject assetManager,
                    jstring fileName_, jstring outPath_, jobject callback,
                    jlong inBufSize);

#ifdef __cplusplus
}
#endif

#endif //ANDROIDUN7ZIP_UN7ZAPI_H
