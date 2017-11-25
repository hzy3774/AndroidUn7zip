package com.hzy.lib7z;

import android.content.res.AssetManager;
import android.text.TextUtils;

import java.io.File;

/**
 * Created by huzongyao on 17-11-24.
 */

public class Z7Extractor {

    public static final long DEFAULT_IN_BUF_SIZE = 0x200000;

    public static String getLzmaVersion() {
        return nGetLzmaVersion();
    }

    public static boolean extractFile(String filePath, String outPath, ExtractCallback callback) {
        callback = callback == null ? ExtractCallback.EMPTY_CALLBACK : callback;
        File inputFile = new File(filePath);
        if (TextUtils.isEmpty(filePath) || !inputFile.exists() ||
                TextUtils.isEmpty(outPath) || !prepareOutPath(outPath)) {
            callback.onError(ErrorCode.ERROR_CODE_PATH_ERROR, "File Path Error!");
            return false;
        }
        return nExtractFile(filePath, outPath, callback, DEFAULT_IN_BUF_SIZE);
    }

    public static boolean extractAsset(AssetManager assetManager, String fileName,
                                       String outPath, ExtractCallback callback) {
        callback = callback == null ? ExtractCallback.EMPTY_CALLBACK : callback;
        if (TextUtils.isEmpty(fileName) || TextUtils.isEmpty(outPath) || !prepareOutPath(outPath)) {
            callback.onError(ErrorCode.ERROR_CODE_PATH_ERROR, "File Path Error!");
            return false;
        }
        return nExtractAsset(assetManager, fileName, outPath, callback, DEFAULT_IN_BUF_SIZE);
    }

    private static boolean prepareOutPath(String outPath) {
        File outDir = new File(outPath);
        if (!outDir.exists()) {
            if (outDir.mkdirs())
                return true;
        }
        return outDir.exists() && outDir.isDirectory();
    }

    private static native boolean nExtractFile(String filePath, String outPath,
                                               ExtractCallback callback, long inBufSize);

    private static native boolean nExtractAsset(AssetManager assetManager,
                                                String fileName, String outPath,
                                                ExtractCallback callback, long inBufSize);

    private static native String nGetLzmaVersion();

    static {
        System.loadLibrary("un7zip");
    }
}
