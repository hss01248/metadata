package com.hss01248.media.metadata;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.exifinterface.media.ExifInterface;

import android.graphics.ColorSpace;
import android.graphics.Rect;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;


public class ExifUtil {


    static Context context;
    public static void init(Context context){
        ExifUtil.context = context;
    }

    public static Map<String,String> readExif(InputStream inputStream){
       return readExif(inputStream,true);
    }

    public static String getExifStr(InputStream inputStream){
        Map<String,String> map = readExif(inputStream);
        return map.toString().replaceAll(",","\n");
    }
    public static Map<String,String> readExif(InputStream inputStream,boolean close){
        Map<String,String> exifMap = new TreeMap<>();
        try {
            ExifInterface exif = new ExifInterface(inputStream);
            Class exifClazz = ExifInterface.class;
            Field[] fields = exifClazz.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                if(field.getName().startsWith("TAG_")){
                    field.setAccessible(true);
                    try {
                        Object obj = field.get(ExifInterface.class);
                        if(obj instanceof String){
                            String tag = (String) field.get(ExifInterface.class);
                            String val = exif.getAttribute(tag);
                            if(!TextUtils.isEmpty(val)){
                               val =  stringfySomeTag(tag,val);
                                exifMap.put(tag,val);
                            }
                        }else {
                            //w("field not string:"+obj);
                        }

                    }catch (Throwable throwable){
                        exception("dd",throwable);
                    }
                }
            }
        } catch (Exception e) {
            exception("dd",e);
        }finally {
            if(close){
                try {
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            json(exifMap);
            return exifMap;
        }
    }

    private static String stringfySomeTag(String tag, String val) {
        if(ExifInterface.TAG_GPS_LATITUDE.equals(tag) || ExifInterface.TAG_GPS_LONGITUDE.equals(tag)){
            try {
                return parseGps(val);
            }catch (Throwable throwable){
                throwable.printStackTrace();
                return val;
            }

        }
        if(ExifInterface.TAG_COLOR_SPACE.equals(tag)){
            return parseColorSpace(val);
        }
        if(ExifInterface.TAG_ORIENTATION.equals(tag)){
            return parseOritation(val);
        }
        return val;
    }

    private static String parseOritation(String val) {
        if((ExifInterface.COLOR_SPACE_S_RGB+"").equals(val)){
            return "sRGB";
        }else if((ExifInterface.COLOR_SPACE_UNCALIBRATED+"").equals(val)){
            return "UNCALIBRATED";
        }
        return "sRGB";
    }

    private static String parseColorSpace(String val) {
       if((ExifInterface.ORIENTATION_ROTATE_90+"").equals(val)){
           return "90";
        }else if((ExifInterface.ORIENTATION_ROTATE_180+"").equals(val)){
           return "180";
       }else if((ExifInterface.ORIENTATION_ROTATE_270+"").equals(val)){
           return "270";
       }
        return "0";
    }

    private static String parseGps(String val) {
        val = val.replace("/", ",");
        String[] lat = val.split(",");
        Float latD =0f;

        Float latM =0f;

        Float latS =0f;
        if (lat.length >=2) {

            latD = Float.parseFloat(lat[0]) / Float.parseFloat(lat[1]);

        }

        if (lat.length >=4) {

            latM = Float.parseFloat(lat[2]) / Float.parseFloat(lat[3]);

        }

        if (lat.length >=6) {

            latS = Float.parseFloat(lat[4]) / Float.parseFloat(lat[5]);

        }



       float latitude = latD + latM /60 + latS /3600;
        return latitude+"";
    }


    private static void json(Map<String, String> exifMap) {
       Log.i("exif", exifMap.toString());
    }

    private static void exception(String dd, Throwable throwable) {
        throwable.printStackTrace();
    }

    public static void writeExif(Map<String,String> exifMap, String file){
        try{
            if(exifMap.isEmpty()){
                w("exifMap.isEmpty");
                return;
            }
            File file1 = new File(file);
            if(!file1.exists()){
                w("file not exist:"+file);
                return;
            }
            resetImageWHToMap(exifMap,getInputStream(file),true);
            ExifInterface exif = new ExifInterface(file);
            Iterator<Map.Entry<String,String>> it = exifMap.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry<String,String> entry = it.next();
                if(entry.getValue() != null){
                    try {
                        exif.setAttribute(entry.getKey(),entry.getValue());
                    }catch (Throwable throwable){
                        exception("setAttribute",throwable);
                    }
                }
            }
            if(!exif.hasAttribute(ExifInterface.TAG_DATETIME)){
                exif.setAttribute(ExifInterface.TAG_DATETIME,String.format("YYYY-MM-DD HH:MM:SS",System.currentTimeMillis()));
            }
            exif.saveAttributes();
        }catch (Throwable e){
            e.printStackTrace();
        }
    }

    private static void w(String s) {
        Log.w("exif",s);
    }

    public static void resetImageWHToMap(Map<String, String> exifMap, InputStream stream,boolean resetOritation) {
        if(exifMap.isEmpty()){
            w("exifMap.isEmpty");
            return;
        }
        if(stream == null){
            w("stream.isEmpty");
            return;
        }
        try{
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            Bitmap bitmap = BitmapFactory.decodeStream(stream,new Rect(), options); // 此时返回的bitmap为null
            if(options.outWidth> 0 && options.outHeight > 0){
                exifMap.put("ImageLength",options.outHeight+"");
                exifMap.put("ImageWidth",options.outWidth+"");
                exifMap.put("PixelXDimension",options.outHeight+"");
                exifMap.put("PixelYDimension",options.outWidth+"");
                if(resetOritation){
                    exifMap.put("Orientation",0+"");
                }
            }


        }catch (Throwable throwable){
            exception("setAttribute",throwable);
        }
    }

    public static InputStream getInputStream(String source) throws IOException{
        if(TextUtils.isEmpty(source)){
            return null;
        }
        File file = new File(source);
        if(file.exists()){
            return new FileInputStream(source);
        }
        return context.getContentResolver().openInputStream(Uri.parse(source));
    }
}
