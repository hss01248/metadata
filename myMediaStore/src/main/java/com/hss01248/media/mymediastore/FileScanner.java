package com.hss01248.media.mymediastore;

import android.text.TextUtils;
import android.util.Log;


import androidx.documentfile.provider.DocumentFile;

import com.hss01248.media.mymediastore.bean.BaseMediaFolderInfo;
import com.hss01248.media.mymediastore.bean.BaseMediaInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class FileScanner {

    static long safStart;

    static AtomicInteger countGetSaf = new AtomicInteger(0);
    static final String TAG = "filescan";


    /**
     * 5条线程同时跑,cpu消耗整体为70%左右.
     * 1条线程,cpu22%  选用此方案
     * 2条线程 cpu 40%
     * 内存占用较小,约50M
     *
     * @param dir
     * @param observer
     */
     static void getAlbums(final File dir, ExecutorService executorService, final ScanFolderCallback observer) {
        Log.v(TAG, "开始遍历当前文件夹,原子count计数:" + countGetSaf.incrementAndGet() + ", " + dir.getName());
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                File[] files = dir.listFiles();
                if (files == null || files.length == 0) {
                    int count0 = countGetSaf.decrementAndGet();
                    Log.v(TAG, "遍历当前一层文件夹完成,原子count计数:" + count0 + ", " + dir.getName());
                    if (count0 == 0) {
                        SafFileFinder.onComplete(observer,false,safStart);
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

                List<BaseMediaInfo> images = null;
                List<BaseMediaInfo> videos = null;
                List<BaseMediaInfo> audios = null;

                for (File file : files) {
                    if (file.isDirectory()) {
                        //todo 6500个文件夹. 最后将其归并显示
                        if ("MicroMsg".equals(file.getName())) {
                            continue;
                        }
                        //700多个
                        if ("MobileQQ".equals(file.getName())) {
                            continue;
                        }
                        //Log.d("监听","进入文件夹遍历:"+dir.getAbsolutePath());
                        //todo 单线程时为深度优先.  那么前后两次要反着来
                        getAlbums(file, executorService, observer);
                    } else {
                        String name = file.getName();
                        if(TextUtils.isEmpty(name)){
                            continue;
                        }
                        if(file.length() <=0){
                            continue;
                        }
                        int type = SafFileFinder.guessTypeByName(name);

                        if (type == BaseMediaInfo.TYPE_IMAGE) {
                            imageCount++;
                            imagesFileSize = imagesFileSize + file.length();

                            if (imageFolder == null) {
                                imageFolder = new BaseMediaFolderInfo();
                                imageFolder.name = dir.getName();
                                imageFolder.cover = file.getAbsolutePath();
                                imageFolder.type = BaseMediaInfo.TYPE_IMAGE;
                                imageFolder.updatedTime = file.lastModified();
                                imageFolder.pathOrUri = dir.getAbsolutePath();
                                Log.d("扫描", "添加有图文件夹:" + dir.getAbsolutePath());
                            }

                            //内部文件uri的保存:
                            if (images == null) {
                                images = new ArrayList<>(files.length / 2);
                            }
                            BaseMediaInfo image = new BaseMediaInfo();
                            image.folderPathOrUri = dir.getAbsolutePath();
                            image.pathOrUri = file.getAbsolutePath();
                            image.updatedTime = file.lastModified();
                            image.name = file.getName();
                            image.fileSize = file.length();
                            image.type = BaseMediaInfo.TYPE_IMAGE;
                            images.add(image);

                        } else if (type == BaseMediaInfo.TYPE_VIDEO) {
                            videoCount++;
                            videoFileSize = videoFileSize + file.length();
                            if (videoFolder == null) {
                                videoFolder = new BaseMediaFolderInfo();
                                videoFolder.name = dir.getName();
                                videoFolder.cover = file.getAbsolutePath();
                                videoFolder.updatedTime = file.lastModified();
                                videoFolder.type = BaseMediaInfo.TYPE_VIDEO;
                                videoFolder.pathOrUri = dir.getAbsolutePath();
                                Log.d("扫描", "添加有视频文件夹:" + dir.getAbsolutePath());
                            }


                            //内部文件uri的保存:
                            if (videos == null) {
                                videos = new ArrayList<>(files.length / 2);
                            }
                            BaseMediaInfo image = new BaseMediaInfo();
                            image.folderPathOrUri = dir.getAbsolutePath();
                            image.pathOrUri = file.getAbsolutePath();
                            image.updatedTime = file.lastModified();
                            image.name = file.getName();
                            image.fileSize = file.length();
                            image.type = BaseMediaInfo.TYPE_VIDEO;
                            videos.add(image);
                        } else if (type == BaseMediaInfo.TYPE_AUDIO) {
                            audioCount++;
                            audioFileSize = audioFileSize + file.length();
                            if (audioFolder == null) {
                                audioFolder = new BaseMediaFolderInfo();
                                audioFolder.name = dir.getName();
                                audioFolder.cover = file.getAbsolutePath();
                                audioFolder.updatedTime = file.lastModified();
                                audioFolder.type = BaseMediaInfo.TYPE_AUDIO;
                                audioFolder.pathOrUri = dir.getAbsolutePath();
                                Log.d("扫描", "添加有音频文件夹:" + dir.getAbsolutePath());
                            }

                            //内部文件uri的保存:
                            if (audios == null) {
                                audios = new ArrayList<>(files.length / 2);
                            }
                            BaseMediaInfo image = new BaseMediaInfo();
                            image.folderPathOrUri = dir.getAbsolutePath();
                            image.pathOrUri = file.getAbsolutePath();
                            image.updatedTime = file.lastModified();
                            image.name = file.getName();
                            image.fileSize = file.length();
                            image.type = BaseMediaInfo.TYPE_AUDIO;
                            audios.add(image);
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
                if (folderInfos.size() != 0) {
                    SafFileFinder.print(folderInfos,false);
                    observer.onScanEachFolder(folderInfos);

                }
                SafFileFinder.writeDB(DocumentFile.fromFile(dir), folderInfos, images, videos, audios);

                int count0 = countGetSaf.decrementAndGet();
                Log.v(TAG, "遍历当前一层文件夹完成,原子count计数:" + count0 + ", " + dir.getName());
                if (count0 == 0) {
                    SafFileFinder.onComplete(observer,false,safStart);
                }
            }
        });

    }
}
