package com.hss01248.media.metadata;

import android.media.MediaMetadataRetriever;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class MetaDataUtil {

    @Deprecated
    public static String getExifStr(String path){
        Map<String,String> map = getAllInfo(path);
        return map.toString().replaceAll(",","\n");
    }

    public static String getDes(String path){
        return new GsonBuilder().setPrettyPrinting().create().toJson(getMetaData(path));
    }

    public static Map<String,String> getMetaData(String path){
        String mimetype = FileTypeUtil.getMineType(path);
        Map<String,String> data = new TreeMap<>();

        try {
            data.putAll(ExifUtil.getBasicMap(path));
        } catch (Exception e) {
            e.printStackTrace();
        }
        data.put("00-ext",FileTypeUtil.extName(path));
        if(TextUtils.isEmpty(data.get("00-path"))){
            data.put("00-path",path);
        }

        if(mimetype.contains("image")){
            data.putAll(ExifUtil.readExif(path));
            for (Map.Entry<String, String> stringStringEntry : data.entrySet()) {
                stringStringEntry.setValue(ExifUtil.stringfySomeTag(stringStringEntry.getKey(),stringStringEntry.getValue()));
            }
        }else if(mimetype.contains("video")){
            data.putAll(getAllInfo(path));
        }else if("gz".equals(FileTypeUtil.getType(new File(path)))){
            //解压缩
        }
        return data;

    }




    /**
     * 可用于音频和视频,不能用于图片
     * @param path
     * @return
     */
    @Deprecated
    public static Map<String,String> getAllInfo(String path){
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        Field[] fields = MediaMetadataRetriever.class.getDeclaredFields();
        Map<String,String> metadatas = new TreeMap<>();
        String key = "METADATA_KEY_";
        try {
            //没有权限时,crash
            retriever.setDataSource(path);
            for (Field field : fields) {
                if(  Modifier.isFinal(field.getModifiers())  && Modifier.isStatic(field.getModifiers())){
                    String name = field.getName();
                    field.setAccessible(true);
                    if(name.startsWith(key)){
                        Object o =  field.get(MediaMetadataRetriever.class);
                        //Log.d("media","field:"+name+" v:"+o);
                        if(o instanceof Integer){
                            int keyCode = (int) o;
                            String s = retriever.extractMetadata(keyCode);
                            String key2 = name.substring(key.length()).toLowerCase();
                            //Log.d("media",key2+" :"+s);
                            if(!TextUtils.isEmpty(s)){
                                metadatas.put(key2,s);
                                Log.d("mediainfo",key2+" :"+s);
                            }
                        }
                    }
                }
            }
        }catch (Throwable throwable){
            throwable.printStackTrace();
        }
        return metadatas;
    }
}
