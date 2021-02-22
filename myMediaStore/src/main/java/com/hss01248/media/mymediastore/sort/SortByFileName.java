package com.hss01248.media.mymediastore.sort;

import com.hss01248.media.mymediastore.ISort;
import com.hss01248.media.mymediastore.bean.BaseMediaFolderInfo;


import org.greenrobot.greendao.query.QueryBuilder;

import java.util.Comparator;

public class SortByFileName implements ISort<BaseMediaFolderInfo> {


    @Override
    public QueryBuilder<BaseMediaFolderInfo> orderBy(QueryBuilder<BaseMediaFolderInfo> builder) {
        return null;
    }

    @Override
    public Comparator<BaseMediaFolderInfo> getListSort() {
        return null;
    }
}
