//
// Created by HZY on 2017/11/25.
// code copy from 7zMain.c
//

#include <sys/stat.h>
#include <errno.h>
#include <string.h>
#include <stdbool.h>
#include "7zFunctions.h"
#include "src/7zAlloc.h"
#include "ndk-helper.h"
#include "src/7zBuf.h"

#define PATH_MAX 4096

#define _UTF8_START(n) (0x100 - (1 << (7 - (n))))
#define _UTF8_RANGE(n) (((UInt32)1) << ((n) * 5 + 6))
#define _UTF8_HEAD(n, val) ((Byte)(_UTF8_START(n) + (val >> (6 * (n)))))
#define _UTF8_CHAR(n, val) ((Byte)(0x80 + (((val) >> (6 * (n))) & 0x3F)))

#define PERIOD_4 (4 * 365 + 1)
#define PERIOD_100 (PERIOD_4 * 25 - 1)
#define PERIOD_400 (PERIOD_100 * 4 + 1)
#define DEBUG_LOG NATIVE_LOG

static const ISzAlloc g_Alloc = {SzAlloc, SzFree};

void Print(const char *s) {
    if (DEBUG_LOG) {
        LOGD("%s", s);
    }
}

void PrintError(const char *s) {
    LOGE("%s", s);
}

static int Buf_EnsureSize(CBuf *dest, size_t size) {
    if (dest->size >= size)
        return 1;
    Buf_Free(dest, &g_Alloc);
    return Buf_Create(dest, size, &g_Alloc);
}

static size_t Utf16_To_Utf8_Calc(const UInt16 *src, const UInt16 *srcLim) {
    size_t size = 0;
    for (; ;) {
        UInt32 val;
        if (src == srcLim)
            return size;
        size++;
        val = *src++;
        if (val < 0x80)
            continue;
        if (val < _UTF8_RANGE(1)) {
            size++;
            continue;
        }
        if (val >= 0xD800 && val < 0xDC00 && src != srcLim) {
            UInt32 c2 = *src;
            if (c2 >= 0xDC00 && c2 < 0xE000) {
                src++;
                size += 3;
                continue;
            }
        }
        size += 2;
    }
}

static Byte *Utf16_To_Utf8(Byte *dest, const UInt16 *src, const UInt16 *srcLim) {
    for (; ;) {
        UInt32 val;
        if (src == srcLim)
            return dest;
        val = *src++;
        if (val < 0x80) {
            *dest++ = (char) val;
            continue;
        }
        if (val < _UTF8_RANGE(1)) {
            dest[0] = _UTF8_HEAD(1, val);
            dest[1] = _UTF8_CHAR(0, val);
            dest += 2;
            continue;
        }
        if (val >= 0xD800 && val < 0xDC00 && src != srcLim) {
            UInt32 c2 = *src;
            if (c2 >= 0xDC00 && c2 < 0xE000) {
                src++;
                val = (((val - 0xD800) << 10) | (c2 - 0xDC00)) + 0x10000;
                dest[0] = _UTF8_HEAD(3, val);
                dest[1] = _UTF8_CHAR(2, val);
                dest[2] = _UTF8_CHAR(1, val);
                dest[3] = _UTF8_CHAR(0, val);
                dest += 4;
                continue;
            }
        }
        dest[0] = _UTF8_HEAD(2, val);
        dest[1] = _UTF8_CHAR(1, val);
        dest[2] = _UTF8_CHAR(0, val);
        dest += 3;
    }
}

static SRes Utf16_To_Utf8Buf(CBuf *dest, const UInt16 *src, size_t srcLen) {
    size_t destLen = Utf16_To_Utf8_Calc(src, src + srcLen);
    destLen += 1;
    if (!Buf_EnsureSize(dest, destLen))
        return SZ_ERROR_MEM;
    *Utf16_To_Utf8(dest->data, src, src + srcLen) = 0;
    return SZ_OK;
}

SRes Utf16_To_Char(CBuf *buf, const UInt16 *s) {
    unsigned len = 0;
    for (len = 0; s[len] != 0; len++);
    return Utf16_To_Utf8Buf(buf, s, len);
}

WRes MyCreateDir(const UInt16 *name, const char *dir) {
    CBuf buf;
    WRes res;
    char temp[PATH_MAX] = {0};
    strcpy(temp, dir);
    Buf_Init(&buf);
    RINOK(Utf16_To_Char(&buf, name));
    strcat(temp, STRING_PATH_SEPARATOR);
    strcat(temp, (const char *) buf.data);
    res = mkdir((const char *) temp, 0777) == 0 ? 0 : errno;
    Buf_Free(&buf, &g_Alloc);
    return res;
}

WRes OutFile_OpenUtf16(CSzFile *p, const UInt16 *name, const char *dir) {
    CBuf buf;
    WRes res;
    Buf_Init(&buf);
    char temp[PATH_MAX] = {0};
    RINOK(Utf16_To_Char(&buf, name));
    strcpy(temp, dir);
    strcat(temp, STRING_PATH_SEPARATOR);
    strcat(temp, (const char *) buf.data);
    Print(temp);
    res = OutFile_Open(p, temp);
    Buf_Free(&buf, &g_Alloc);
    return res;
}

