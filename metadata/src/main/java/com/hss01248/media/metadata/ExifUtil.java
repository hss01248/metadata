package com.hss01248.media.metadata;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.exifinterface.media.ExifInterface;

import android.graphics.Rect;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.blankj.utilcode.util.LogUtils;
import com.hss01248.media.metadata.quality.Magick;

import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;


public class ExifUtil {

   public static  byte dataToAdd = 0x66;
    static Context context;
   public static boolean enableLog;
    public static void init(Context context){
        ExifUtil.context = context;
    }

    public static Map<String,String> readExif(InputStream inputStream){
       return readExif(inputStream,true);
    }

    public static void copyExif(String from, String to){
        long start = System.currentTimeMillis();
        writeExif(readExif(from),to);
        LogUtils.d("copyExif finished: cost ms: "+(System.currentTimeMillis() - start));

        //拷贝缩略图
       /* try {
            ExifInterface exifInterface = new ExifInterface(from);
            byte[] thumbnailBytes = exifInterface.getThumbnailBytes();
            //exifInterface.th
        } catch (IOException e) {
           LogUtils.w(e);
        }*/
        //copyMotionPhotoJpegTail(from, to);

    }

    public static void copyJpegTail(String from, String to){
        long start = System.currentTimeMillis();
        byte[] jpgTail = ExifUtil.getJpgTail(from);
        if(jpgTail !=null){
            try {
                FileOutputStream outputStream = new FileOutputStream(to, true); // set append to true
                outputStream.write(jpgTail); // write byte array to file
                outputStream.close();
                LogUtils.i("Data appended to file successfully."+to);
            } catch (Exception e) {
                LogUtils.w("Error while appending data to file: " + e.getMessage());
            }
            LogUtils.d("copyJpegTail : cost ms: "+(System.currentTimeMillis() - start));
        }
    }

    public static  void copyMotionPhotoJpegTail(String from, String to){
        long start = System.currentTimeMillis();
        byte[] jpgTail = ExifUtil.getMotionPhotoJpgTail(from);
        if(jpgTail !=null){
            try {
                FileOutputStream outputStream = new FileOutputStream(to, true); // set append to true
                outputStream.write(jpgTail); // write byte array to file
                outputStream.close();
                LogUtils.i("Data appended to file successfully."+to);
            } catch (Exception e) {
                LogUtils.w("Error while appending data to file: " + e.getMessage());
            }
            LogUtils.d("copyMotionPhoto JpegTail : cost ms: "+(System.currentTimeMillis() - start));
        }
    }

    public static String getExifStr(InputStream inputStream){
        String fileSize = "";
        String wh = "";
        String quality = "";
        String path = "";
        try {
             fileSize = formatFileSize(inputStream.available());
             path = getPathFromStream(inputStream);

            byte[] bytes = decryptedInputStream(inputStream);

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

            byte[] bytes  = decryptedInputStream(inputStream);

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


    public static byte[] decryptedInputStream(InputStream inputStream){
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        long count = 0;
        int n = 0;
        try{
            while (-1 != (n = inputStream.read(buffer))) {
                output.write(buffer, 0, n);
                count += n;
            }
            byte[] bytes = output.toByteArray();
            FileTypeUtil.close(inputStream);
            if(bytes[0] == dataToAdd){
               //return  bytes.;
                byte[] subArray = new byte[bytes.length-1];
                System.arraycopy(bytes, 1, subArray, 0, bytes.length);
                return subArray;
            }else{
               return bytes;
            }
        }catch (Throwable throwable){
            LogUtils.w(throwable);
            return  null;
        }finally {
            FileTypeUtil.close(inputStream);
        }

    }

    public static InputStream fileInputStream(File file){
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            boolean encrypted = inputStream.read() == dataToAdd;
            if(encrypted){
                return  inputStream;
            }else {
                FileTypeUtil.close(inputStream);
                return new FileInputStream(file);
            }
           //
        } catch (Exception e) {
            LogUtils.e(file.getAbsolutePath(),e);
            return null;
        }

    }

    static SimpleDateFormat format;
    public static Map<String,String> getBasicMap(String filePath){
        String fileSize = "";
        String wh = "";
        String quality = "";
        String path = filePath;
        String type = "";
        /*if(filePath.startsWith("content://")){

        }*/
        File file = new File(filePath);
       // FileInputStream inputStream = new FileInputStream(file);

        Map<String,String> map = new TreeMap<>();
        try {
            FileInputStream inputStream = new FileInputStream(file);
            boolean encrypted = inputStream.read() == dataToAdd;
            FileTypeUtil.close(inputStream);

            map.put("0-encrypted",encrypted+"");

            fileSize = formatFileSize(file.length());
            path = filePath;



            type = FileTypeUtil.getType(file);

            if("jpg".equals(type)){
                quality = new Magick().getJPEGImageQuality(fileInputStream(file))+"";
                map.put("0-jpg-quality",quality);
                /*String tail = readJpgTail(filePath);
                map.put("0-jpg-tail",tail);*/
            }
            if(FileTypeUtil.getMimeByType(type).contains("image")){
                wh = formatWh(fileInputStream(file));
                map.put("0-wh",wh);
            }
        } catch (Throwable e) {
            LogUtils.w(filePath,e);
        }

        //for (Map.Entry<String, String> stringStringEntry : map.entrySet()) {
        //stringStringEntry.setValue(stringfySomeTag(stringStringEntry.getKey(),stringStringEntry.getValue()));
        // }
        map.put("00-path",filePath);
        map.put("00-realType",type);
        map.put("0-fileSize",fileSize);
        if(format ==  null){
            format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
        map.put("0-lastModified", format.format(new Date(file.lastModified())));
        return map;
    }





    @Deprecated
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

            exifMap.put("0-wh",formatWh(fileInputStream(file)));

            exifMap.put("0-jpg-quality",new Magick().getJPEGImageQuality(fileInputStream(file))+"");
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
            return readExif(fileInputStream(new File(path)),true);
        } catch (Exception e) {
            e.printStackTrace();
            return new TreeMap<>();
        }
    }



