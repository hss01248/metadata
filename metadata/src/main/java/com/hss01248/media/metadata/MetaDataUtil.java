package com.hss01248.media.metadata;

import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import com.google.gson.GsonBuilder;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetaDataUtil {

    @Deprecated
    public static String getExifStr(String path) {
        Map<String, String> map = getAllInfo(path);
        return map.toString().replaceAll(",", "\n");
    }

    public static String getDes(String path) {
        if(path.startsWith("content://")){
            return getDes2(Uri.parse(path));
        }
        Map<String, String> map = getMetaData( path);
        String xml = "";
        if(map.containsKey(ExifInterface.TAG_XMP)){
            xml = map.get(ExifInterface.TAG_XMP);
        }
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(map)+"\n"+xml;
    }
    public static String getDes2(Uri uri){
        MetaInfo info =  getMetaData2( uri);
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(getMetaData2(uri))
                +"\n"+info.getXml();
    }

    public static MetaInfo getMetaData2(Uri uri){
        return MetaInfo.parse(uri);
    }
    @Deprecated
    public static Map<String, String> getMetaData(String path) {
        String mimetype = FileTypeUtil.getMineType(path);
        Map<String, String> data = new TreeMap<>();

        try {
            data.putAll(ExifUtil.getBasicMap(path));
        } catch (Exception e) {
            e.printStackTrace();
        }
        data.put("00-ext", FileTypeUtil.extName(path));
        if (TextUtils.isEmpty(data.get("00-path"))) {
            data.put("00-path", path);
        }

        if (mimetype.contains("image")) {
            data.putAll(ExifUtil.readExif(path));
            for (Map.Entry<String, String> stringStringEntry : data.entrySet()) {
                stringStringEntry.setValue(ExifUtil.stringfySomeTag(stringStringEntry.getKey(), stringStringEntry.getValue()));
            }

        } else if (mimetype.contains("video") || mimetype.contains("audio")) {
            data.putAll(getAllInfo(path));
        } else if ("gz".equals(FileTypeUtil.getType(new File(path)))) {
            //解压缩
        }
        return data;

    }


    /**
     * 可用于音频和视频,不能用于图片
     *
     * @param path
     * @return
     */
    @Deprecated
    public static Map<String, String> getAllInfo(String path) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        Field[] fields = MediaMetadataRetriever.class.getDeclaredFields();
        Map<String, String> metadatas = new TreeMap<>();
        String key = "METADATA_KEY_";
        try {
            //没有权限时,crash
            retriever.setDataSource(path);
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
            throwable.printStackTrace();
        }
        return metadatas;
    }

    /**
     * 优先级: 首先从文件头里取
     * 然后从文件名里猜
     * 最后都没有,才取文件系统的lastModified
     *
     * @param path
     * @return
     */
    public static long getMediaCreateTime(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return 0;
        }
        if (file.isDirectory()) {
            return 0;
        }
        String mimetype = FileTypeUtil.getMineType(path);
        if (mimetype.contains("image")) {
            Map<String, String> map = ExifUtil.readExif(path);
            //DateTime
            long timeFromExif = timeFromExif(map);
            if(timeFromExif > 0){
                return timeFromExif;
            }
        }else if(mimetype.contains("video") || mimetype.contains("audio")){
            Map<String, String> map = getAllInfo(path);
            long timeFromExif = timeFromMeta(map);
            if(timeFromExif > 0){
                return timeFromExif;
            }
        }
        long timeFromFileName = timeGuessFromFileName(file.getName());
        if(timeFromFileName > 0){
            return timeFromFileName;
        }
        return file.lastModified();
    }

    static SimpleDateFormat sdfFile = new SimpleDateFormat("yyyyMMddHHmmss");
    public static long timeGuessFromFileName(String name) {
        //VID_20210302_16304170-xxxx.mp4
        //Screenshot_20190619_105917_包名.jpg/png
        //20190619_105917.jpg
        //2019-06-19_10-59-17_2545184214851555.jpg
        //2019-06-19-10-59-17-xxxx.jpg
        //IMG_20190619_105917.jpg
        //数字19或20开头, 然后匹配
        if(!name.contains("19") && !name.contains("20")){
            return 0;
        }
        name = name.replaceAll("-","");
        name = name.replaceAll("_","");
        Log.d("before",name);

        String fengli="[20,19]\\d{13}";//提取日期的正则表达式
        Pattern pafengli=Pattern.compile(fengli);
        Matcher matfengli = pafengli.matcher(name);
        if(matfengli.find()) {
           String cutstr =matfengli.group();  //group为捕获组
            Log.i("find",cutstr);
            return parseTime(cutstr,sdfFile);

        } else{
            Log.w("not find",name);
        }
        return 0;
    }

    private static long parseTime(String cutstr, SimpleDateFormat sdfFile) {
        Date date = null;
        try {
            date = sdfFile.parse(cutstr);
            if(date != null){
                return date.getTime();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    static SimpleDateFormat sdfVideo = new SimpleDateFormat("yyyyMMddHHmmss");
    /**
     * METADATA_KEY_DATE=5
     * @param map
     * @return
     */
     static long timeFromMeta(Map<String, String> map) {
        if(map == null || !map.containsKey("date")){
            return 0;
        }
        String dateStr = map.get("date");
        if(TextUtils.isEmpty(dateStr)){
            return 0;
        }
        //.000Z
        if(dateStr.contains(".")){
            String timeZone = dateStr.substring(dateStr.indexOf(".")+1);
            Log.i("timezone",timeZone);
            dateStr = dateStr.substring(0,dateStr.indexOf("."));
        }
        dateStr = dateStr.replace("T","");
        Date date = null;
        try {
            date = sdfVideo.parse(dateStr);
            if(date != null){
                return date.getTime();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return 0;
    }


    // Pattern to check date time primary format (e.g. 2020:01:01 00:00:00)
    private static final Pattern DATETIME_PRIMARY_FORMAT_PATTERN =
            Pattern.compile("^(\\d{4}):(\\d{2}):(\\d{2})\\s(\\d{2}):(\\d{2}):(\\d{2})$");
    // Pattern to check date time secondary format (e.g. 2020-01-01 00:00:00)
    private static final Pattern DATETIME_SECONDARY_FORMAT_PATTERN =
            Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})\\s(\\d{2}):(\\d{2}):(\\d{2})$");
    private static final int DATETIME_VALUE_STRING_LENGTH = 19;

    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

     static long timeFromExif(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return 0;
        }
        for (String tag : map.keySet()) {
            if (ExifInterface.TAG_DATETIME.equals(tag) || ExifInterface.TAG_DATETIME_ORIGINAL.equals(tag)
                    || ExifInterface.TAG_DATETIME_DIGITIZED.equals(tag)) {
                String value = map.get(tag);
                if (value != null) {
                    boolean isPrimaryFormat = DATETIME_PRIMARY_FORMAT_PATTERN.matcher(value).find();
                    boolean isSecondaryFormat = DATETIME_SECONDARY_FORMAT_PATTERN.matcher(value).find();
                    // Validate
                    if (value.length() != DATETIME_VALUE_STRING_LENGTH
                            || (!isPrimaryFormat && !isSecondaryFormat)) {
                        Log.w("exif", "Invalid value for " + tag + " : " + value);
                        continue;
                    }
                    // If datetime value has secondary format (e.g. 2020-01-01 00:00:00), convert it to
                    // primary format (e.g. 2020:01:01 00:00:00) since it is the format in the
                    // official documentation.
                    // See JEITA CP-3451C Section 4.6.4. D. Other Tags, DateTime
                    if (isSecondaryFormat) {
                        // Replace "-" with ":" to match the primary format.
                        value = value.replaceAll("-", ":");
                    }
                    //YYYY:MM:DD HH:MM:SS
                    Date date = null;
                    try {
                        date = sdf.parse(value);
                        if(date != null){
                            return date.getTime();
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return 0;
    }
}
