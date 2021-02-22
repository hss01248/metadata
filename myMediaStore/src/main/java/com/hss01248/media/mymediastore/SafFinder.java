/*
package com.hss01248.media.mymediastore;

import android.os.Environment;
import android.os.FileUtils;
import android.text.TextUtils;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;


import com.hss01248.media.mymediastore.bean.Album;
import com.hss01248.media.mymediastore.bean.Image;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class SafFinder {

    //public static volatile List<Album> albumsOld = new ArrayList<>(256);
    public static volatile List<Album> albumsNew = new ArrayList<>(256);


    public static void listSafAlbum(final Observer<Album> observer,final Observer<List<Album>> observer2){
        if(SafUtil.sdRoot == null){
            Log.w(SafUtil.TAG,Thread.currentThread().getName()+"  SafUtil.sdRoot is null");
            observer2.onComplete();
            return;
        }

        final boolean[] hasCached = {false};
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                //使用文件缓存或者数据库缓存:
                File file = new File(ImageInfoFormater.context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"safcache.json");
                if(file.exists()){
                    try {
                        String json = FileUtils.readFileToString(file);
                        Log.d(SafUtil.TAG,json);
                        if(!TextUtils.isEmpty(json)){
                            List<Album> albums = new Gson().fromJson(json, new TypeToken<List<Album>>(){}.getType());
                            if(albums.size() >0){
                                hasCached[0] = true;

                                //如何去重?
                                LinkedHashSet<Album> set = new LinkedHashSet<Album>(albums.size());
                                set.addAll(albums);
                                albums.clear();
                                albums.addAll(set);

                               // albumsOld.clear();
                               // albumsOld.addAll(albums);

                                observer2.onNext(albums);
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                observer2.onComplete();
            }
        });
        // //1h间隔
        if(countGet.get() != 0){
            Log.w(SafUtil.TAG,Thread.currentThread().getName()+"  上一次的任务还在跑,不再继续");
            return;
        }
        if(System.currentTimeMillis() - lastTime < 30*60*1000){
            Log.w(SafUtil.TAG,Thread.currentThread().getName()+"  30min内不再执行刷新缓存操作");
            return;
        }
        lastTime = System.currentTimeMillis();
        //stopBefore();
        //并行递归,如何判断最终完成?
        getAlbums(SafUtil.sdRoot,observer);
    }
    static long lastTime;



    private static void stopBefore() {
        //stopAll.getAndSet(true);
        if(countGet.get() != 0){
            executorService.shutdownNow();
            executorService = Executors.newFixedThreadPool(20);
            albumsNew.clear();
        }

    }

    public   static ExecutorService executorService = Executors.newFixedThreadPool(20);

   static  AtomicInteger countGet = new AtomicInteger(0);


    private static void getAlbums(final DocumentFile dir, final Observer<Album> observer) {

        Log.w(SafUtil.TAG,"开始遍历当前文件夹,原子count计数:"+countGet.incrementAndGet()+", "+dir.getName());
        executorService.execute(new Runnable() {
            @Override
            public void run() {

                DocumentFile[] files = dir.listFiles();
                if(files == null || files.length ==0){
                    int count0 = countGet.decrementAndGet();
                    if(count0 ==0){
                        onComplete(observer);
                    }
                    Log.w(SafUtil.TAG,"遍历当前一层文件夹完成,原子count计数:"+count0+", "+dir.getName());
                    return;
                }
                Album album = null;
                List<Image> images = new ArrayList<>(files.length);
                int count = 0;
                //Log.d(SafUtil.TAG,Thread.currentThread().getName()+"  展开文件夹:"+ Uri.decode(dir.getUri().toString()));
                for (DocumentFile file : files) {

                    if(file.isDirectory()){
                        //6500个文件夹
                        if("MicroMsg".equals(file.getName())){
                            continue;
                        }
                        //700多个
                        if("MobileQQ".equals(file.getName())){
                            continue;
                        }
                        getAlbums(file,observer);
                    }else {

                        String name = file.getName();
                        if(TextUtils.isEmpty(name)){
                            continue;
                        }
                        if(name.endsWith(".jpg")|| name.endsWith(".png") || name.endsWith(".gif")
                                || name.endsWith(".webp") || name.endsWith(".JPG") || name.endsWith(".jpeg")){
                            count++;

                            Image image = new Image(0,name,file.getUri().toString(),false);
                            images.add(image);
                            if(count != 0 && count % 100 ==0){
                                //writeCacheImages(images,dir.getUri().toString());
                            }
                            if(album != null){
                                album.fileSize = file.length() + album.fileSize;
                                continue;
                            }
                            album = new Album(dir.getName(),file.getUri().toString());
                            album.fileSize = file.length();
                            album.pathOrUri = dir.getUri().toString();
                           // album.dirSaf = dir;
                            //Log.d(SafUtil.TAG,Thread.currentThread().getName()+"  添加有图文件夹:"+album.dir);
                        }
                    }
                }
                if(album != null){
                    album.count = count;
                    albumsNew.add(album);
                    observer.onNext(album);
                    writeCacheImages(images,dir.getUri().toString());
                    Log.d("监听", "添加有图文件夹 完成 :" + URLDecoder.decode(album.pathOrUri));
                }
                int count0 = countGet.decrementAndGet();
                if(count0 ==0){
                    onComplete(observer);
                }
                Log.w(SafUtil.TAG,"遍历当前一层文件夹完成,原子count计数:"+count0+", "+dir.getName());
            }
        });


    }

    private static void onComplete(Observer<Album> observer) {

        //写文件:
        Collections.sort(albumsNew, new Comparator<Album>() {
            @Override
            public int compare(Album o1, Album o2) {
                return (o2.fileSize > o1.fileSize) ? 1 : -1;
            }
        });

        //新旧数据同步
       // albumsOld.clear();
       // albumsOld.addAll(albumsNew);
        */