    public static Map<String,String> readExif(InputStream inputStream,boolean close){
        Map<String,String> exifMap = new TreeMap<>();
        try {
            byte[] bytes = decryptedInputStream(inputStream);
            ExifInterface exif = new ExifInterface(new ByteArrayInputStream(bytes));
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
            FileTypeUtil.close(inputStream);
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
                exif.setAttribute(ExifInterface.TAG_DATETIME,String.format("YYYY-MM-DD HH:mm:SS",System.currentTimeMillis()));
            }
            exif.saveAttributes();
        }catch (Throwable e){
           LogUtils.w(e);
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

    public static long getVideoLength(String path){
        Map<String, String> stringStringMap = readExif(path);
        String xmp = stringStringMap.get("Xmp");
        if(TextUtils.isEmpty(xmp)){
            return 0;
        }
/*        boolean isMotionPhoto = xmp.contains("MotionPhoto=\"1\"");
        if(!isMotionPhoto){
            return 0;
        }*/
        int index = xmp.indexOf("Item:Semantic=\"MotionPhoto\"");
        if(index<0){
            return 0;
        }
        String str = xmp.substring(index+"Item:Semantic=\"MotionPhoto\"".length()).trim();
        if(str.startsWith("\n")){
            str = str.substring(1);
        }
        if(str.startsWith("Item:Length=\"")){
            str = str.substring("Item:Length=\"".length());

            String time = str.substring(0,str.indexOf("\""));
            LogUtils.w("MotionPhoto video length: "+time);
            try {
                return  Long.parseLong(time);
            }catch (Throwable throwable){
                throwable.printStackTrace();
            }
        }
        return 0;
    }





    /**
     *
     * <x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="Adobe XMP Core 5.1.0-jc003">
     *       <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
     *         <rdf:Description rdf:about=""
     *             xmlns:GCamera="http://ns.google.com/photos/1.0/camera/"
     *             xmlns:Container="http://ns.google.com/photos/1.0/container/"
     *             xmlns:Item="http://ns.google.com/photos/1.0/container/item/"
     *             xmlns:xmpNote="http://ns.adobe.com/xmp/note/"
     *           GCamera:MotionPhoto="1"
     *           GCamera:MotionPhotoVersion="1"
     *           GCamera:MotionPhotoPresentationTimestampUs="857949"
     *           xmpNote:HasExtendedXMP="9F6C5546DA50BD17DCB8DD1604C96BE6">
     *           <Container:Directory>
     *             <rdf:Seq>
     *               <rdf:li rdf:parseType="Resource">
     *                 <Container:Item
     *                   Item:Mime="image/jpeg"
     *                   Item:Semantic="Primary"
     *                   Item:Length="0"
     *                   Item:Padding="0"/>
     *               </rdf:li>
     *               <rdf:li rdf:parseType="Resource">
     *                 <Container:Item
     *                   Item:Mime="video/mp4"
     *                   Item:Semantic="MotionPhoto"
     *                   Item:Length="482189"
     *                   Item:Padding="0"/>
     *               </rdf:li>
     *             </rdf:Seq>
     *           </Container:Directory>
     *         </rdf:Description>
     *       </rdf:RDF>
     *     </x:xmpmeta>
     *
     *     语法: https://www.itpow.com/c/2010/07/8N0MHOUN27V0R3JB.asp
     * @param xmlString
     * @return
     */
    public static long getVideoLengthWithXPath(String xmlString) {
        try {
            XPathFactory xpathFactory = XPathFactory.newInstance();
            XPathExpression expr = xpathFactory.newXPath().compile("//Container:Item[@Item:Mime='video/mp4']");///@Item:Length
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xmlString));
            Node result = (Node) expr.evaluate(builder.parse(is), XPathConstants.NODE);
            if (result != null) {
                long length = Long.parseLong(result.getNodeValue());
                System.out.println("Video length: " + length + " bytes");
                return length;
            }
        } catch (Exception e) {
            System.err.println("Error selecting video length with XPath: " + e.getMessage());
        }
        return 0;
    }

    public static byte[] getMotionPhotoJpgTail(String path){
        long videoLength = ExifUtil.getVideoLength(path);
        LogUtils.i("video length: "+ videoLength);
        if(videoLength<=0 ){
            return null;
        }
        File file = new File(path);
        RandomAccessFile raf = null;
        byte[] bytes = new byte[(int) videoLength];
        try {
            raf = new RandomAccessFile(file, "r");
            long fileLength = file.length();
            raf.seek(fileLength - videoLength);
            raf.readFully(bytes);
            //String s = ExifUtil.bytes2HexString(bytes, true);
            //LogUtils.w("read index:"+raf.getFilePointer()+","+ file.length()+","+s);

            // 然后继续读前面的,直到ffd9:
            byte[] tail = null;
            raf.seek(fileLength - videoLength - 2); // Set the pointer to the second-to-last byte of the file
            while (raf.getFilePointer() >= 0) {
                byte b1 = raf.readByte();
                byte b2 = raf.readByte();
                //LogUtils.v("bytes: 0x"+byteToHex(b1)+byteToHex(b2)+", index: 倒数第 "+(fileLength - raf.getFilePointer()));
                if (b1 == (byte) 0xFF && b2 == (byte) 0xD9) {
                    if(fileLength - videoLength == raf.getFilePointer()){
                        LogUtils.d("普通jpg,MotionPhoto之前, 以0xFFD9结尾,没有尾部隐藏信息: " + path);
                        break;
                    }
                    tail = new byte[(int) (fileLength - raf.getFilePointer() - 2)];
                    raf.read(tail, 0, tail.length);
                    break;
                }
                raf.seek(raf.getFilePointer() - 3); // Move the pointer backwards by 2 bytes
            }
            if(tail != null){
                LogUtils.i("jpg tail : 尾部隐藏信息 : " + tail.length+"B,"+bytes2HexString(tail,true));
            }
            raf.close();
        } catch (Exception e) {
            LogUtils.w(e);
        }
        return bytes;
    }

    /**
     * 不能仅凭ffd9来判断
     * @param filePath
     * @return
     */
    public static byte[] getJpgTail(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            LogUtils.w("File path cannot be empty or null");
            return null;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            LogUtils.w("File does not exist: " + filePath);
            return null;
        }
        if(file.isDirectory()){
            LogUtils.w("File isDirectory : " + filePath);
            return null;
        }
        //todo 判断是否为jpg,如果不是,直接return:  0xFFD8

        boolean isJpeg = isJpegFile(filePath);

        if(!isJpeg){
            LogUtils.w("不是jpg文件 : " + filePath);
            return null;
        }
        LogUtils.w("是jpg文件,开始读尾部 : " + filePath);
        byte[] tail = null;
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            long fileLength = file.length();
            raf.seek(fileLength - 2); // Set the pointer to the second-to-last byte of the file
            while (raf.getFilePointer() >= 0) {
                byte b1 = raf.readByte();
                byte b2 = raf.readByte();
                //LogUtils.v("bytes: 0x"+byteToHex(b1)+byteToHex(b2)+", index: 倒数第 "+(fileLength - raf.getFilePointer()));
                if (b1 == (byte) 0xFF && b2 == (byte) 0xD9) {
                    if(fileLength == raf.getFilePointer()){
                        LogUtils.d("普通jpg,以0xFFD9结尾,没有尾部隐藏信息: " + filePath);
                        return null;
                    }
                    tail = new byte[(int) (fileLength - raf.getFilePointer() - 2)];
                    raf.read(tail, 0, tail.length);
                    break;
                }
                raf.seek(raf.getFilePointer() - 3); // Move the pointer backwards by 2 bytes
            }
            raf.close();
        } catch (Exception e) {
            LogUtils.w("Error reading file: " + filePath,e);
            return null;
        }
        if(tail !=null){
            LogUtils.i("jpg tail : 尾部隐藏信息 : " + tail.length+"B,"+bytes2HexString(tail,true));

        }
        return tail;
    }

    private static boolean isJpegFile(String filePath) {
        if(TextUtils.isEmpty(filePath)){
            return false;
        }
        File file = new File(filePath);
        if(!file.exists()){
            return false;
        }
        if(file.isDirectory()){
            return false;
        }
        RandomAccessFile raf0 = null;
        try {
            raf0 = new RandomAccessFile(file, "r");
            byte a1 = raf0.readByte();
            byte a2 = raf0.readByte();
            if(a1 == (byte) 0xFF && a2 == (byte) 0xD8){
                return true;
            }
            return false;
        } catch (Exception e) {
            LogUtils.w(e);
        }finally {
            if(raf0 !=null){
                try {
                    raf0.close();
                } catch (IOException e) {
                   LogUtils.w(e);
                }
                ;
            }
        }

        return false;
    }


}
