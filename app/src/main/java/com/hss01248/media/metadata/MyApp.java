package com.hss01248.media.metadata;

import android.app.Application;

import com.facebook.stetho.Stetho;
import com.hss01248.media.mymediastore.SafUtil;

public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SafUtil.context = this;
        Stetho.initializeWithDefaults(this);
    }
}
