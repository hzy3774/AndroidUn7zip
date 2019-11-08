package com.hzy.un7zip;

import com.hzy.lib7z.IExtractCallback;

public abstract class UnzipCallback implements IExtractCallback {
    @Override
    public void onStart() {
    }

    @Override
    public void onGetFileNum(int fileNum) {
    }

    @Override
    public void onProgress(String name, long size) {
    }

    @Override
    public void onError(int errorCode, String message) {
    }

    @Override
    public void onSucceed() {
    }
}
