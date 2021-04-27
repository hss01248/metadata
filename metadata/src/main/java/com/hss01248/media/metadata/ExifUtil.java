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

import com.hss01248.media.metadata.quality.Magick;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;


public class ExifUtil {


    static Context context;
   public static boolean enableLog;
    public static void init(Context context){
        ExifUtil.context = context;
    }

    public static Map<String,String> readExif(InputStream inputStream){
       return readExif(inputStream,true);
    }

    public static void copyExif(String from, String to){
        writeExif(readExif(from),to);
    }

    public static String getExifStr(InputStream inputStream){
        String fileSize = "";
        String wh = "";
        String quality = "";
        String path = "";
        try {
             fileSize = formatFileSize(inputStream.available());
             path = getPathFromStream(inputStream);


            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            long count = 0;
            int n = 0;
            while (-1 != (n = inputStream.read(buffer))) {
                output.write(buffer, 0, n);
                count += n;
            }
            byte[] bytes = output.toByteArray();
            try {
                inputStream.close();
            }catch (Throwable throwable){

            }

            inputStream = new ByteArrayInputStream(bytes);
            wh = formatWh(new ByteArrayInputStream(bytes));
            quality = new Magick().getJPEGImageQuality(new ByteArrayInputStream(bytes))+"";

        } catch (Throwable e) {
            e.printStackTrace();
        }
        Map<String,String> map = new TreeMap<>(readExif(inputStream));
        for (Map.Entry<String, String> stringStringEntry : map.entrySet()) {
            stringStringEntry.setValue(stringfySomeTag(stringStringEntry.getKey(),stringStringEntry.getValue()));
        }
        map.put("0-wh",wh);
        map.put("0-quality",quality);
        map.put("0-fileSize",fileSize);
        String str =  map.toString().replaceAll(",","\n");
        return str;
    }

    private static String getPathFromStream(InputStream inputStream) {
        try {
            if(inputStream instanceof FileInputStream){
                //Accessing hidden field Ljava/io/FileInputStream;->path:Ljava/lang/String; (greylist-max-o, reflection, denied)
//java.lang.NoSuchFieldException: No field path in class Ljava/io/FileInputStream; (declaration of 'java.io.FileInputStream' appears in /apex/com.android.runtime/javalib/core-oj.jar)
               /* Class clazz = FileInputStream.class;
                Field field = clazz.getDeclaredField("path");
                field.setAccessible(true);
              String path = (String) field.get(inputStream);*/
              return "";

            }
        }catch (Throwable throwable){
            throwable.printStackTrace();
        }

        return "";
    }

    public static String getExifStr(String path){
        Map<String,String> exifMap = new TreeMap<>(readExif(path));
        try {
            //inputStream有位移,所以不能继续使用
            File file = new File(path);
            exifMap.put("00-path",path);
            exifMap.put("0-fileSize",formatFileSize(file.length()));

            exifMap.put("0-wh",formatWh(new FileInputStream(path)));

            exifMap.put("0-quality",new Magick().getJPEGImageQuality(new FileInputStream(file))+"");
        }catch (Throwable throwable){
            throwable.printStackTrace();
        }
        for (Map.Entry<String, String> stringStringEntry : exifMap.entrySet()) {
            stringStringEntry.setValue(stringfySomeTag(stringStringEntry.getKey(),stringStringEntry.getValue()));
        }
        String str =  exifMap.toString().replaceAll(",","\n");
        return str;
    }

    public static Map<String,String> readExif(String path){
        try {
            return readExif(new FileInputStream(new File(path)),true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return new TreeMap<>();
        }
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

    private static String formatWh(InputStream inputStream) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try {
            BitmapFactory.decodeStream(inputStream,new Rect(),options);
        } catch (Throwable e) {
            e.printStackTrace();
        }finally {
            try {
                inputStream.close();
            }catch (Throwable e){

            }
        }
        /**
         *options.outHeight为原始图片的高
         */
        return options.outWidth+"x"+ options.outHeight;


    }

     static String formatFileSize(long size) {
        try {
            DecimalFormat dff = new DecimalFormat(".00");
            if (size >= 1024 * 1024) {
                double doubleValue = ((double) size) / (1024 * 1024);
                String value = dff.format(doubleValue);
                return value + "MB";
            } else if (size > 1024) {
                double doubleValue = ((double) size) / 1024;
                String value = dff.format(doubleValue);
                return value + "KB";
            } else {
                return size + "B";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return String.valueOf(size);
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

    private static String parseColorSpace(String val) {
        if((ExifInterface.COLOR_SPACE_S_RGB+"").equals(val)){
            return "sRGB";
        }else if((ExifInterface.COLOR_SPACE_UNCALIBRATED+"").equals(val)){
            return "UNCALIBRATED";
        }
        return "sRGB";
    }

    private static String parseOritation(String val) {
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
        if(enableLog)
       Log.i("exif", exifMap.toString());
    }

    private static void exception(String dd, Throwable throwable) {
        if(enableLog)
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
            ExifInterface exif = new ExifInterface(getInputStream(file));
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
        if(enableLog)
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
        }finally {
            try {
                stream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
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