SRes PrintString(const UInt16 *s) {
    CBuf buf;
    SRes res;
    Buf_Init(&buf);
    res = Utf16_To_Char(&buf, s);
    if (res == SZ_OK)
        Print((const char *) buf.data);
    Buf_Free(&buf, &g_Alloc);
    return res;
}

void UInt64ToStr(UInt64 value, char *s, int numDigits) {
    char temp[32];
    int pos = 0;
    do {
        temp[pos++] = (char) ('0' + (unsigned) (value % 10));
        value /= 10;
    }
    while (value != 0);
    for (numDigits -= pos; numDigits > 0; numDigits--)
        *s++ = ' ';
    do
        *s++ = temp[--pos];
    while (pos);
    *s = '\0';
}

char *UIntToStr(char *s, unsigned value, int numDigits) {
    char temp[16];
    int pos = 0;
    do
        temp[pos++] = (char) ('0' + (value % 10));
    while (value /= 10);
    for (numDigits -= pos; numDigits > 0; numDigits--)
        *s++ = '0';
    do
        *s++ = temp[--pos];
    while (pos);
    *s = '\0';
    return s;
}

static void UIntToStr_2(char *s, unsigned value) {
    s[0] = (char) ('0' + (value / 10));
    s[1] = (char) ('0' + (value % 10));
}

void ConvertFileTimeToString(const CNtfsFileTime *nt, char *s) {
    unsigned year, mon, hour, min, sec;
    Byte ms[] = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    unsigned t;
    UInt32 v;
    UInt64 v64 = nt->Low | ((UInt64) nt->High << 32);
    v64 /= 10000000;
    sec = (unsigned) (v64 % 60);
    v64 /= 60;
    min = (unsigned) (v64 % 60);
    v64 /= 60;
    hour = (unsigned) (v64 % 24);
    v64 /= 24;
    v = (UInt32) v64;
    year = (unsigned) (1601 + v / PERIOD_400 * 400);
    v %= PERIOD_400;
    t = v / PERIOD_100;
    if (t == 4) t = 3;
    year += t * 100;
    v -= t * PERIOD_100;
    t = v / PERIOD_4;
    if (t == 25) t = 24;
    year += t * 4;
    v -= t * PERIOD_4;
    t = v / 365;
    if (t == 4) t = 3;
    year += t;
    v -= t * 365;
    if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0))
        ms[1] = 29;
    for (mon = 0; ; mon++) {
        unsigned d = ms[mon];
        if (v < d)
            break;
        v -= d;
    }
    s = UIntToStr(s, year, 4);
    *s++ = '-';
    UIntToStr_2(s, mon + 1);
    s[2] = '-';
    s += 3;
    UIntToStr_2(s, (unsigned) v + 1);
    s[2] = ' ';
    s += 3;
    UIntToStr_2(s, hour);
    s[2] = ':';
    s += 3;
    UIntToStr_2(s, min);
    s[2] = ':';
    s += 3;
    UIntToStr_2(s, sec);
    s[2] = 0;
}

void GetAttribString(UInt32 wa, BoolInt isDir, char *s) {
    s[0] = (char) (((wa & (1 << 4)) != 0 || isDir) ? 'D' : '.');
    s[1] = 0;
}

void CallJavaVoidMethod(JNIEnv *env, jobject obj, jmethodID id) {
    if (id != NULL) {
        (*env)->CallVoidMethod(env, obj, id);
    }
}

void CallJavaIntMethod(JNIEnv *env, jobject obj, jmethodID id, jint param) {
    if (id != NULL) {
        (*env)->CallVoidMethod(env, obj, id, param);
    }
}

void CallJavaStringMethod(JNIEnv *env, jobject obj, jmethodID id, char *param) {
    if (id != NULL) {
        jstring jparam = (*env)->NewStringUTF(env, param);
        (*env)->CallVoidMethod(env, obj, id, jparam);
        (*env)->DeleteLocalRef(env, jparam);
    }
}

void CallJavaIntStringMethod(JNIEnv *env, jobject obj, jmethodID id, int param1, char *param2) {
    if (id != NULL) {
        jstring jparam = (*env)->NewStringUTF(env, param2);
        (*env)->CallVoidMethod(env, obj, id, param1, jparam);
        (*env)->DeleteLocalRef(env, jparam);
    }
}

void CallJavaStringLongMethod(JNIEnv *env, jobject obj, jmethodID id, char* param1, jlong param2) {
    if (id != NULL) {
        jstring jparam = (*env)->NewStringUTF(env, param1);
        (*env)->CallVoidMethod(env, obj, id, jparam, param2);
        (*env)->DeleteLocalRef(env, jparam);
    }
}