/*Iterator<Album> iterator = albumsOld.iterator();
        while (iterator.hasNext()){
            Album albumOld = iterator.next();
            if(albumsNew.contains(albumOld)){
               Album albumNew =  albumsNew.get(albumsNew.indexOf(albumOld));
               albumOld.fileSize = albumNew.fileSize;
               albumOld.count = albumNew.count;
            }else {
                //老数据里有,新数据已经没有了,那么要删除
                iterator.remove();
            }
        }
        for (Album albumNew : albumsNew) {
            if(!albumsOld.contains(albumNew)){
                albumsOld.add(albumNew);
            }
        }*//*


        observer.onComplete();
        try {
            File file = new File(ImageInfoFormater.context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"safcache.json");
            Log.v(SafUtil.TAG,"遍历终于结束,写文件:"+file.getAbsolutePath());
            String json = new Gson().toJson(albumsNew);
            FileUtils.writeStringToFile(file,json);

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            albumsNew.clear();
        }
    }
    public static String md5(String dataStr) {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(dataStr.getBytes("UTF8"));
            byte s[] = m.digest();
            String result = "";
            for (int i = 0; i < s.length; i++) {
                result += Integer.toHexString((0x000000FF & s[i]) | 0xFFFFFF00).substring(6);
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return dataStr;
    }
    public static void writeCacheImages(List<Image> temp,String albumDir) {
        Observable.just(1).subscribeOn(Schedulers.io())
                .doOnNext(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        File dir = new File(ImageInfoFormater.context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"picuricache");
                        if(!dir.exists()){
                            dir.mkdirs();
                        }
                        File file = new File(dir,md5(albumDir)+".json");
                        try {
                            String json = new Gson().toJson(new ArrayList<>(temp));
                            FileUtils.writeStringToFile(file,json);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                })//.observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Integer i) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {

                    }
                });

    }
}
*/
