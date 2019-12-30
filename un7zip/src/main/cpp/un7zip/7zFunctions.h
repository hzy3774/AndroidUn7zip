//
// Created by HZY on 2017/11/25.
//

#include "7zTypes.h"
#include "7zFile.h"
#include "7z.h"
#include "ndk-helper.h"
#include "7zBuf.h"

#ifndef ANDROIDUN7ZIP_7ZFUNCTIONS_H
#define ANDROIDUN7ZIP_7ZFUNCTIONS_H

#ifdef __cplusplus
extern "C" {
#endif

void Print(const char *s);

void PrintError(const char *s);

SRes Utf16_To_Char(CBuf *buf, const UInt16 *s);

WRes MyCreateDir(const UInt16 *name, const char *dir);

WRes OutFile_OpenUtf16(CSzFile *p, const UInt16 *name, const char *dir);

SRes PrintString(const UInt16 *s);

void UInt64ToStr(UInt64 value, char *s, int numDigits);

char *UIntToStr(char *s, unsigned value, int numDigits);

void ConvertFileTimeToString(const CNtfsFileTime *nt, char *s);

void GetAttribString(UInt32 wa, BoolInt isDir, char *s);

void CallJavaVoidMethod(JNIEnv *env, jobject obj, jmethodID id);

void CallJavaIntMethod(JNIEnv *env, jobject obj, jmethodID id, jint param);

void CallJavaStringMethod(JNIEnv *env, jobject obj, jmethodID id, const char *param);

void
CallJavaIntStringMethod(JNIEnv *env, jobject obj, jmethodID id, int param1, const char *param2);

void
CallJavaStringLongMethod(JNIEnv *env, jobject obj, jmethodID id, const char *param1, jlong param2);

#ifdef __cplusplus
}
#endif

#endif //ANDROIDUN7ZIP_7ZFUNCTIONS_H
