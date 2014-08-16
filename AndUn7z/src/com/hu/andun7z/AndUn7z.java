package com.hu.andun7z;

import java.io.File;

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
	
	//JNI interface
	private static native int un7zip(String filePath, String outPath);
	
	static {
		System.loadLibrary("un7z");
	}
}
