/* 7zMain.c - Test application for 7z Decoder
 2010-10-28 : Igor Pavlov : Public domain */

#include <jni.h>
#include <android/log.h>

#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <errno.h>

#include "7z.h"
#include "7zAlloc.h"
#include "7zCrc.h"
#include "7zFile.h"
#include "7zVersion.h"

#define LOG_TAG "jniLog"
#undef LOG

#ifdef DEBUG
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL,LOG_TAG,__VA_ARGS__)
#else
#define LOGD(...) do{}while(0)
#define LOGI(...) do{}while(0)
#define LOGW(...) do{}while(0)
#define LOGE(...) do{}while(0)
#define LOGF(...) do{}while(0)
#endif


#define PATH_MAX 2048

static ISzAlloc g_Alloc = { SzAlloc, SzFree };

static int Buf_EnsureSize(CBuf *dest, size_t size) {
	if (dest->size >= size)
		return 1;
	Buf_Free(dest, &g_Alloc);
	return Buf_Create(dest, size, &g_Alloc);
}

static Byte kUtf8Limits[5] = { 0xC0, 0xE0, 0xF0, 0xF8, 0xFC };

static Bool Utf16_To_Utf8(Byte *dest, size_t *destLen, const UInt16 *src,
		size_t srcLen) {
	size_t destPos = 0, srcPos = 0;
	for (;;) {
		unsigned numAdds;
		UInt32 value;
		if (srcPos == srcLen) {
			*destLen = destPos;
			return True;
		}
		value = src[srcPos++];
		if (value < 0x80) {
			if (dest)
				dest[destPos] = (char) value;
			destPos++;
			continue;
		}
		if (value >= 0xD800 && value < 0xE000) {
			UInt32 c2;
			if (value >= 0xDC00 || srcPos == srcLen)
				break;
			c2 = src[srcPos++];
			if (c2 < 0xDC00 || c2 >= 0xE000)
				break;
			value = (((value - 0xD800) << 10) | (c2 - 0xDC00)) + 0x10000;
		}
		for (numAdds = 1; numAdds < 5; numAdds++)
			if (value < (((UInt32) 1) << (numAdds * 5 + 6)))
				break;
		if (dest)
			dest[destPos] = (char) (kUtf8Limits[numAdds - 1]
					+ (value >> (6 * numAdds)));
		destPos++;
		do {
			numAdds--;
			if (dest)
				dest[destPos] = (char) (0x80
						+ ((value >> (6 * numAdds)) & 0x3F));
			destPos++;
		} while (numAdds != 0);
	}
	*destLen = destPos;
	return False;
}

static SRes Utf16_To_Utf8Buf(CBuf *dest, const UInt16 *src, size_t srcLen) {
	size_t destLen = 0;
	Bool res;
	Utf16_To_Utf8(NULL, &destLen, src, srcLen);
	destLen += 1;
	if (!Buf_EnsureSize(dest, destLen))
		return SZ_ERROR_MEM;
	res = Utf16_To_Utf8(dest->data, &destLen, src, srcLen);
	dest->data[destLen] = 0;
	return res ? SZ_OK : SZ_ERROR_FAIL;
}

static SRes Utf16_To_Char(CBuf *buf, const UInt16 *s, int fileMode) {
	int len = 0;
	for (len = 0; s[len] != '\0'; len++)
		;
	return Utf16_To_Utf8Buf(buf, s, len);
}

static WRes MyCreateDir(const char* root, const UInt16 *name) {
	CBuf buf;
	WRes res;
	char temp[PATH_MAX] = { 0 };

	Buf_Init(&buf);
	RINOK(Utf16_To_Char(&buf, name, 1));
	strcpy(temp, root);
	strcat(temp, STRING_PATH_SEPARATOR);
	strcat(temp, (const char *) buf.data);

	if (access(temp, 0) == -1) {
		LOGD("Dir : %s", temp);
		res = mkdir(temp, 0777) == 0 ? 0 : errno;
	}else{
		res = 0;
	}
	Buf_Free(&buf, &g_Alloc);
	return res;
}

static WRes OutFile_OpenUtf16(CSzFile *p, const char* root, const UInt16 *name) {
	CBuf buf;
	WRes res;
	char temp[PATH_MAX] = {0};

	Buf_Init(&buf);
	RINOK(Utf16_To_Char(&buf, name, 1));
	strcpy(temp, root);
	strcat(temp, STRING_PATH_SEPARATOR);
	strcat(temp, (const char *) buf.data);

	LOGD("File: %s", temp);
	res = OutFile_Open(p, temp);
	Buf_Free(&buf, &g_Alloc);
	return res;
}

