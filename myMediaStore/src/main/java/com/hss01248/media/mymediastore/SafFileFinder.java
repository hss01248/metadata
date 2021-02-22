package com.hss01248.media.mymediastore;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.documentfile.provider.DocumentFile;

import com.hss01248.media.mymediastore.bean.BaseMediaFolderInfo;
import com.hss01248.media.mymediastore.bean.BaseMediaInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Observer;


public class SafFileFinder {

    public static void listAllAlbum(final Observer<List<BaseMediaFolderInfo>> observer) {
        if(SafUtil.sdRoot == null){
            Log.w(SafUtil.TAG,Thread.currentThread().getName()+"  SafUtil.sdRoot is null");
            observer.onComplete();
            return;
        }
        start = System.currentTimeMillis();
        getAlbums(SafUtil.sdRoot, observer);
    }
    static long start ;
    public   static ExecutorService executorService = Executors.newFixedThreadPool(5);

    static AtomicInteger countGet = new AtomicInteger(0);


    private static void getAlbums(final DocumentFile dir, final Observer<List<BaseMediaFolderInfo>> observer) {
        Log.v(SafUtil.TAG,"开始遍历当前文件夹,原子count计数:"+countGet.incrementAndGet()+", "+dir.getName());
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                DocumentFile[] files = dir.listFiles();
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
                        getAlbums(file, observer);
                    } else {
                        String name = file.getName();
                        int type = guessTypeByName(name);

                        if (type == BaseMediaFolderInfo.TYPE_IMAGE) {
                            imageCount++;
                            imagesFileSize = imagesFileSize + file.length();
                            if (imageFolder == null) {
                                imageFolder = new BaseMediaFolderInfo();
                                imageFolder.name= dir.getName();
                                imageFolder.cover = file.getUri().toString();
                                imageFolder.type = BaseMediaFolderInfo.TYPE_IMAGE;
                                imageFolder.updatedTime = file.lastModified();
                                imageFolder.pathOrUri = dir.getUri().toString();
                                Log.d("扫描", "添加有图文件夹:" + dir.getUri().toString());
                            }
                        }else if(type == BaseMediaFolderInfo.TYPE_VIDEO){
                            videoCount++;
                            videoFileSize = videoFileSize + file.length();
                            if (videoFolder == null) {
                                videoFolder = new BaseMediaFolderInfo();
                                videoFolder.name= dir.getName();
                                videoFolder.cover = file.getUri().toString();
                                videoFolder.updatedTime = file.lastModified();
                                videoFolder.type = BaseMediaFolderInfo.TYPE_VIDEO;
                                videoFolder.pathOrUri = dir.getUri().toString();
                                Log.d("扫描", "添加有视频文件夹:" + dir.getUri().toString());
                            }
                        }else if(type == BaseMediaFolderInfo.TYPE_AUDIO){
                            audioCount++;
                            audioFileSize = audioFileSize + file.length();
                            if (audioFolder == null) {
                                audioFolder = new BaseMediaFolderInfo();
                                audioFolder.name= dir.getName();
                                audioFolder.cover = file.getUri().toString();
                                audioFolder.updatedTime = file.lastModified();
                                audioFolder.type = BaseMediaFolderInfo.TYPE_AUDIO;
                                audioFolder.pathOrUri = dir.getUri().toString();
                                Log.d("扫描", "添加有音频文件夹:" + dir.getUri().toString());
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
                if(folderInfos.size() != 0){
                    print(folderInfos);
                    observer.onNext(folderInfos);

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
            Log.v(SafUtil.TAG,folderInfo.type+"-type-count-"+folderInfo.count+"-文件夹---->:"+folderInfo.pathOrUri);
        }
    }

    private static void onComplete(Observer<List<BaseMediaFolderInfo>> observer) {
        observer.onComplete();
        Log.w(SafUtil.TAG,"遍历所有文件夹完成!!!!!!!!!!!!!!! 耗时(s):"+(System.currentTimeMillis() - start)/1000);
    }


    /**
     * 图片: https://zh.wikipedia.org/wiki/%E5%9B%BE%E5%BD%A2%E6%96%87%E4%BB%B6%E6%A0%BC%E5%BC%8F%E6%AF%94%E8%BE%83
     * 视频后缀
     * 最常见：.mpg .mpeg .avi .rm .rmvb .mov .wmv .asf .dat
     * 不常见的：.asx .wvx .mpe .mpa
     * 音频后缀
     * 常见的：.mp3 .wma .rm .wav .mid
     * .ape .flac
     *
     *常见 MIME 类型列表
     * https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types
     *
     * 作者：耐住寂寞守住繁华_5b9a
     * 链接：https://www.jianshu.com/p/8962f2a5186e
     * 来源：简书
     * 著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。
     * @param name
     * @return
     */
    public static int guessTypeByName(String name){
        if(TextUtils.isEmpty(name)){
            return -1;
        }
       String mime =  getTypeForName(name);
        if(mime.startsWith("image/")){
            return BaseMediaInfo.TYPE_IMAGE;
        }else if(mime.startsWith("video/")){
            return BaseMediaInfo.TYPE_VIDEO;
        }else if(mime.startsWith("audio/")){
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

    public static boolean isVideo(String name){
        return guessTypeByName(name) == BaseMediaInfo.TYPE_VIDEO;
    }
    public static boolean isImage(String name){
        return guessTypeByName(name) == BaseMediaInfo.TYPE_IMAGE;
    }
    public static boolean isAudio(String name){
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
