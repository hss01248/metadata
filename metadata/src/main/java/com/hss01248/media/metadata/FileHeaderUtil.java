package com.hss01248.media.metadata;

import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.Utils;
import com.hss01248.media.metadata.quality.Magick;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class FileHeaderUtil {

    static Map<String, String> uriHeaders = new HashMap<>();

    public static TreeMap<String, String> parseHeaders(Uri uri,@Nullable MetaInfo info) {
        String mimetype = FileTypeUtil.getMimeType(uri);
        TreeMap<String, String> metadatas = new TreeMap<>();

        if (mimetype.startsWith("image")) {
            InputStream inputStream = getInputStream(uri);
            if(inputStream == null){
                return metadatas;
            }
            //todo http文件大小设置
            Map<String, String> stringStringMap = ExifUtil.readExif(inputStream, true);
            for (Map.Entry<String, String> stringStringEntry : stringStringMap.entrySet()) {
                stringStringEntry.setValue(ExifUtil.stringfySomeTag(stringStringEntry.getKey(), stringStringEntry.getValue()));
            }
            metadatas.putAll(stringStringMap);
        } else if (mimetype.startsWith("video") || mimetype.startsWith("audio")) {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            Field[] fields = MediaMetadataRetriever.class.getDeclaredFields();
            String key = "METADATA_KEY_";
            try {
                //没有权限时,crash
                if ("file".equals(uri.getScheme())) {
                    retriever.setDataSource(URLDecoder.decode(uri.toString().substring("file://".length())));
                } else if ("content".equals(uri.getScheme())) {
                    retriever.setDataSource(Utils.getApp(), uri);
                } else if ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) {
                    retriever.setDataSource(uri.toString(), uriHeaders);
                }

                for (Field field : fields) {
                    if (Modifier.isFinal(field.getModifiers()) && Modifier.isStatic(field.getModifiers())) {
                        String name = field.getName();
                        field.setAccessible(true);
                        if (name.startsWith(key)) {
                            Object o = field.get(MediaMetadataRetriever.class);
                            //Log.d("media","field:"+name+" v:"+o);
                            if (o instanceof Integer) {
                                int keyCode = (int) o;
                                String s = retriever.extractMetadata(keyCode);
                                String key2 = name.substring(key.length()).toLowerCase();
                                //Log.d("media",key2+" :"+s);
                                if (!TextUtils.isEmpty(s)) {
                                    metadatas.put(key2, s);
                                    Log.d("mediainfo", key2 + " :" + s);
                                }
                            }
                        }
                    }
                }
            } catch (Throwable throwable) {
                LogUtils.w(throwable);
            }
        } else {
            LogUtils.w("非媒体类型-"+mimetype,uri);
        }
        return metadatas;
    }

    public static InputStream getInputStream(Uri uri) {

        InputStream inputStream = null;

        try {
            if ("file".equals(uri.getScheme())) {
                inputStream = new FileInputStream(new File(URLDecoder.decode(uri.toString().substring("file://".length()))));
            } else if ("content".equals(uri.getScheme())) {
                inputStream = Utils.getApp().getContentResolver().openInputStream(uri);
            } else if ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) {
                Request.Builder builder = new Request.Builder();
                if (!uriHeaders.isEmpty()) {
                    for (String s : uriHeaders.keySet()) {
                        builder.header(s, uriHeaders.get(s) + "");
                    }
                }
                inputStream = new OkHttpClient.Builder()
                        .build()
                        .newCall(builder
                                .url(uri.toString())
                                .get()
                                .build())
                        .execute()
                        .body()
                        .byteStream();
            }
        } catch (Throwable throwable) {
            LogUtils.w(throwable);
        }

        return inputStream;

    }

    public static int getJpegQuality(Uri uri) {
        InputStream inputStream = getInputStream(uri);
        if (inputStream == null) {
            return 0;
        }
        return new Magick().getJPEGImageQuality(inputStream);
    }

    public static void parseRealCreatedTime(MetaInfo info) {
        if(info.fileInfo.mimeTypeByExt.startsWith("video")){
            long timeFromExif = MetaDataUtil.timeFromMeta(info.fileHeaders);
            if(timeFromExif > 0){
                info.fileInfo.realCreatedTime = timeFromExif;
                return;
            }
        }else if(info.fileInfo.mimeTypeByExt.startsWith("image")){
            long timeFromExif = MetaDataUtil.timeFromExif(info.fileHeaders);
            if(timeFromExif > 0){
                info.fileInfo.realCreatedTime = timeFromExif;
                return;
            }
        }
        long timeFromFileName = MetaDataUtil.timeGuessFromFileName(info.fileInfo.name);
        if(timeFromFileName > 0){
            info.fileInfo.realCreatedTime = timeFromFileName;
            return ;
        }
        info.fileInfo.realCreatedTime = info.fileInfo.lastModified;
    }

    public static void parseLocation(MetaInfo info) {
        if(info.fileHeaders.containsKey(ExifInterface.TAG_GPS_LATITUDE) && !"0".equals(info.fileHeaders.get(ExifInterface.TAG_GPS_LATITUDE))){
            info.extras.put(ExifInterface.TAG_GPS_LATITUDE,info.fileHeaders.get(ExifInterface.TAG_GPS_LATITUDE));
        }
        if(info.fileHeaders.containsKey(ExifInterface.TAG_GPS_LONGITUDE) && !"0".equals(info.fileHeaders.get(ExifInterface.TAG_GPS_LONGITUDE))){
            info.extras.put(ExifInterface.TAG_GPS_LONGITUDE,info.fileHeaders.get(ExifInterface.TAG_GPS_LONGITUDE));
        }
        if(info.fileHeaders.containsKey("location") ){
            //视频信息里 location: "+22.998+110.6769/
            info.extras.put("location",info.fileHeaders.get("location"));
        }

    }
}
