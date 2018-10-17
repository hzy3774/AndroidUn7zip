package com.hzy.lib7z;

import android.content.res.AssetManager;
import android.text.TextUtils;

import java.io.File;

/**
 * Created by huzongyao on 17-11-24.
 */

public class Z7Extractor {

    public static final long DEFAULT_IN_BUF_SIZE = 0x1000000;

    /**
     * Get the Lzma version name
     *
     * @return Lzma version name
     */
    public static String getLzmaVersion() {
        return nGetLzmaVersion();
    }

    /**
     * Extract every thing from a 7z file to some place
     *
     * @param filePath in file
     * @param outPath  output path
     * @param callback callback
     * @return status
     */
    public static int extractFile(String filePath, String outPath,
                                      IExtractCallback callback) {
        File inputFile = new File(filePath);
        if (TextUtils.isEmpty(filePath) || !inputFile.exists() ||
                TextUtils.isEmpty(outPath) || !prepareOutPath(outPath)) {
            if (callback != null) {
                callback.onError(ErrorCode.ERROR_CODE_PATH_ERROR, "File Path Error!");
            }
            return ErrorCode.ERROR_CODE_PATH_ERROR;
        }
        return nExtractFile(filePath, outPath, callback, DEFAULT_IN_BUF_SIZE);
    }

    /**
     * extract some stream from assets
     *
     * @param assetManager assetManager
     * @param fileName     fileName
     * @param outPath      out Path
     * @param callback     callback
     * @return status
     */
    public static int extractAsset(AssetManager assetManager, String fileName,
                                       String outPath, IExtractCallback callback) {
        if (TextUtils.isEmpty(fileName) || TextUtils.isEmpty(outPath) || !prepareOutPath(outPath)) {
            if (callback != null) {
                callback.onError(ErrorCode.ERROR_CODE_PATH_ERROR, "File Path Error!");
            }
            return ErrorCode.ERROR_CODE_PATH_ERROR;
        }
        return nExtractAsset(assetManager, fileName, outPath, callback, DEFAULT_IN_BUF_SIZE);
    }

    /**
     * make sure out path exists
     *
     * @param outPath out path
     * @return status
     */
    private static boolean prepareOutPath(String outPath) {
        File outDir = new File(outPath);
        if (!outDir.exists()) {
            if (outDir.mkdirs())
                return true;
        }
        return outDir.exists() && outDir.isDirectory();
    }

    private static native int nExtractFile(String filePath, String outPath,
                                               IExtractCallback callback, long inBufSize);

    private static native int nExtractAsset(AssetManager assetManager,
                                                String fileName, String outPath,
                                                IExtractCallback callback, long inBufSize);

    private static native String nGetLzmaVersion();

    static {
        System.loadLibrary("un7zip");
    }
}
