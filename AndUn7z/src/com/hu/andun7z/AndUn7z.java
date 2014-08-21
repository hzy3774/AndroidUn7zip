package com.hu.andun7z;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import android.content.Context;

public class AndUn7z {
	
	public static boolean extract7z(String filePath, String outPath)
	{
		File outDir = new File(outPath);
		if(!outDir.exists() || !outDir.isDirectory())
		{
			outDir.mkdirs();
		}
		return (AndUn7z.un7zip(filePath, outPath) == 1);
	}
	
	/**
	 * Extract from assets
	 * @param context
	 * @param assetPath
	 * @param outPath
	 * @return
	 * @throws Exception
	 */
	public static boolean extractAssets(Context context, String assetPath, String outPath) 
	{
		File outDir = new File(outPath);
		if(!outDir.exists() || !outDir.isDirectory())
		{
			outDir.mkdirs();
		}
		
		String tempPath = outPath + File.separator + ".temp";
		try {
			copyFromAssets(context, assetPath, tempPath);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		boolean ret = (AndUn7z.un7zip(tempPath, outPath) == 1);
		new File(tempPath).delete();
		
		return ret;
	}
	
	/**
	 * Copy asset to temp
	 * @param context
	 * @param assetPath
	 * @param tempPath
	 * @throws Exception
	 */
	private static void copyFromAssets(Context context, String assetPath, String tempPath) 
			throws Exception
	{
		InputStream inputStream = context.getAssets().open(assetPath);
		FileOutputStream fileOutputStream = new FileOutputStream(tempPath);
		int length = -1;
		byte[] buffer = new byte[0x400000];
		while ((length = inputStream.read(buffer)) != -1) {
			fileOutputStream.write(buffer, 0, length);
		}
		fileOutputStream.flush();
		fileOutputStream.close();
		inputStream.close();
	}
	
	//JNI interface
	private static native int un7zip(String filePath, String outPath);
	
	static {
		System.loadLibrary("un7z");
	}
}
