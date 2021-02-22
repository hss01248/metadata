package com.hss01248.media.mymediastore;

import android.os.Handler;
import android.os.Looper;

import com.hss01248.media.mymediastore.bean.BaseMediaFolderInfo;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class DefaultScanFolderCallback implements ScanFolderCallback{

    public List<BaseMediaFolderInfo> getInfos() {
        return infos;
    }
    Handler handler;

    public DefaultScanFolderCallback() {
        handler = new Handler(Looper.getMainLooper());
    }

    List<BaseMediaFolderInfo> infos = new ArrayList<>();
    long lastUpdate = 0;
    @Override
    public void onComplete() {

    }

    @Override
    public void onFromDB(List<BaseMediaFolderInfo> folderInfos) {
        if(folderInfos != null){
            handler.post(new Runnable() {
                @Override
                public void run() {
                    infos.clear();
                    infos.addAll(folderInfos);
                    notifyDataSetChanged();
                }
            });

        }
    }

    @Override
    public void onScanEachFolder(List<BaseMediaFolderInfo> folderInfos) {
        for (BaseMediaFolderInfo folderInfo : folderInfos) {
            if(!infos.contains(folderInfo)){
                infos.add(folderInfo);
                //排序?
            }else {
                //更新:
                infos.set(infos.indexOf(folderInfo),folderInfo);
            }
        }
        try {
            Collections.sort(infos, new Comparator<BaseMediaFolderInfo>() {
                @Override
                public int compare(BaseMediaFolderInfo o1, BaseMediaFolderInfo o2) {
                    return (o2.fileSize > o1.fileSize) ? 1: -1;
                }
            });
        }catch (Throwable throwable){
            throwable.printStackTrace();
        }



        if(System.currentTimeMillis() - lastUpdate > 3000){
            lastUpdate = System.currentTimeMillis();

        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });

    }

    protected abstract void notifyDataSetChanged() ;

    @Override
    public void onScanFinished(List<BaseMediaFolderInfo> folderInfos) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                infos.clear();
                if(folderInfos != null){
                    infos.addAll(folderInfos);
                }
                notifyDataSetChanged();
            }
        });

    }
}
