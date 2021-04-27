package com.hss01248.mediax.demo;

import android.app.Application;

import com.facebook.stetho.Stetho;
import com.hss01248.media.metadata.ExifUtil;


public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Stetho.initializeWithDefaults(this);
        ExifUtil.enableLog = true;
    }
}
