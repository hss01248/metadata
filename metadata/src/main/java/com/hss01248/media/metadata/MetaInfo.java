package com.hss01248.media.metadata;


import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.webkit.URLUtil;

import androidx.documentfile.provider.DocumentFile;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.Utils;

import java.io.File;
import java.net.URLDecoder;
import java.util.Map;
import java.util.TreeMap;

public class MetaInfo {
    FileInfo fileInfo;
    Map<String,Object> uriContent = new TreeMap<>();
    Map<String,String> fileHeaders = new TreeMap<>();

    Map<String,Object> extras = new TreeMap<>();

    public static MetaInfo parse(Uri uri){
        if(uri ==null){
            return null;
        }
        MetaInfo info = new MetaInfo();
        if("file".equals(uri.getScheme())){
            String path = URLDecoder.decode(uri.toString().substring("file://".length()));
            info.fileInfo = FileInfo.create(new File(path));

        }else if("content".equals(uri.getScheme())){
            boolean isDocumentUri = DocumentFile.isDocumentUri(Utils.getApp(), uri);
            if(isDocumentUri){
                DocumentFile documentFile = DocumentFile.fromSingleUri(Utils.getApp(),uri);
                info.fileInfo = FileInfo.create(documentFile);
                if(info.fileInfo == null){
                    info.fileInfo = new FileInfo();
                    info.fileInfo.absolutePath = URLDecoder.decode(uri.toString());
                    info.fileInfo.name = URLUtil.guessFileName(info.fileInfo.absolutePath,"","");
                    info.fileInfo.mimeTypeByExt = FileTypeUtil.getMineType(info.fileInfo.absolutePath);
                }
            }else {
                info.fileInfo = new FileInfo();
                info.fileInfo.absolutePath = URLDecoder.decode(uri.toString());
            }

            info.uriContent = ContentUriUtil.getInfos(uri);
            //file provider 或普通content provider

            fillFileInfoByContentUri(info,uri);
            if(TextUtils.isEmpty(info.fileInfo.mimeTypeByExt )){
                info.fileInfo.mimeTypeByExt = FileTypeUtil.getMineType(info.fileInfo.absolutePath);
            }
        }else if("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())){
            info.fileInfo = new FileInfo();
            info.fileInfo.absolutePath = URLDecoder.decode(uri.toString());
            info.fileInfo.name = URLUtil.guessFileName(info.fileInfo.absolutePath,"","");
            info.fileInfo.mimeTypeByExt = FileTypeUtil.getMineType(info.fileInfo.absolutePath);

            //todo 文件大小,是否存在
        }

        //内部有一次读流操作
        info.fileInfo.mimeTypeReal = FileTypeUtil.getRealMimeType(uri);
        //使用uri操作,获取文件头:
        ////内部有一次读流操作
        info.fileHeaders = FileHeaderUtil.parseHeaders(uri,info);
        if("jpg".equals(info.fileInfo.mimeTypeReal)){
            //内部有一次读流操作
            info.extras.put("jpegQuality",FileHeaderUtil.getJpegQuality(uri));
        }
        //文件真正创建时间: 从文件头里取->没有,则从文件名里取->还没有,才取lastModified
        FileHeaderUtil.parseRealCreatedTime(info);
        FileHeaderUtil.parseLocation(info);
        return info;
    }

    private static void fillFileInfoByContentUri(MetaInfo info, Uri uri) {
        if(info.uriContent != null){
            if(info.uriContent.containsKey(MediaStore.Files.FileColumns.MIME_TYPE)){
                info.fileInfo.mimeTypeByExt = info.uriContent.get(MediaStore.Files.FileColumns.MIME_TYPE)+"";
            }
            if(info.uriContent.containsKey(MediaStore.Files.FileColumns.DATA)){
                info.fileInfo.realPath = info.uriContent.get(MediaStore.Files.FileColumns.DATA)+"";
            }
            if(info.fileInfo.lastModified==0){
                if(info.uriContent.containsKey(MediaStore.Files.FileColumns.DATE_ADDED)){
                    try {
                        info.fileInfo.lastModified = Long.parseLong(info.uriContent.get(MediaStore.Files.FileColumns.DATE_ADDED)+"")*1000;
                    }catch (Throwable throwable){
                        LogUtils.w(throwable);
                    }
                }
            }
            if(info.fileInfo.length <=0){
                if(info.uriContent.containsKey(MediaStore.Files.FileColumns.SIZE)){
                    try {
                        info.fileInfo.length = Long.parseLong(info.uriContent.get(MediaStore.Files.FileColumns.SIZE)+"");
                    }catch (Throwable throwable){
                        LogUtils.w(throwable);
                    }
                }
            }
            if(TextUtils.isEmpty(info.fileInfo.name)){
                info.fileInfo.name = info.uriContent.get(MediaStore.Files.FileColumns.DISPLAY_NAME)+"";
            }
        }
    }


    public static class  FileInfo{
        public String name;
        public long lastModified;
        public long realCreatedTime;
        public String absolutePath;
        public String realPath;
        public Boolean canRead;
        public Boolean canWrite;
        public long length;
        public Boolean isDir;
        public Boolean exist;
        public String mimeTypeByExt;
        public String mimeTypeReal;
        //public Uri uri;



        public static FileInfo create(File file){
            if(file == null){
                return null;
            }
            FileInfo info = new FileInfo();
            info.absolutePath = file.getAbsolutePath();
            info.lastModified = file.lastModified();
            info.name = file.getName();

            info.length = file.length();
            info.isDir = file.isDirectory();
            //info.uri = Uri.fromFile(file);
            info.exist = file.exists();
            info.canRead = file.canRead();
            info.canWrite = file.canWrite();
            info.mimeTypeByExt = FileTypeUtil.getMineType(file.getAbsolutePath());

            return info;
        }

        public static FileInfo create(DocumentFile file){
            if(file == null){
                return null;
            }
            FileInfo info = new FileInfo();
            info.absolutePath = file.getUri().toString();
            info.lastModified = file.lastModified();
            info.name = file.getName();

            info.length = file.length();
            info.isDir = file.isDirectory();
            //info.uri = file.getUri();
            info.exist = file.exists();
            info.canRead = file.canRead();
            info.canWrite = file.canWrite();

            info.mimeTypeByExt = FileTypeUtil.getMineType(info.absolutePath);
            return info;
        }
    }
}
