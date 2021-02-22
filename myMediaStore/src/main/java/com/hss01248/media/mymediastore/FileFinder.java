package com.hss01248.media.mymediastore;

import android.os.Environment;
import android.util.Log;


import com.hss01248.media.mymediastore.bean.BaseMediaFolderInfo;
import com.hss01248.media.mymediastore.bean.BaseMediaInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Observer;


public class FileFinder {

    public static void listAllAlbum(final Observer<List<BaseMediaFolderInfo>> observer) {
        start = System.currentTimeMillis();
        getAlbums(Environment.getExternalStorageDirectory(), observer);
    }
    static long start ;
    public   static ExecutorService executorService = Executors.newFixedThreadPool(5);

    static AtomicInteger countGet = new AtomicInteger(0);


    private static void getAlbums(final File dir, final Observer<List<BaseMediaFolderInfo>> observer) {
        Log.v(SafUtil.TAG,"开始遍历当前文件夹,原子count计数:"+countGet.incrementAndGet()+", "+dir.getName());
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                File[] files = dir.listFiles();
                if (files == null || files.length == 0) {
                    int count0 = countGet.decrementAndGet();
                    Log.v(SafUtil.TAG,"遍历当前一层文件夹完成,原子count计数:"+count0+", "+dir.getName());
                    if(count0 ==0){
                        onComplete(observer);
                    }

                    return;
                }
                List<BaseMediaFolderInfo> folderInfos = new ArrayList<>();

                BaseMediaFolderInfo imageFolder = null;
                int imageCount = 0;
                long imagesFileSize = 0;

                BaseMediaFolderInfo videoFolder = null;
                int videoCount = 0;
                long videoFileSize = 0;

                BaseMediaFolderInfo audioFolder = null;
                int audioCount = 0;
                long audioFileSize = 0;

                for (File file : files) {
                    if (file.isDirectory()) {
                        //6500个文件夹
                        if ("MicroMsg".equals(file.getName())) {
                            continue;
                        }
                        //700多个
                        if ("MobileQQ".equals(file.getName())) {
                            continue;
                        }
                        //Log.d("监听","进入文件夹遍历:"+dir.getAbsolutePath());
                        getAlbums(file, observer);
                    } else {
                        String name = file.getName();
                        int type = SafFileFinder.guessTypeByName(name);

                        if (type == BaseMediaFolderInfo.TYPE_IMAGE) {
                            imageCount++;
                            imagesFileSize = imagesFileSize + file.length();
                            if (imageFolder == null) {
                                imageFolder = new BaseMediaFolderInfo();
                                imageFolder.name= dir.getName();
                                imageFolder.cover = file.getAbsolutePath();
                                imageFolder.type = BaseMediaFolderInfo.TYPE_IMAGE;
                                imageFolder.updatedTime = file.lastModified();
                                imageFolder.pathOrUri = dir.getAbsolutePath();
                                Log.d("扫描", "添加有图文件夹:" + dir.getAbsolutePath());
                            }
                        }else if(type == BaseMediaFolderInfo.TYPE_VIDEO){
                            videoCount++;
                            videoFileSize = videoFileSize + file.length();
                            if (videoFolder == null) {
                                videoFolder = new BaseMediaFolderInfo();
                                videoFolder.name= dir.getName();
                                videoFolder.cover = file.getAbsolutePath();
                                videoFolder.updatedTime = file.lastModified();
                                videoFolder.type = BaseMediaFolderInfo.TYPE_VIDEO;
                                videoFolder.pathOrUri = dir.getAbsolutePath();
                                Log.d("扫描", "添加有视频文件夹:" + dir.getAbsolutePath());
                            }
                        }else if(type == BaseMediaFolderInfo.TYPE_AUDIO){
                            audioCount++;
                            audioFileSize = audioFileSize + file.length();
                            if (audioFolder == null) {
                                audioFolder = new BaseMediaFolderInfo();
                                audioFolder.name= dir.getName();
                                audioFolder.cover = file.getAbsolutePath();
                                audioFolder.updatedTime = file.lastModified();
                                audioFolder.type = BaseMediaFolderInfo.TYPE_AUDIO;
                                audioFolder.pathOrUri = dir.getAbsolutePath();
                                Log.d("扫描", "添加有音频文件夹:" + dir.getAbsolutePath());
                            }
                        }
                    }
                }
                if (imageFolder != null) {
                    imageFolder.count = imageCount;
                    imageFolder.fileSize = imagesFileSize;
                    folderInfos.add(imageFolder);
                }
                if (videoFolder != null) {
                    videoFolder.count = videoCount;
                    videoFolder.fileSize = videoFileSize;
                    folderInfos.add(videoFolder);
                }

                if (audioFolder != null) {
                    audioFolder.count = audioCount;
                    audioFolder.fileSize = audioFileSize;
                    folderInfos.add(audioFolder);
                }
                //写数据库:

                if(folderInfos.size() != 0){
                    observer.onNext(folderInfos);
                    print(folderInfos);
                }

                int count0 = countGet.decrementAndGet();
                Log.v(SafUtil.TAG,"遍历当前一层文件夹完成,原子count计数:"+count0+", "+dir.getName());
                if(count0 ==0){
                    onComplete(observer);
                }
            }
        });

    }

    private static void print(List<BaseMediaFolderInfo> folderInfos) {
        for (BaseMediaFolderInfo folderInfo : folderInfos) {
            Log.e(SafUtil.TAG,folderInfo.type+"-type-count-"+folderInfo.count+"-文件夹---->:"+folderInfo.pathOrUri);
        }
    }

    private static void onComplete(Observer<List<BaseMediaFolderInfo>> observer) {
        observer.onComplete();
        Log.w(SafUtil.TAG,"遍历所有文件夹完成!!!!!!!!!!!!!!! 耗时(ms):"+(System.currentTimeMillis() - start));
    }






}
