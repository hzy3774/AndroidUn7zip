/* 7zMain.c - Test application for 7z Decoder
 2008-11-23 : Igor Pavlov : Public domain */

#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#ifdef _WIN32
#include <direct.h>
#else
#include <sys/stat.h>
#endif

#include "../JniWrapper.h"

#include "7zCrc.h"
#include "7zFile.h"
#include "7zVersion.h"

#include "7zAlloc.h"
#include "7zExtract.h"
#include "7zIn.h"

int MY_CDECL extract7z(const char* srcFile, const char* dstPath)
{
	CFileInStream archiveStream;
	CLookToRead lookStream;
	CSzArEx db;
	SRes res;
	ISzAlloc allocImp;
	ISzAlloc allocTempImp;
	char outPath[1024] = { 0 };

	LOGD("7z ANSI-C Decoder " MY_VERSION_COPYRIGHT_DATE );

	if (InFile_Open(&archiveStream.file, srcFile)) {//open 7z file
		LOGE("can not open input file");
		return 1;
	}

	FileInStream_CreateVTable(&archiveStream);
	LookToRead_CreateVTable(&lookStream, False);

	lookStream.realStream = &archiveStream.s;
	LookToRead_Init(&lookStream);

	allocImp.Alloc = SzAlloc;
	allocImp.Free = SzFree;

	allocTempImp.Alloc = SzAllocTemp;
	allocTempImp.Free = SzFreeTemp;

	CrcGenerateTable();

	SzArEx_Init(&db);
	res = SzArEx_Open(&db, &lookStream.s, &allocImp, &allocTempImp);

	if(res == SZ_OK)
	{
		Int32 i;

		UInt32 blockIndex = 0xFFFFFFFF; /* it can have any value before first call (if outBuffer = 0) */
		Byte *outBuffer = 0; /* it must be 0 before first call for each new archive. */
		size_t outBufferSize = 0; /* it can have any value before first call (if outBuffer = 0) */

		LOGD("Total file/directory count[%d]\n", db.db.NumFiles);
		for (i = db.db.NumFiles - 1; i >= 0; i--) {
			size_t offset;
			size_t outSizeProcessed;
			CSzFileItem *f = db.db.Files + i;

			strcpy(outPath, dstPath);
			strcat(outPath, "/");
			strcat(outPath, f->Name);

			if (f->IsDir) {	//dir
				LOGD("dir [%s]\n", outPath);

#ifdef _WIN32
				mkdir(outPath);
#else
				mkdir(outPath, 0777);
#endif
				continue;
			}else{	//file
				LOGD("file [%s]\n", outPath);
				res = SzAr_Extract(&db, &lookStream.s, i, &blockIndex,
						&outBuffer, &outBufferSize, &offset, &outSizeProcessed,
						&allocImp, &allocTempImp);
				if (res != SZ_OK){
					break;
				}else{
					CSzFile outFile;
					size_t processedSize;
					if (OutFile_Open(&outFile, outPath)) {
						LOGE("can not open output file");
						res = SZ_ERROR_FAIL;
						break;
					}
					processedSize = outSizeProcessed;
					if (File_Write(&outFile, outBuffer + offset, &processedSize)
							!= 0 || processedSize != outSizeProcessed) {
						LOGE("can not write output file");
						res = SZ_ERROR_FAIL;
						break;
					}
					if (File_Close(&outFile)) {
						LOGE("can not close output file");
						res = SZ_ERROR_FAIL;
						break;
					}
				}
			}
		}
		IAlloc_Free(&allocImp, outBuffer);
	}
	SzArEx_Free(&db, &allocImp);

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



