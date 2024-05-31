package com.hss01248.media.metadata;


import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * 文件类型判断工具类
 *
 * <p>此工具根据文件的前几位bytes猜测文件类型，对于文本、zip判断不准确，对于视频、图片类型判断准确</p>
 *
 * <p>需要注意的是，xlsx、docx等Office2007格式，全部识别为zip，因为新版采用了OpenXML格式，这些格式本质上是XML文件打包为zip</p>
 *
 * @author Looly
 */
public class FileTypeUtil {

    private static final Map<String, String> FILE_TYPE_MAP;

    static {
        FILE_TYPE_MAP = new ConcurrentSkipListMap<>((s1, s2) -> {
            int len1 = s1.length();
            int len2 = s2.length();
            if (len1 == len2) {
                return s1.compareTo(s2);
            } else {
                return len2 - len1;
            }
        });

        FILE_TYPE_MAP.put("ffd8ff", "jpg"); // JPEG (jpg)
        FILE_TYPE_MAP.put("89504e47", "png"); // PNG (png)
        FILE_TYPE_MAP.put("4749463837", "gif"); // GIF (gif)
        FILE_TYPE_MAP.put("4749463839", "gif"); // GIF (gif)
        FILE_TYPE_MAP.put("49492a00227105008037", "tif"); // TIFF (tif)
        FILE_TYPE_MAP.put("424d228c010000000000", "bmp"); // 16色位图(bmp)
        FILE_TYPE_MAP.put("424d8240090000000000", "bmp"); // 24色位图(bmp)
        FILE_TYPE_MAP.put("424d8e1b030000000000", "bmp"); // 256色位图(bmp)
        FILE_TYPE_MAP.put("41433130313500000000", "dwg"); // CAD (dwg)
        FILE_TYPE_MAP.put("7b5c727466315c616e73", "rtf"); // Rich Text Format (rtf)
        FILE_TYPE_MAP.put("38425053000100000000", "psd"); // Photoshop (psd)
        FILE_TYPE_MAP.put("46726f6d3a203d3f6762", "eml"); // Email [Outlook Express 6] (eml)
        FILE_TYPE_MAP.put("5374616E64617264204A", "mdb"); // MS Access (mdb)
        FILE_TYPE_MAP.put("252150532D41646F6265", "ps");
        FILE_TYPE_MAP.put("255044462d312e", "pdf"); // Adobe Acrobat (pdf)
        FILE_TYPE_MAP.put("2e524d46000000120001", "rmvb"); // rmvb/rm相同
        FILE_TYPE_MAP.put("464c5601050000000900", "flv"); // flv与f4v相同
        FILE_TYPE_MAP.put("0000001C66747970", "mp4");
        FILE_TYPE_MAP.put("00000020667479706", "mp4");
        FILE_TYPE_MAP.put("00000018667479706D70", "mp4");
        FILE_TYPE_MAP.put("49443303000000002176", "mp3");
        FILE_TYPE_MAP.put("000001ba210001000180", "mpg"); //
        FILE_TYPE_MAP.put("3026b2758e66cf11a6d9", "wmv"); // wmv与asf相同
        FILE_TYPE_MAP.put("52494646e27807005741", "wav"); // Wave (wav)
        FILE_TYPE_MAP.put("52494646d07d60074156", "avi");
        FILE_TYPE_MAP.put("4d546864000000060001", "mid"); // MIDI (mid)
        FILE_TYPE_MAP.put("526172211a0700cf9073", "rar"); // WinRAR
        FILE_TYPE_MAP.put("235468697320636f6e66", "ini");
        FILE_TYPE_MAP.put("504B03040a0000000000", "jar");
        FILE_TYPE_MAP.put("504B0304140008000800", "jar");
        // MS Excel 注意：word、msi 和 excel的文件头一样
        FILE_TYPE_MAP.put("d0cf11e0a1b11ae10", "xls");
        FILE_TYPE_MAP.put("504B0304", "zip");
        FILE_TYPE_MAP.put("4d5a9000030000000400", "exe"); // 可执行文件
        FILE_TYPE_MAP.put("3c25402070616765206c", "jsp"); // jsp文件
        FILE_TYPE_MAP.put("4d616e69666573742d56", "mf"); // MF文件
        FILE_TYPE_MAP.put("7061636b616765207765", "java"); // java文件
        FILE_TYPE_MAP.put("406563686f206f66660d", "bat"); // bat文件
        FILE_TYPE_MAP.put("1f8b0800000000000000", "gz"); // gz文件/gzip
        FILE_TYPE_MAP.put("cafebabe0000002e0041", "class"); // class文件
        FILE_TYPE_MAP.put("49545346030000006000", "chm"); // chm文件
        FILE_TYPE_MAP.put("04000000010000001300", "mxp"); // mxp文件
        FILE_TYPE_MAP.put("6431303a637265617465", "torrent");
        FILE_TYPE_MAP.put("6D6F6F76", "mov"); // Quicktime (mov)
        FILE_TYPE_MAP.put("FF575043", "wpd"); // WordPerfect (wpd)
        FILE_TYPE_MAP.put("CFAD12FEC5FD746F", "dbx"); // Outlook Express (dbx)
        FILE_TYPE_MAP.put("2142444E", "pst"); // Outlook (pst)
        FILE_TYPE_MAP.put("AC9EBD8F", "qdf"); // Quicken (qdf)
        FILE_TYPE_MAP.put("E3828596", "pwl"); // Windows Password (pwl)
        FILE_TYPE_MAP.put("2E7261FD", "ram"); // Real Audio (ram)
    }

