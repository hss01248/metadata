package com.hss01248.mediax.demo;

import android.app.Application;
import android.content.Context;

import com.facebook.stetho.Stetho;
import com.hss01248.media.metadata.ExifUtil;

import me.weishu.reflection.Reflection;


public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Stetho.initializeWithDefaults(this);
        ExifUtil.enableLog = true;
        ExifUtil.init(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
       ExifUtil.attachBaseContext(base,BuildConfig.DEBUG);
    }
}
