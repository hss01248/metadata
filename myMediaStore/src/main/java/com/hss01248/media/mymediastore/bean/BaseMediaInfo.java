package com.hss01248.media.mymediastore.bean;

public class BaseMediaInfo {

    public long id;

    public long folderId;

    public String name;
    /**
     * 可能是纯文件路径,或者saf拿到的content://xxxx
     */
    public String pathOrUri;

    public long fileSize;
    public long updatedTime;
    public long createdTime;

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
