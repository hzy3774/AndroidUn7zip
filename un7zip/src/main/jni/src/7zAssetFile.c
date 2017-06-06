/* 7zFile.c -- File IO
2009-11-24 : Igor Pavlov : Public domain */

#include "7zAssetFile.h"

#ifndef USE_WINDOWS_FILE

#ifndef UNDER_CE
#endif

#else

/*
   ReadFile and WriteFile functions in Windows have BUG:
   If you Read or Write 64MB or more (probably min_failure_size = 64MB - 32KB + 1)
   from/to Network file, it returns ERROR_NO_SYSTEM_RESOURCES
   (Insufficient system resources exist to complete the requested service).
   Probably in some version of Windows there are problems with other sizes:
   for 32 MB (maybe also for 16 MB).
   And message can be "Network connection was lost"
*/

#endif

WRes InAssetFile_Open(struct AAssetManager *mgr, CSzAssetFile *p, const char *name){
    AAsset *asset = AAssetManager_open(mgr, name, AASSET_MODE_UNKNOWN);
    p->asset = asset;
    p->length = AAsset_getLength64(asset);
    return 0;
}

WRes AssetFile_Close(CSzAssetFile *p)
{
  if (p->asset != NULL)
  {
    AAsset_close(p->asset);
    p->asset = NULL;
  }
  return 0;
}

WRes AssetFile_Read(CSzAssetFile *p, void *data, size_t *size)
{
  size_t originalSize = *size;
  if (originalSize == 0)
    return 0;
  *size = AAsset_read(p->asset, data, originalSize);
  if (*size == originalSize)
    return 0;
  return -1;
}

WRes AssetFile_Seek(CSzAssetFile *p, Int64 *pos, ESzSeek origin)
{
  int moveMethod;
  int res;
  switch (origin)
  {
    case SZ_SEEK_SET: moveMethod = SEEK_SET; break;
    case SZ_SEEK_CUR: moveMethod = SEEK_CUR; break;
    case SZ_SEEK_END: moveMethod = SEEK_END; break;
    default: return 1;
  }
  *pos = AAsset_seek(p->asset, *pos, moveMethod);
  res = (*pos == -1 ? -1 : 0);
  return res;
}

static SRes AssetFileInStream_Read(void *pp, void *buf, size_t *size)
{
  CAssetFileInStream *p = (CAssetFileInStream *)pp;
  return (AssetFile_Read(&p->assetFile, buf, size) == 0) ? SZ_OK : SZ_ERROR_READ;
}

static SRes AssetFileInStream_Seek(void *pp, Int64 *pos, ESzSeek origin)
{
  CAssetFileInStream *p = (CAssetFileInStream *)pp;
  return AssetFile_Seek(&p->assetFile, pos, origin);
}

void AssetFileInStream_CreateVTable(CAssetFileInStream *p)
{
  p->vt.Read = AssetFileInStream_Read;
  p->vt.Seek = AssetFileInStream_Seek;
}