void PrintError(char *sz) {
	LOGD("ERROR: %s", sz);
}

int extract7z(const char* inFile, const char* outPath) {
	CFileInStream archiveStream;
	CLookToRead lookStream;
	CSzArEx db;
	SRes res;
	ISzAlloc allocImp;
	ISzAlloc allocTempImp;
	UInt16 *temp = NULL;
	size_t tempSize = 0;
	size_t pathLen = 0;

	LOGD("7z ANSI-C Decoder " MY_VERSION_COPYRIGHT_DATE);

	allocImp.Alloc = SzAlloc;
	allocImp.Free = SzFree;

	allocTempImp.Alloc = SzAllocTemp;
	allocTempImp.Free = SzFreeTemp;

	if (InFile_Open(&archiveStream.file, inFile)) {
		PrintError("can not open input file");
		return 1;
	}

	FileInStream_CreateVTable(&archiveStream);
	LookToRead_CreateVTable(&lookStream, False);

	lookStream.realStream = &archiveStream.s;
	LookToRead_Init(&lookStream);

	CrcGenerateTable();

	SzArEx_Init(&db);
	res = SzArEx_Open(&db, &lookStream.s, &allocImp, &allocTempImp);

	if (res == SZ_OK)
	{
		UInt32 i;
		UInt32 j;
		/*
		 if you need cache, use these 3 variables.
		 if you use external function, you can make these variable as static.
		 */
		UInt32 blockIndex = 0xFFFFFFFF; /* it can have any value before first call (if outBuffer = 0) */
		Byte *outBuffer = 0; /* it must be 0 before first call for each new archive. */
		size_t outBufferSize = 0; /* it can have any value before first call (if outBuffer = 0) */

		for (i = 0; i < db.db.NumFiles; i++) {
			size_t offset = 0;
			size_t outSizeProcessed = 0;
			const CSzFileItem *f = db.db.Files + i;

			size_t len = SzArEx_GetFileNameUtf16(&db, i, NULL);

			if (len > tempSize) {
				SzFree(NULL, temp);
				tempSize = len;
				temp = (UInt16 *) SzAlloc(NULL, tempSize * sizeof(temp[0]));
				if (temp == 0) {
					res = SZ_ERROR_MEM;
					break;
				}
			}

			SzArEx_GetFileNameUtf16(&db, i, temp);
			if (res != SZ_OK)
				break;
			if (!f->IsDir){
				res = SzArEx_Extract(&db, &lookStream.s, i, &blockIndex,
						&outBuffer, &outBufferSize, &offset, &outSizeProcessed,
						&allocImp, &allocTempImp);
				if (res != SZ_OK)
					break;
			}

			CSzFile outFile;
			size_t processedSize;
			size_t j;
			UInt16 *name = (UInt16 *) temp;
			const UInt16 *destPath = (const UInt16 *) name;
			for (j = 0; name[j] != 0; j++) {
				if (name[j] == '/') {
					name[j] = 0;
					MyCreateDir(outPath, name);
					name[j] = CHAR_PATH_SEPARATOR;
				}
			}
			if (f->IsDir) {
				MyCreateDir(outPath, destPath);
				continue;
			} else if (OutFile_OpenUtf16(&outFile, outPath, destPath)) {
				PrintError("can not open output file");
				res = SZ_ERROR_FAIL;
				break;
			}
			processedSize = outSizeProcessed;
			if (File_Write(&outFile, outBuffer + offset, &processedSize) != 0
					|| processedSize != outSizeProcessed) {
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
		IAlloc_Free(&allocImp, outBuffer);
	}
	SzArEx_Free(&db, &allocImp);
	SzFree(NULL, temp);

	File_Close(&archiveStream.file);
	if (res == SZ_OK)
	{
		LOGD("Everything is Ok");
		return 0;
	}
	if (res == SZ_ERROR_UNSUPPORTED
		)
		LOGE("decoder doesn't support this archive");
	else if (res == SZ_ERROR_MEM
		)
		LOGE("can not allocate memory");
	else if (res == SZ_ERROR_CRC
		)
		LOGE("CRC error");
	else
		LOGE("ERROR #%d", res);
	return 1;
}



