package com.hss01248.mediax.demo;

import androidx.multidex.MultiDexApplication;

import com.facebook.stetho.Stetho;
import com.hss01248.media.metadata.ExifUtil;


public class MyApp extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();

        Stetho.initializeWithDefaults(this);
        ExifUtil.enableLog = true;
    }
}