    public static String getMimeByType(String typeOrSuffix){
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(typeOrSuffix);
    }

  public   static String getMineType(String filePath) {

        String type = "text/plain";
        String extension = MimeTypeMap.getFileExtensionFromUrl(filePath);
        /*try {
            type = getType(new File(filePath));
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        if (!TextUtils.isEmpty(extension)) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
        }
        if(type == null){
            type = "";
        }
        return type;


       /* MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        String mime = "text/plain";
        if (filePath != null) {
            try {
                mmr.setDataSource(filePath);
                mime = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
            } catch (IllegalStateException e) {
                return mime;
            } catch (IllegalArgumentException e) {
                return mime;
            } catch (RuntimeException e) {
                return mime;
            }
        }
        return mime;*/
    }

    public static String getMimeType(Uri uri){
        if("content".equals(uri.getScheme())){
            Map<String, Object> infos = ContentUriUtil.getInfos(uri);
            if(infos != null && infos.containsKey(MediaStore.Files.FileColumns.MIME_TYPE)){
                String mimeTypeByExt = infos.get(MediaStore.Files.FileColumns.MIME_TYPE)+"";
                if(!TextUtils.isEmpty(mimeTypeByExt) && !"null".equals(mimeTypeByExt)){
                    return mimeTypeByExt;
                }
            }
        }
        String mimetype = FileTypeUtil.getMineType(uri.toString());
        return mimetype;
    }
    public static String getRealMimeType(Uri uri){
        try {
            InputStream inputStream = FileHeaderUtil.getInputStream(uri);
            if(inputStream != null){
                try {
                    return   getType(inputStream);
                }finally {
                    inputStream.close();
                }

            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return "*/*";

    }



    /**
     * 增加文件类型映射<br>
     * 如果已经存在将覆盖之前的映射
     *
     * @param fileStreamHexHead 文件流头部Hex信息
     * @param extName           文件扩展名
     * @return 之前已经存在的文件扩展名
     */
    public static String putFileType(String fileStreamHexHead, String extName) {
        return FILE_TYPE_MAP.put(fileStreamHexHead, extName);
    }

    /**
     * 移除文件类型映射
     *
     * @param fileStreamHexHead 文件流头部Hex信息
     * @return 移除的文件扩展名
     */
    public static String removeFileType(String fileStreamHexHead) {
        return FILE_TYPE_MAP.remove(fileStreamHexHead);
    }

    /**
     * 根据文件流的头部信息获得文件类型
     *
     * @param fileStreamHexHead 文件流头部16进制字符串
     * @return 文件类型，未找到为{@code null}
     */
    public static String getType(String fileStreamHexHead) {
        for (Entry<String, String> fileTypeEntry : FILE_TYPE_MAP.entrySet()) {
            if (startWith(fileStreamHexHead, fileTypeEntry.getKey(),true,false)) {
                return fileTypeEntry.getValue();
            }
        }
        return null;
    }

    /**
     * 根据文件流的头部信息获得文件类型
     *
     * @param in {@link InputStream}
     * @return 类型，文件的扩展名，未找到为{@code null}
     */
    public static String getType(InputStream in) throws Exception {
        return getType(readHex(in,28,false));
    }
    public static String readHex(InputStream in, int length, boolean toLowerCase) throws Exception {
        return encodeHexStr(readBytes(in, length), toLowerCase);
    }
    /**
     * 用于建立十六进制字符的输出的小写字符数组
     */
    private static final char[] DIGITS_LOWER = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    /**
     * 用于建立十六进制字符的输出的大写字符数组
     */
    private static final char[] DIGITS_UPPER = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    static String encodeHexStr(byte[] data, boolean toLowerCase) {
        return new String(encodeHex(data, toLowerCase ? DIGITS_LOWER : DIGITS_UPPER));
    }



    /**
     * 读取指定长度的byte数组，不关闭流
     *
     * @param in     {@link InputStream}，为null返回null
     * @param length 长度，小于等于0返回空byte数组
     * @return bytes

     */
    public static byte[] readBytes(InputStream in, int length) throws Exception {
        if (null == in) {
            return null;
        }
        if (length <= 0) {
            return new byte[0];
        }

        byte[] b = new byte[length];
        int readLength;
        try {
            readLength = in.read(b);
            if (readLength > 0 && readLength < length) {
                byte[] b2 = new byte[readLength];
                System.arraycopy(b, 0, b2, 0, readLength);
                return b2;
            } else {
                return b;
            }
        } catch (IOException e) {
            throw new Exception(e);
        }finally {
            try {
                in.close();
            }catch (Throwable throwable){
                throwable.printStackTrace();
            }
        }
    }

    private static String encodeHexStr(byte[] data, char[] toDigits) {
        return new String(encodeHex(data, toDigits));
    }

    /**
     * 从流中读取前28个byte并转换为16进制，字母部分使用大写
     *
     * @param in {@link InputStream}
     * @return 16进制字符串
     */
    public static String readHex28Upper(InputStream in) throws Exception {
        return readHex(in, 28, false);
    }

    /**
     * 将字节数组转换为十六进制字符数组
     *
     * @param data     byte[]
     * @param toDigits 用于控制输出的char[]
     * @return 十六进制char[]
     */
    private static char[] encodeHex(byte[] data, char[] toDigits) {
        final int len = data.length;
        final char[] out = new char[len << 1];//len*2
        // two characters from the hex value.
        for (int i = 0, j = 0; i < len; i++) {
            out[j++] = toDigits[(0xF0 & data[i]) >>> 4];// 高位
            out[j++] = toDigits[0x0F & data[i]];// 低位
        }
        return out;
    }

    /**
     * 将十六进制字符转换成一个整数
     *
     * @param ch    十六进制char
     * @param index 十六进制字符在字符数组中的位置
     * @return 一个整数

     */
    private static int toDigit(char ch, int index) {
        int digit = Character.digit(ch, 16);
        if (digit < 0) {
             new Exception(String.format("Illegal hexadecimal character %s at index %d", ch+"", index)).printStackTrace();
        }
        return digit;
    }


    /**
     * 根据文件流的头部信息获得文件类型
     *
     * <pre>
     *     1、无法识别类型默认按照扩展名识别
     *     2、xls、doc、msi头信息无法区分，按照扩展名区分
     *     3、zip可能为docx、xlsx、pptx、jar、war、ofd头信息无法区分，按照扩展名区分
     * </pre>
     * @param in {@link InputStream}
     * @param filename 文件名
     * @return 类型，文件的扩展名，未找到为{@code null}
     */
    public static String getType(InputStream in, String filename) {
        String typeName = null;
        try {
            typeName = getType(in);
        } catch (Exception e) {
            e.printStackTrace();
            return extName(filename);
        }

        if (null == typeName) {
            // 未成功识别类型，扩展名辅助识别
            typeName = extName(filename);
        } else if ("xls".equals(typeName)) {
            // xls、doc、msi的头一样，使用扩展名辅助判断
            final String extName = extName(filename);
            if ("doc".equalsIgnoreCase(extName)) {
                typeName = "doc";
            } else if ("msi".equalsIgnoreCase(extName)) {
                typeName = "msi";
            }
        } else if ("zip".equals(typeName)) {
            // zip可能为docx、xlsx、pptx、jar、war、ofd等格式，扩展名辅助判断
            final String extName = extName(filename);
            if ("docx".equalsIgnoreCase(extName)) {
                typeName = "docx";
            } else if ("xlsx".equalsIgnoreCase(extName)) {
                typeName = "xlsx";
            } else if ("pptx".equalsIgnoreCase(extName)) {
                typeName = "pptx";
            } else if ("jar".equalsIgnoreCase(extName)) {
                typeName = "jar";
            } else if ("war".equalsIgnoreCase(extName)) {
                typeName = "war";
            } else if ("ofd".equalsIgnoreCase(extName)) {
                typeName = "ofd";
            }
        }
        return typeName;
    }

    /**
     * 根据文件流的头部信息获得文件类型
     *
     * <pre>
     *     1、无法识别类型默认按照扩展名识别
     *     2、xls、doc、msi头信息无法区分，按照扩展名区分
     *     3、zip可能为docx、xlsx、pptx、jar、war头信息无法区分，按照扩展名区分
     * </pre>
     *
     * @param file 文件 {@link File}
     * @return 类型，文件的扩展名，未找到为{@code null}

     */
    public static String getType(File file)  {
        InputStream in = null;
        try {
            in = ExifUtil.fileInputStream(file);
            return getType(in, file.getName());
        }catch (Throwable throwable){
            throwable.printStackTrace();
            return "";
        }finally {
            close(in);
        }
    }

    public static void close(Closeable in) {
        try {
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 通过路径获得文件类型
     *
     * @param path 路径，绝对路径或相对ClassPath的路径
     * @return 类型

     */
    public static String getTypeByPath(String path) throws Exception {
        return getType(new File(path));
    }

    /**
     * 是否以指定字符串开头<br>
     * 如果给定的字符串和开头字符串都为null则返回true，否则任意一个值为null返回false
     *
     * @param str          被监测字符串
     * @param prefix       开头字符串
     * @param ignoreCase   是否忽略大小写
     * @param ignoreEquals 是否忽略字符串相等的情况
     * @return 是否以指定字符串开头
     * @since 5.4.3
     */
     static boolean startWith(CharSequence str, CharSequence prefix, boolean ignoreCase, boolean ignoreEquals) {
        if (null == str || null == prefix) {
            if (false == ignoreEquals) {
                return false;
            }
            return null == str && null == prefix;
        }

        boolean isStartWith;
        if (ignoreCase) {
            isStartWith = str.toString().toLowerCase().startsWith(prefix.toString().toLowerCase());
        } else {
            isStartWith = str.toString().startsWith(prefix.toString());
        }

        if (isStartWith) {
            return (false == ignoreEquals) || (false == equals(str, prefix, ignoreCase));
        }
        return false;
    }

    /**
     * 比较两个字符串是否相等。
     *
     * @param str1       要比较的字符串1
     * @param str2       要比较的字符串2
     * @param ignoreCase 是否忽略大小写
     * @return 如果两个字符串相同，或者都是{@code null}，则返回{@code true}
     * @since 3.2.0
     */
    public static boolean equals(CharSequence str1, CharSequence str2, boolean ignoreCase) {
        if (null == str1) {
            // 只有两个都为null才判断相等
            return str2 == null;
        }
        if (null == str2) {
            // 字符串2空，字符串1非空，直接false
            return false;
        }

        if (ignoreCase) {
            return str1.toString().equalsIgnoreCase(str2.toString());
        } else {
            return str1.toString().contentEquals(str2);
        }
    }

    /**
     * 获得文件的扩展名（后缀名），扩展名不带“.”
     *
     * @param fileName 文件名
     * @return 扩展名
     */
     static String extName(String fileName) {
        if (fileName == null) {
            return null;
        }
        int index = fileName.lastIndexOf(".");
        if (index == -1) {
            return "";
        } else {
            String ext = fileName.substring(index + 1);
            if(ext.contains("/")){
                return "";
            }
            // 扩展名中不能包含路径相关的符号
            return  ext;
        }
    }
}

