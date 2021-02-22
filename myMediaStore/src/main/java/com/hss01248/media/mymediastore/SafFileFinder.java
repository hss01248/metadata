package com.hss01248.media.mymediastore;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.documentfile.provider.DocumentFile;

import com.hss01248.media.mymediastore.bean.BaseMediaFolderInfo;
import com.hss01248.media.mymediastore.bean.BaseMediaInfo;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;


public class SafFileFinder {

    static final String SP_NAME = "DirPermission";

    public static void listAllAlbum(final ScanFolderCallback observer) {
        listFromDb(observer);
    }

    private static void listFromDb(ScanFolderCallback observer) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                List<BaseMediaFolderInfo> infos = DbUtil.getAllImageAndVideoFolders();

                boolean hasDataInDb = false;
                if (infos != null && infos.size() > 0) {
                    observer.onFromDB(infos);
                    hasDataInDb = true;
                }

                scanByFile(hasDataInDb, executorService, observer);
                scanBySaf(hasDataInDb, executorService, observer);

            }
        });
    }

    private static void scanByFile(boolean hasDataInDb, ExecutorService executorService, ScanFolderCallback observer) {
        FileScanner.safStart = System.currentTimeMillis();
        FileScanner.getAlbums(Environment.getExternalStorageDirectory(),executorService,observer);
    }

    private static void scanBySaf(boolean hasDataInDb, ExecutorService executorService, ScanFolderCallback observer) {
        if (SafUtil.sdRoot == null) {
            Log.w(SafUtil.TAG, Thread.currentThread().getName() + "  SafUtil.sdRoot is null");
            observer.onComplete();
            return;
        }

        SharedPreferences sp = SafUtil.context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        if (hasDataInDb) {
            long latScanFinishedTime = sp.getLong("latScanFinishedTime", 0);
           /* if (latScanFinishedTime != 0 && (System.currentTimeMillis() - latScanFinishedTime < 12 * 60 * 60 * 1000)) {
                Log.w(SafUtil.TAG, "一天内只扫描一次");
                return;
            }*/
            sp.edit().putBoolean("isScaning", true).commit();
            //有数据,那么接下来就只用一个线程去跑
            safStart = System.currentTimeMillis();
            getAlbums(SafUtil.sdRoot, executorService, observer);
        } else {
            sp.edit().putBoolean("isScaning", true).commit();
            //没有数据,就用5个线程去跑
            safStart = System.currentTimeMillis();
            getAlbums(SafUtil.sdRoot, executorService, observer);
        }
    }

    static long safStart;

    static AtomicInteger countGetSaf = new AtomicInteger(0);


    /**
     * 5条线程同时跑,cpu消耗整体为70%左右.
     * 1条线程,cpu22%  选用此方案
     * 2条线程 cpu 40%
     * 内存占用较小,约50M
     *
     * @param dir
     * @param observer
     */
    private static void getAlbums(final DocumentFile dir, ExecutorService executorService, final ScanFolderCallback observer) {
        Log.v(SafUtil.TAG, "开始遍历当前文件夹,原子count计数:" + countGetSaf.incrementAndGet() + ", " + dir.getName());
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                DocumentFile[] files = dir.listFiles();
                if (files == null || files.length == 0) {
                    int count0 = countGetSaf.decrementAndGet();
                    Log.v(SafUtil.TAG, "遍历当前一层文件夹完成,原子count计数:" + count0 + ", " + dir.getName());
                    if (count0 == 0) {
                        onComplete(observer,true,safStart);
                    }

                    return;
                }
                if (countGetSaf.get() == 1) {
                    //根目录. 随机逆序
                    boolean random = new Random().nextBoolean();
                    if (random) {
                        List<DocumentFile> list = new ArrayList<>(Arrays.asList(files));
                        Collections.reverse(list);
                        for (int i = 0; i < list.size(); i++) {
                            files[i] = list.get(i);
                        }
                    }
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

                for (DocumentFile file : files) {
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
                        int type = guessTypeByName(name);

                        if (type == BaseMediaInfo.TYPE_IMAGE) {
                            imageCount++;
                            imagesFileSize = imagesFileSize + file.length();

                            if (imageFolder == null) {
                                imageFolder = new BaseMediaFolderInfo();
                                imageFolder.name = dir.getName();
                                imageFolder.cover = file.getUri().toString();
                                imageFolder.type = BaseMediaInfo.TYPE_IMAGE;
                                imageFolder.updatedTime = file.lastModified();
                                imageFolder.pathOrUri = dir.getUri().toString();
                                Log.d("扫描", "添加有图文件夹:" + dir.getUri().toString());
                            }

                            //内部文件uri的保存:
                            if (images == null) {
                                images = new ArrayList<>(files.length / 2);
                            }
                            BaseMediaInfo image = new BaseMediaInfo();
                            image.folderPathOrUri = dir.getUri().toString();
                            image.pathOrUri = file.getUri().toString();
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
                                videoFolder.cover = file.getUri().toString();
                                videoFolder.updatedTime = file.lastModified();
                                videoFolder.type = BaseMediaInfo.TYPE_VIDEO;
                                videoFolder.pathOrUri = dir.getUri().toString();
                                Log.d("扫描", "添加有视频文件夹:" + dir.getUri().toString());
                            }


                            //内部文件uri的保存:
                            if (videos == null) {
                                videos = new ArrayList<>(files.length / 2);
                            }
                            BaseMediaInfo image = new BaseMediaInfo();
                            image.folderPathOrUri = dir.getUri().toString();
                            image.pathOrUri = file.getUri().toString();
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
                                audioFolder.cover = file.getUri().toString();
                                audioFolder.updatedTime = file.lastModified();
                                audioFolder.type = BaseMediaInfo.TYPE_AUDIO;
                                audioFolder.pathOrUri = dir.getUri().toString();
                                Log.d("扫描", "添加有音频文件夹:" + dir.getUri().toString());
                            }

                            //内部文件uri的保存:
                            if (audios == null) {
                                audios = new ArrayList<>(files.length / 2);
                            }
                            BaseMediaInfo image = new BaseMediaInfo();
                            image.folderPathOrUri = dir.getUri().toString();
                            image.pathOrUri = file.getUri().toString();
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
                    print(folderInfos, true);
                    observer.onScanEachFolder(folderInfos);

                }
                writeDB(dir, folderInfos, images, videos, audios);

                int count0 = countGetSaf.decrementAndGet();
                Log.v(SafUtil.TAG, "遍历当前一层文件夹完成,原子count计数:" + count0 + ", " + dir.getName());
                if (count0 == 0) {
                    onComplete(observer,true,safStart);
                }
            }
        });

    }

     static void writeDB(DocumentFile dir, List<BaseMediaFolderInfo> folderInfos, List<BaseMediaInfo> images, List<BaseMediaInfo> videos, List<BaseMediaInfo> audios) {
        long start = System.currentTimeMillis();
        //文件夹:
        if (folderInfos.size() > 0) {
            DbUtil.getDaoSession().getBaseMediaFolderInfoDao().insertOrReplaceInTx(folderInfos);
            //DbUtil.getDaoSession().getBaseMediaFolderInfoDao().upda
            //todo 如何不更新里面的hidden值?
        }
        if (images != null) {
            DbUtil.getDaoSession().getBaseMediaInfoDao().insertOrReplaceInTx(images);
        }
        if (videos != null) {
            DbUtil.getDaoSession().getBaseMediaInfoDao().insertOrReplaceInTx(videos);
        }
        if (audios != null) {
            DbUtil.getDaoSession().getBaseMediaInfoDao().insertOrReplaceInTx(audios);
        }
        if (folderInfos.size() > 0) {
            Log.v(SafUtil.TAG, URLDecoder.decode(dir.getUri().toString()) + "  路径下更新数据库完成!!!!!!!!!!!!!!! 耗时(ms):" + (System.currentTimeMillis() - start));
        }

        //todo 已经删除的文件,怎么删除数据库里的条目?


    }

     static void print(List<BaseMediaFolderInfo> folderInfos, boolean isSaf) {
        for (BaseMediaFolderInfo folderInfo : folderInfos) {
            Log.v(isSaf ? SafUtil.TAG : FileScanner.TAG, folderInfo.type + "-type-count-" + folderInfo.count + "-文件夹---->:" + folderInfo.pathOrUri);
        }
    }

     static void onComplete(ScanFolderCallback observer,boolean isSaf,long safStart) {
        List<BaseMediaFolderInfo> infos = DbUtil.getAllImageAndVideoFolders();
        observer.onScanFinished(infos);
        observer.onComplete();
        Log.w(isSaf ? SafUtil.TAG : FileScanner.TAG, "遍历所有文件夹完成!!!!!!!!!!!!!!! 耗时(s):" + (System.currentTimeMillis() - safStart) / 1000);

        if(isSaf){
            SharedPreferences sp = SafUtil.context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();

            editor.putBoolean("isScaning", false).commit();
            editor.putLong("latScanFinishedTime", System.currentTimeMillis()).commit();
        }

    }


    /**
     * 图片: https://zh.wikipedia.org/wiki/%E5%9B%BE%E5%BD%A2%E6%96%87%E4%BB%B6%E6%A0%BC%E5%BC%8F%E6%AF%94%E8%BE%83
     * 视频后缀
     * 最常见：.mpg .mpeg .avi .rm .rmvb .mov .wmv .asf .dat
     * 不常见的：.asx .wvx .mpe .mpa
     * 音频后缀
     * 常见的：.mp3 .wma .rm .wav .mid
     * .ape .flac
     * <p>
     * 常见 MIME 类型列表
     * https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types
     * <p>
     * 作者：耐住寂寞守住繁华_5b9a
     * 链接：https://www.jianshu.com/p/8962f2a5186e
     * 来源：简书
     * 著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。
     *
     * @param name
     * @return
     */
    public static int guessTypeByName(String name) {
        if (TextUtils.isEmpty(name)) {
            return -1;
        }
        String mime = getTypeForName(name);
        if (mime.startsWith("image/")) {
            return BaseMediaInfo.TYPE_IMAGE;
        } else if (mime.startsWith("video/")) {
            return BaseMediaInfo.TYPE_VIDEO;
        } else if (mime.startsWith("audio/")) {
            return BaseMediaInfo.TYPE_AUDIO;
        }
        /*if (name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".gif")
                || name.endsWith(".webp") || name.endsWith(".JPG") || name.endsWith(".jpeg")
                || name.endsWith(".svg")  || name.endsWith(".bmp")) {
            return BaseMediaInfo.TYPE_IMAGE;
        }else if(name.endsWith(".mp4") || name.endsWith(".MP4") || name.endsWith(".mkv") || name.endsWith(".avi")
                || name.endsWith(".mpeg") || name.endsWith(".wmv") || name.endsWith(".mpg") || name.endsWith(".rmvb")
                || name.endsWith(".mov") || name.endsWith(".flv")){
            return BaseMediaInfo.TYPE_VIDEO;
        }else if(name.endsWith(".m4a") ||name.endsWith(".mp3") || name.endsWith(".MP3") || name.endsWith(".aac") || name.endsWith(".wav")
                || name.endsWith(".wma") || name.endsWith(".mid") || name.endsWith(".ape") || name.endsWith(".flac")){
            return BaseMediaInfo.TYPE_AUDIO;
        }*/
        return -1;
    }

    public static boolean isVideo(String name) {
        return guessTypeByName(name) == BaseMediaInfo.TYPE_VIDEO;
    }

    public static boolean isImage(String name) {
        return guessTypeByName(name) == BaseMediaInfo.TYPE_IMAGE;
    }

    public static boolean isAudio(String name) {
        return guessTypeByName(name) == BaseMediaInfo.TYPE_AUDIO;
    }

    private static String getTypeForName(String name) {
        final int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            final String extension = name.substring(lastDot + 1).toLowerCase();
            final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mime != null) {
                /*Log.v(SafUtil.TAG,"mimeType:"+mime +" ->>"+name);
                int last = mime.indexOf("/");
                if(last >0){
                    String type = mime.substring(0,last);
                    Log.v(SafUtil.TAG,"raw type:"+type +" ->>"+name);
                }*/

                return mime;
            }
        }

        return "application/octet-stream";
    }


}
