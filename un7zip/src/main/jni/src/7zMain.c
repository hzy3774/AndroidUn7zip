/* 7zMain.c - Test application for 7z Decoder
2017-04-05 : Igor Pavlov : Public domain */

#include <stdio.h>
#include "CpuArch.h"

#include "NdkHelper.h"
#include "7z.h"
#include "7zAlloc.h"
#include "7zBuf.h"
#include "7zCrc.h"
#include "7zFile.h"
#include "7zVersion.h"
#include <android/asset_manager_jni.h>
#include "7zAssetFile.h"
#include <sys/stat.h>
#include <errno.h>
#include <string.h>

#define kInputBufSize ((size_t)1 << 18)
#define PATH_MAX 2048

static const ISzAlloc g_Alloc = {SzAlloc, SzFree};

static void Print(const char *s) {
    LOGD(s);
}

static void PrintError(char *s) {
    LOGE(s);
}

static int Buf_EnsureSize(CBuf *dest, size_t size) {
    if (dest->size >= size)
        return 1;
    Buf_Free(dest, &g_Alloc);
    return Buf_Create(dest, size, &g_Alloc);
}

#define _UTF8_START(n) (0x100 - (1 << (7 - (n))))
#define _UTF8_RANGE(n) (((UInt32)1) << ((n) * 5 + 6))
#define _UTF8_HEAD(n, val) ((Byte)(_UTF8_START(n) + (val >> (6 * (n)))))
#define _UTF8_CHAR(n, val) ((Byte)(0x80 + (((val) >> (6 * (n))) & 0x3F)))

static size_t Utf16_To_Utf8_Calc(const UInt16 *src, const UInt16 *srcLim) {
    size_t size = 0;
    for (;;) {
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
    for (;;) {
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

static SRes Utf16_To_Char(CBuf *buf, const UInt16 *s) {
    unsigned len = 0;
    for (len = 0; s[len] != 0; len++);
    return Utf16_To_Utf8Buf(buf, s, len);
}

static WRes MyCreateDir(const UInt16 *name, const char *dir) {
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

static WRes OutFile_OpenUtf16(CSzFile *p, const UInt16 *name, const char *dir) {
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

static void UInt64ToStr(UInt64 value, char *s, int numDigits) {
    char temp[32];
    int pos = 0;
    do {
        temp[pos++] = (char) ('0' + (unsigned) (value % 10));
        value /= 10;
    } while (value != 0);
    for (numDigits -= pos; numDigits > 0; numDigits--)
        *s++ = ' ';
    do
        *s++ = temp[--pos];
    while (pos);
    *s = '\0';
}

int extract7z(const char *srcFile, const char *destDir) {
    ISzAlloc allocImp = g_Alloc;
    ISzAlloc allocTempImp = g_Alloc;
    CFileInStream archiveStream;
    CLookToRead2 lookStream;
    CSzArEx db;
    SRes res;
    UInt16 *temp = NULL;
    size_t tempSize = 0;

    Print("\n7z Decoder " MY_VERSION_CPU " : " MY_COPYRIGHT_DATE "\n\n");

    if (InFile_Open(&archiveStream.file, srcFile)) {
        PrintError("can not open input file");
        return 1;
    }

    FileInStream_CreateVTable(&archiveStream);
    LookToRead2_CreateVTable(&lookStream, False);
    lookStream.buf = NULL;

    res = SZ_OK;
    lookStream.buf = ISzAlloc_Alloc(&allocImp, kInputBufSize);
    if (!lookStream.buf)
        res = SZ_ERROR_MEM;
    else {
        lookStream.bufSize = kInputBufSize;
        lookStream.realStream = &archiveStream.vt;
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

        for (i = 0; i < db.NumFiles; i++) {
            size_t offset = 0;
            size_t outSizeProcessed = 0;
            size_t len;
            unsigned isDir = SzArEx_IsDir(&db, i);
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
            if (!isDir) {
                res = SzArEx_Extract(&db, &lookStream.vt, i,
                                     &blockIndex, &outBuffer, &outBufferSize,
                                     &offset, &outSizeProcessed,
                                     &allocImp, &allocTempImp);
                if (res != SZ_OK)
                    break;
            }

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
                MyCreateDir(destPath, destDir);
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
        ISzAlloc_Free(&allocImp, outBuffer);
    }

    SzFree(NULL, temp);
    SzArEx_Free(&db, &allocImp);
    ISzAlloc_Free(&allocImp, lookStream.buf);
    File_Close(&archiveStream.file);

    if (res == SZ_OK) {
        Print("\nEverything is Ok\n");
        return 0;
    }

    if (res == SZ_ERROR_UNSUPPORTED)
        PrintError("decoder doesn't support this archive");
    else if (res == SZ_ERROR_MEM)
        PrintError("can not allocate memory");
    else if (res == SZ_ERROR_CRC)
        PrintError("CRC error");
    else {
        char s[32];
        UInt64ToStr(res, s, 0);
        PrintError(s);
    }
    return 1;
}

int extract7zFromAssets(JNIEnv *env, jobject jAssetsManager,
                        const char *inFile, const char *destDir) {
    ISzAlloc allocImp = g_Alloc;
    ISzAlloc allocTempImp = g_Alloc;
    CAssetFileInStream archiveStream;
    CLookToRead2 lookStream;
    CSzArEx db;
    SRes res;
    UInt16 *temp = NULL;
    size_t tempSize = 0;

    Print("\n7z Decoder " MY_VERSION_CPU " : " MY_COPYRIGHT_DATE "\n\n");

    AAssetManager *mgr = AAssetManager_fromJava(env, jAssetsManager);
    if (InAssetFile_Open(mgr, &archiveStream.assetFile, inFile)) {
        PrintError("can not open asset file");
        return 1;
    }

    AssetFileInStream_CreateVTable(&archiveStream);
    LookToRead2_CreateVTable(&lookStream, False);
    lookStream.buf = NULL;
    res = SZ_OK;
    lookStream.buf = ISzAlloc_Alloc(&allocImp, kInputBufSize);
    if (!lookStream.buf)
        res = SZ_ERROR_MEM;
    else {
        lookStream.bufSize = kInputBufSize;
        lookStream.realStream = &archiveStream.vt;
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

        for (i = 0; i < db.NumFiles; i++) {
            size_t offset = 0;
            size_t outSizeProcessed = 0;
            size_t len;
            unsigned isDir = SzArEx_IsDir(&db, i);
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
            if (!isDir) {
                res = SzArEx_Extract(&db, &lookStream.vt, i,
                                     &blockIndex, &outBuffer, &outBufferSize,
                                     &offset, &outSizeProcessed,
                                     &allocImp, &allocTempImp);
                if (res != SZ_OK)
                    break;
            }

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
                MyCreateDir(destPath, destDir);
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
        ISzAlloc_Free(&allocImp, outBuffer);
    }
    SzFree(NULL, temp);
    SzArEx_Free(&db, &allocImp);
    ISzAlloc_Free(&allocImp, lookStream.buf);
    AssetFile_Close(&archiveStream.assetFile);
    if (res == SZ_OK) {
        Print("\nEverything is Ok\n");
        return 0;
    }
    if (res == SZ_ERROR_UNSUPPORTED)
        PrintError("decoder doesn't support this archive");
    else if (res == SZ_ERROR_MEM)
        PrintError("can not allocate memory");
    else if (res == SZ_ERROR_CRC)
        PrintError("CRC error");
    else {
        char s[32];
        UInt64ToStr(res, s, 0);
        PrintError(s);
    }
    return 1;
}
