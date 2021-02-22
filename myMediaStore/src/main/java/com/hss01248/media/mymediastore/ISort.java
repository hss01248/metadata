package com.hss01248.media.mymediastore;

import com.hss01248.media.mymediastore.bean.BaseMediaFolderInfo;

import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.query.QueryBuilder;

import java.util.Comparator;

public interface ISort<T> {

    QueryBuilder<T> orderBy(QueryBuilder<T> builder);

    Comparator<T> getListSort();
}
