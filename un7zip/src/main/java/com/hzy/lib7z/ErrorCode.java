package com.hzy.lib7z;

/**
 * Created by huzongyao on 17-11-24.
 */

public class ErrorCode {

    public static final int SZ_OK = 0;

    public static final int SZ_ERROR_DATA = 1;
    public static final int SZ_ERROR_MEM = 2;
    public static final int SZ_ERROR_CRC = 3;
    public static final int SZ_ERROR_UNSUPPORTED = 4;
    public static final int SZ_ERROR_PARAM = 5;
    public static final int SZ_ERROR_INPUT_EOF = 6;
    public static final int SZ_ERROR_OUTPUT_EOF = 7;
    public static final int SZ_ERROR_READ = 8;
    public static final int SZ_ERROR_WRITE = 9;
    public static final int SZ_ERROR_PROGRESS = 10;
    public static final int SZ_ERROR_FAIL = 11;
    public static final int SZ_ERROR_THREAD = 12;

    public static final int SZ_ERROR_ARCHIVE = 16;
    public static final int SZ_ERROR_NO_ARCHIVE = 17;

    public static final int ERROR_CODE_PATH_ERROR = 999;
}
