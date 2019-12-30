//
// Created by huzongyao on 17-11-24.
//

#include "ndk-helper.h"

#ifndef ANDROIDUN7ZIP_7ZEXTRACTOR_H
#define ANDROIDUN7ZIP_7ZEXTRACTOR_H

#ifdef __cplusplus
extern "C" {
#endif

jint extractFile(JNIEnv *env, const char *srcFile, const char *destDir, jobject callback,
                 jlong inBufSize);

jint extractAsset(JNIEnv *env, jobject assetsManager, const char *assetName,
                  const char *destDir, jobject callback, jlong inBufSize);

#ifdef __cplusplus
}
#endif

#endif //ANDROIDUN7ZIP_7ZEXTRACTER_H
