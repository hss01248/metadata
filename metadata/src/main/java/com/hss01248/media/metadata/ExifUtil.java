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
import android.util.TimeUtils;

import com.hss01248.media.metadata.quality.Magick;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
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
        map.put("00-path",path);
        map.put("0-wh",wh);
        map.put("0-jpg-quality",quality);
        map.put("0-fileSize",fileSize);
        String str =  map.toString().replaceAll(",","\n");
        return str;
    }

    public static Map<String,String> getBasicMap(InputStream inputStream){
        String fileSize = "";
        String wh = "";
        String quality = "";
        String path = "";
        String type = "";
        Map<String,String> map = new TreeMap<>();
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
            type = FileTypeUtil.getType(inputStream);

            if("jpg".equals(type)){
                quality = new Magick().getJPEGImageQuality(new ByteArrayInputStream(bytes))+"";
                map.put("0-jpg-quality",quality);
            }
            if(FileTypeUtil.getMimeByType(type).contains("image")){
                wh = formatWh(new ByteArrayInputStream(bytes));
                map.put("0-wh",wh);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        //for (Map.Entry<String, String> stringStringEntry : map.entrySet()) {
        //stringStringEntry.setValue(stringfySomeTag(stringStringEntry.getKey(),stringStringEntry.getValue()));
        // }
        map.put("00-path",path);
        map.put("00-realType",type);
        map.put("0-fileSize",fileSize);
        return map;
    }

    static SimpleDateFormat format;
    public static Map<String,String> getBasicMap(String filePath){
        String fileSize = "";
        String wh = "";
        String quality = "";
        String path = "";
        String type = "";
        /*if(filePath.startsWith("content://")){

        }*/
        File file = new File(filePath);
        Map<String,String> map = new TreeMap<>();
        try {
            fileSize = formatFileSize(file.length());
            path = filePath;



            type = FileTypeUtil.getType(file);

            if("jpg".equals(type)){
                quality = new Magick().getJPEGImageQuality(new FileInputStream(file))+"";
                map.put("0-jpg-quality",quality);
                /*String tail = readJpgTail(filePath);
                map.put("0-jpg-tail",tail);*/
            }
            if(FileTypeUtil.getMimeByType(type).contains("image")){
                wh = formatWh(new FileInputStream(file));
                map.put("0-wh",wh);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        //for (Map.Entry<String, String> stringStringEntry : map.entrySet()) {
        //stringStringEntry.setValue(stringfySomeTag(stringStringEntry.getKey(),stringStringEntry.getValue()));
        // }
        map.put("00-path",path);
        map.put("00-realType",type);
        map.put("0-fileSize",fileSize);
        if(format ==  null){
            format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
        map.put("0-lastModified", format.format(new Date(file.lastModified())));
        return map;
    }





    private static String getPathFromStream(InputStream inputStream) {
        try {
            if(inputStream instanceof FileInputStream){
                //Accessing hidden field Ljava/io/FileInputStream;->path:Ljava/lang/String; (greylist-max-o, reflection, denied)
//java.lang.NoSuchFieldException: No field path in class Ljava/io/FileInputStream; (declaration of 'java.io.FileInputStream' appears in /apex/com.android.runtime/javalib/core-oj.jar)
                Class clazz = FileInputStream.class;
                Field field = clazz.getDeclaredField("path");
                field.setAccessible(true);
              String path = (String) field.get(inputStream);
              return path;
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

            exifMap.put("0-jpg-quality",new Magick().getJPEGImageQuality(new FileInputStream(file))+"");
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

     static String stringfySomeTag(String tag, String val) {
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

    private static boolean appJpgTail(String path,String text) {


        Log.w("mark","write mark in end:"+text);
        //UTF-8 编码中，一个英文字为一个字节，一个中文为三个字节。
        byte[] bytes = text.getBytes(Charset.forName("UTF-8"));
        File file = new File(path);
        try {
            RandomAccessFile accessFile = new RandomAccessFile(file,"rw");
            accessFile.seek(file.length());
            accessFile.write(bytes);
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String readJpgTail(String path){
        try {
            appJpgTail(path,"i am jpg tail-->random num:"+new Random().nextInt(1000));
            String last2Bytes = readTail2Byte2Hex(path);
            Log.d("jpg","last 2:"+last2Bytes);
            if("FFD9".equals(last2Bytes)){
                return "";
            }
            //逆向读文件
            RandomAccessFile in = new RandomAccessFile(path, "r");
            int pre = 0;
            int current;
            long jpgEndIdx = -1;

            for(long p = in.length() - 1; p >= 0; p--) {
                in.seek(p);
                current = in.read();
                //Log.i("jpg","hex:-->"+byteToHex((byte) current));
                if(pre == 0){
                    pre = current;
                    continue;
                }
                if(byteToHex((byte) pre).toUpperCase().endsWith("D9")){
                    if(byteToHex((byte) current).toUpperCase().endsWith("FF")){
                        jpgEndIdx = p;
                        Log.v("jpg","file tail ffd9:"+jpgEndIdx+",file lenth:"+new File(path).length());
                        break;
                    }
                }
                pre = current;
            }
            in.seek(jpgEndIdx+2);
            long tailLen = in.length() - jpgEndIdx-2;
            byte[] bytes = new byte[(int) tailLen];
            in.read(bytes);
            String str = new String(bytes,"UTF-8");
            //Log.w("mark","last  bytes in end:"+str);
            return str;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private static String  readTail2Byte2Hex(String result) {
        byte[] bytes = new byte[2];
        File file = new File(result);
        try {
            RandomAccessFile accessFile = new RandomAccessFile(file,"rw");
            accessFile.seek(file.length()-2);
            accessFile.read(bytes);
           return bytes2HexString(bytes,true);
        } catch (Throwable e) {
           return "";
        }
    }

    private static final char[] HEX_DIGITS_UPPER =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private static final char[] HEX_DIGITS_LOWER =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static String bytes2HexString(final byte[] bytes, boolean isUpperCase) {
        if (bytes == null) return "";
        char[] hexDigits = isUpperCase ? HEX_DIGITS_UPPER : HEX_DIGITS_LOWER;
        int len = bytes.length;
        if (len <= 0) return "";
        char[] ret = new char[len << 1];
        for (int i = 0, j = 0; i < len; i++) {
            ret[j++] = hexDigits[bytes[i] >> 4 & 0x0f];
            ret[j++] = hexDigits[bytes[i] & 0x0f];
        }
        return new String(ret);
    }




    public static String byteToHex(byte b){
        String hex = Integer.toHexString(b & 0xFF);
        if(hex.length() < 2){
            hex = "0" + hex;
        }
        return hex;
    }

}
