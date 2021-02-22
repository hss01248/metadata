package com.hss01248.media.mymediastore.bean;

public class BaseMediaFolderInfo {

    public long id;

    public String name;
    /**
     * 可能是纯文件路径,或者saf拿到的content://xxxx
     */
    public String cover;
    /**
     * 可能是纯文件路径,或者saf拿到的content://xxxx
     */
    public String pathOrUri;


    public int count;
    public long fileSize;
    public int hidden;//1: true 0 :false
    public long updatedTime;

    /**
     * 排序的序号.用于置顶功能
     */
    public int order;

    public int type;

    public boolean isImage(){
        return type == TYPE_IMAGE;
    }

    public boolean isVideo(){
        return type == TYPE_VIDEO;
    }

    public boolean isAudio(){
        return type == TYPE_AUDIO;
    }




    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_VIDEO = 2;
    public static final int TYPE_AUDIO = 3;
}
