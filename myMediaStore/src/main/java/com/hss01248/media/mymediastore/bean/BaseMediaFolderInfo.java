package com.hss01248.media.mymediastore.bean;

import android.os.Build;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

import java.util.Objects;

import static com.hss01248.media.mymediastore.bean.BaseMediaInfo.TYPE_AUDIO;
import static com.hss01248.media.mymediastore.bean.BaseMediaInfo.TYPE_IMAGE;
import static com.hss01248.media.mymediastore.bean.BaseMediaInfo.TYPE_VIDEO;

@Entity
public class BaseMediaFolderInfo {


    public String name;
    /**
     * 可能是纯文件路径,或者saf拿到的content://xxxx
     */
    public String cover;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseMediaFolderInfo)) return false;
        BaseMediaFolderInfo that = (BaseMediaFolderInfo) o;
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
        return 9090;
    }

    /**
     * 可能是纯文件路径,或者saf拿到的content://xxxx
     */
    @Id
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

    @Generated(hash = 210610148)
    public BaseMediaFolderInfo(String name, String cover, String pathOrUri,
            int count, long fileSize, int hidden, long updatedTime, int order,
            int type) {
        this.name = name;
        this.cover = cover;
        this.pathOrUri = pathOrUri;
        this.count = count;
        this.fileSize = fileSize;
        this.hidden = hidden;
        this.updatedTime = updatedTime;
        this.order = order;
        this.type = type;
    }

    @Generated(hash = 1055136609)
    public BaseMediaFolderInfo() {
    }

    public boolean isImage(){
        return type == TYPE_IMAGE;
    }

    public boolean isVideo(){
        return type == TYPE_VIDEO;
    }

    public boolean isAudio(){
        return type == TYPE_AUDIO;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCover() {
        return this.cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getPathOrUri() {
        return this.pathOrUri;
    }

    public void setPathOrUri(String pathOrUri) {
        this.pathOrUri = pathOrUri;
    }

    public int getCount() {
        return this.count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public long getFileSize() {
        return this.fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public int getHidden() {
        return this.hidden;
    }

    public void setHidden(int hidden) {
        this.hidden = hidden;
    }

    public long getUpdatedTime() {
        return this.updatedTime;
    }

    public void setUpdatedTime(long updatedTime) {
        this.updatedTime = updatedTime;
    }

    public int getOrder() {
        return this.order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }


}
