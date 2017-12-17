//
// Created by huzongyao on 17-11-24.
//

#include "ndk-helper.h"

#ifndef ANDROIDUN7ZIP_7ZEXTRACTER_H
#define ANDROIDUN7ZIP_7ZEXTRACTER_H

jboolean extractFile(JNIEnv *env, const char *srcFile, const char *destDir, jobject callback,
                     jlong inBufSize);

jboolean extractAsset(JNIEnv *env, jobject assetsManager, const char *assetName,
                      const char *destDir, jobject callback, jlong inBufSize);

#endif //ANDROIDUN7ZIP_7ZEXTRACTER_H
