package com.hzy.un7zip;

import android.app.Application;

import com.blankj.utilcode.util.Utils;
import com.getkeepsafe.relinker.ReLinker;
import com.hzy.lib7z.Z7Extractor;

public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Z7Extractor.init(libName ->
                ReLinker.loadLibrary(MainApplication.this, libName));
        Utils.init(this);
    }
}
