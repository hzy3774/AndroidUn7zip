//
// Created by huzongyao on 17-11-24.
//

#include <stdint.h>
#include <android/asset_manager_jni.h>
#include "7zExtracter.h"
#include "src/7zTypes.h"
#include "src/7z.h"
#include "src/7zFile.h"
#include "src/7zAlloc.h"
#include "src/7zAssetFile.h"
#include "src/7zCrc.h"
#include "7zFunctions.h"

#define OPTION_DETAIL 0x01
#define OPTION_TEST 0x02
#define OPTION_OUTPUT 0x04
#define OPTION_EXTRACT (OPTION_TEST|OPTION_OUTPUT)

static const ISzAlloc g_Alloc = {SzAlloc, SzFree};

static SRes
extractStream(JNIEnv *env, ISeekInStream *seekStream, const char *destDir,
              const int options, jobject callback, size_t inBufSize) {

    jmethodID onGetFileNum = NULL;
    jmethodID onError = NULL;
    jmethodID onProgress = NULL;
    if (callback != NULL) {
        jclass callbackClass = (*env)->GetObjectClass(env, callback);
        onGetFileNum = (*env)->GetMethodID(env, callbackClass, "onGetFileNum", "(I)V");
        onError = (*env)->GetMethodID(env, callbackClass, "onError", "(ILjava/lang/String;)V");
        onProgress = (*env)->GetMethodID(env, callbackClass, "onProgress", "(Ljava/lang/String;J)V");
    }

    ISzAlloc allocImp = g_Alloc;
    ISzAlloc allocTempImp = g_Alloc;
    CLookToRead2 lookStream;
    CSzArEx db;
    SRes res;
    UInt16 *temp = NULL;
    size_t tempSize = 0;

    LOGD("Stream In Buffer Size:[0X%lX]", inBufSize);
    LookToRead2_CreateVTable(&lookStream, False);
    lookStream.buf = NULL;
    res = SZ_OK;
    lookStream.buf = ISzAlloc_Alloc(&allocImp, inBufSize);
    if (!lookStream.buf)
        res = SZ_ERROR_MEM;
    else {
        lookStream.bufSize = inBufSize;
        lookStream.realStream = seekStream;
        LookToRead2_Init(&lookStream);
    }

    CrcGenerateTable();
    SzArEx_Init(&db);
    if (res == SZ_OK) {
        res = SzArEx_Open(&db, &lookStream.vt, &allocImp, &allocTempImp);
    }
    if (res == SZ_OK) {
        UInt32 i;
        /*
        if you need cache, use these 3 variables.
        if you use external function, you can make these variable as static.
        */
        UInt32 blockIndex = 0xFFFFFFFF; /* it can have any value before first call (if outBuffer = 0) */
        Byte *outBuffer = 0; /* it must be 0 before first call for each new archive. */
        size_t outBufferSize = 0;  /* it can have any value before first call (if outBuffer = 0) */
        CBuf fileNameBuf;
        Buf_Init(&fileNameBuf);

        CallJavaIntMethod(env, callback, onGetFileNum, db.NumFiles);
        for (i = 0; i < db.NumFiles; i++) {
            size_t offset = 0;
            size_t outSizeProcessed = 0;
            size_t len;
            unsigned isDir = (unsigned) SzArEx_IsDir(&db, i);
            len = SzArEx_GetFileNameUtf16(&db, i, NULL);
            if (len > tempSize) {
                SzFree(NULL, temp);
                tempSize = len;
                temp = (UInt16 *) SzAlloc(NULL, tempSize * sizeof(temp[0]));
                if (!temp) {
                    res = SZ_ERROR_MEM;
                    break;
                }
            }
            SzArEx_GetFileNameUtf16(&db, i, temp);
            res = Utf16_To_Char(&fileNameBuf, temp);
            if (res != SZ_OK) {
                break;
            }
            UInt64 fileSize = SzArEx_GetFileSize(&db, i);
            CallJavaStringLongMethod(env, callback, onProgress,
                                     (char *) fileNameBuf.data, fileSize);
            if (options & OPTION_DETAIL) {
                char attr[8], file_size[32], file_time[32];
                GetAttribString(SzBitWithVals_Check(&db.Attribs, i)
                                ? db.Attribs.Vals[i] : 0, isDir, attr);
                UInt64ToStr(fileSize, file_size, 10);
                if (SzBitWithVals_Check(&db.MTime, i))
                    ConvertFileTimeToString(&db.MTime.Vals[i], file_time);
                else {
                    size_t j;
                    for (j = 0; j < 19; j++)
                        file_time[j] = ' ';
                    file_time[j] = '\0';
                }
            }
            if (options & OPTION_TEST) {
                if (!isDir) {
                    res = SzArEx_Extract(&db, &lookStream.vt, i, &blockIndex, &outBuffer,
                                         &outBufferSize, &offset, &outSizeProcessed,
                                         &allocImp, &allocTempImp);
                    if (res != SZ_OK)
                        break;
                }
                if (options & OPTION_OUTPUT) {
                    CSzFile outFile;
                    size_t processedSize;
                    size_t j;
                    UInt16 *name = temp;
                    const UInt16 *destPath = (const UInt16 *) name;

                    for (j = 0; name[j] != 0; j++) {
                        if (name[j] == '/') {
                            name[j] = 0;
                            MyCreateDir(name, destDir);
                            name[j] = CHAR_PATH_SEPARATOR;
                        }
                    }
                    if (isDir) {
                        MyCreateDir(name, destDir);
                        continue;
                    } else if (OutFile_OpenUtf16(&outFile, destPath, destDir)) {
                        PrintError("can not open output file");
                        res = SZ_ERROR_FAIL;
                        break;
                    }
                    processedSize = outSizeProcessed;
                    if (File_Write(&outFile, outBuffer + offset, &processedSize) != 0 ||
                        processedSize != outSizeProcessed) {
                        PrintError("can not write output file");
                        res = SZ_ERROR_FAIL;
                        break;
                    }
                    if (File_Close(&outFile)) {
                        PrintError("can not close output file");
                        res = SZ_ERROR_FAIL;
                        break;
                    }
                }
            }
        }
        Buf_Free(&fileNameBuf, &g_Alloc);
        ISzAlloc_Free(&allocImp, outBuffer);
    }
    SzFree(NULL, temp);
    SzArEx_Free(&db, &allocImp);
    ISzAlloc_Free(&allocImp, lookStream.buf);
    if (res != SZ_OK) {
        CallJavaIntStringMethod(env, callback, onError, SZ_ERROR_ARCHIVE, "Stream Extract Error");
    }
    return res;
}

