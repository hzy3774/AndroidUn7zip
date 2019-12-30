/* 7zFile.h -- File IO
2009-11-24 : Igor Pavlov : Public domain */

#ifndef __7Z_ASSETS_H
#define __7Z_ASSETS_H


#include <stdio.h>
#include "7zTypes.h"
#include <android/asset_manager.h>

EXTERN_C_BEGIN

/* ---------- File ---------- */

typedef struct {
    AAsset *asset;
    int64_t length;
} CSzAssetFile;

WRes InAssetFile_Open(struct AAssetManager *mgr, CSzAssetFile *p, const char *name);

WRes AssetFile_Close(CSzAssetFile *p);

/* reads max(*size, remain file's size) bytes */
static SRes AssetFileInStream_Read(const ISeekInStream *pp, void *buf, size_t *size);

static SRes AssetFileInStream_Seek(const ISeekInStream *pp, Int64 *pos, ESzSeek origin);

typedef struct {
    ISeekInStream vt;
    CSzAssetFile assetFile;
} CAssetFileInStream;

void AssetFileInStream_CreateVTable(CAssetFileInStream *p);

EXTERN_C_END

#endif
