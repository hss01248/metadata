package com.hss01248.media.mymediastore;

import com.hss01248.media.mymediastore.bean.BaseMediaFolderInfo;

import java.util.List;

public interface ScanFolderCallback {

    void onComplete();

    void onFromDB(List<BaseMediaFolderInfo> folderInfos);

    /**
     * 可能有图片,音视频
     * @param folderInfos
     */
    void onScanEachFolder(List<BaseMediaFolderInfo> folderInfos);


    /**
     * 可能会回调两次:
     * file扫描完成一次
     * saf扫描完成一次
     * @param folderInfos
     */
    void onScanFinished(List<BaseMediaFolderInfo> folderInfos);


}
