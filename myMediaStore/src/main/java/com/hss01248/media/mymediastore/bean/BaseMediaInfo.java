package com.hss01248.media.mymediastore.bean;

import android.os.Build;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

import java.util.Objects;

@Entity
public class BaseMediaInfo {



    public String folderPathOrUri;

    public String name;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseMediaInfo)) return false;
        BaseMediaInfo that = (BaseMediaInfo) o;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return Objects.equals(pathOrUri, that.pathOrUri);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return Objects.hash(pathOrUri);
        }
        return 908;
    }

    /**
     * 可能是纯文件路径,或者saf拿到的content://xxxx
     */
    @Id
    public String pathOrUri;

    public long fileSize;
    public long updatedTime;

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

    public String getFolderPathOrUri() {
        return this.folderPathOrUri;
    }

    public void setFolderPathOrUri(String folderPathOrUri) {
        this.folderPathOrUri = folderPathOrUri;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPathOrUri() {
        return this.pathOrUri;
    }

    public void setPathOrUri(String pathOrUri) {
        this.pathOrUri = pathOrUri;
    }

    public long getFileSize() {
        return this.fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getUpdatedTime() {
        return this.updatedTime;
    }

    public void setUpdatedTime(long updatedTime) {
        this.updatedTime = updatedTime;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_VIDEO = 2;
    public static final int TYPE_AUDIO = 3;

    @Generated(hash = 646981729)
    public BaseMediaInfo(String folderPathOrUri, String name, String pathOrUri,
            long fileSize, long updatedTime, int type) {
        this.folderPathOrUri = folderPathOrUri;
        this.name = name;
        this.pathOrUri = pathOrUri;
        this.fileSize = fileSize;
        this.updatedTime = updatedTime;
        this.type = type;
    }

    @Generated(hash = 1446686172)
    public BaseMediaInfo() {
    }

}