/**
 * extract all from 7z
 */
jint extractFile(JNIEnv *env, const char *srcFile, const char *destDir, jobject callback,
                     jlong inBufSize) {
    jmethodID onStart = NULL;
    jmethodID onError = NULL;
    jmethodID onSucceed = NULL;
    if (callback != NULL) {
        jclass  callbackClass = (*env)->GetObjectClass(env, callback);
        onStart = (*env)->GetMethodID(env, callbackClass, "onStart", "()V");
        onError = (*env)->GetMethodID(env, callbackClass, "onError", "(ILjava/lang/String;)V");
        onSucceed = (*env)->GetMethodID(env, callbackClass, "onSucceed", "()V");
    }
    CFileInStream archiveStream;
    CallJavaVoidMethod(env, callback, onStart);
    if (InFile_Open(&archiveStream.file, srcFile)) {
        CallJavaIntStringMethod(env, callback, onError, SZ_ERROR_ARCHIVE, "Input File Open Error");
        return SZ_ERROR_ARCHIVE;
    }
    FileInStream_CreateVTable(&archiveStream);
    SRes res = extractStream(env, &archiveStream.vt, destDir, OPTION_EXTRACT, callback,
                             (size_t) inBufSize);
    File_Close(&archiveStream.file);
    if (res == SZ_OK) {
        CallJavaVoidMethod(env, callback, onSucceed);
    }
    return res;
}

/**
 * extract from assets
 */
jint extractAsset(JNIEnv *env, jobject assetsManager, const char *assetName,
                      const char *destDir, jobject callback, jlong inBufSize) {
    jmethodID onStart = NULL;
    jmethodID onError = NULL;
    jmethodID onSucceed = NULL;
    if (callback != NULL) {
        jclass  callbackClass = (*env)->GetObjectClass(env, callback);
        onStart = (*env)->GetMethodID(env, callbackClass, "onStart", "()V");
        onError = (*env)->GetMethodID(env, callbackClass, "onError", "(ILjava/lang/String;)V");
        onSucceed = (*env)->GetMethodID(env, callbackClass, "onSucceed", "()V");
    }
    CAssetFileInStream archiveStream;
    CallJavaVoidMethod(env, callback, onStart);
    AAssetManager *mgr = AAssetManager_fromJava(env, assetsManager);
    if (InAssetFile_Open(mgr, &archiveStream.assetFile, assetName)) {
        CallJavaIntStringMethod(env, callback, onError, SZ_ERROR_ARCHIVE, "Asset Open Error");
        return SZ_ERROR_ARCHIVE;
    }
    AssetFileInStream_CreateVTable(&archiveStream);
    SRes res = extractStream(env, &archiveStream.vt, destDir, OPTION_EXTRACT, callback,
                             (size_t) inBufSize);
    AssetFile_Close(&archiveStream.assetFile);
    if (res == SZ_OK) {
        CallJavaVoidMethod(env, callback, onSucceed);
    }
    return res;
}

