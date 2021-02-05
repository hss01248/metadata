package com.hss01248.media.metadata;

import android.media.MediaMetadataRetriever;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.TreeMap;

public class MetaDataUtil {

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
