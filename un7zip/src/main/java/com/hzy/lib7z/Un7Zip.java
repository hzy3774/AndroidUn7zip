package com.hzy.lib7z;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class Un7Zip {

    public static boolean extract7z(String filePath, String outPath) {
        File outDir = new File(outPath);
        if (!outDir.exists() || !outDir.isDirectory()) {
            outDir.mkdirs();
        }
        return (Un7Zip.un7zip(filePath, outPath) == 0);
    }

    /**
     * Extract 7z file from assets
     *
     * @param context
     * @param assetPath
     * @param outPath
     * @return
     * @throws Exception
     */
    public static boolean extract7zFromAssets(Context context, String assetPath, String outPath) {
        File outDir = new File(outPath);
        if (!outDir.exists() || !outDir.isDirectory()) {
            outDir.mkdirs();
        }
        return (Un7Zip.un7zipFromAssets(context.getAssets(), assetPath, outPath) == 0);
    }

    //JNI interface
    private static native int un7zip(String filePath, String outPath);
    private static native int un7zipFromAssets(AssetManager assetManager, String filePath, String outPath);

    static {
        System.loadLibrary("un7zip");
    }
}
