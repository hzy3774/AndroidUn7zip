package com.hzy.lib7z;

/**
 * Created by huzongyao on 17-11-24.
 */

public abstract class ExtractCallback implements IExtractCallback {

    @Override
    public void onStart() {
    }

    @Override
    public void onGetFileNum(int fileNum) {
    }

    @Override
    public void onSucceed() {
    }

    public static ExtractCallback EMPTY_CALLBACK = new ExtractCallback() {

        @Override
        public void onProgress(String name, long size) {
        }

        @Override
        public void onError(int errorCode, String message) {
        }
    };
}
