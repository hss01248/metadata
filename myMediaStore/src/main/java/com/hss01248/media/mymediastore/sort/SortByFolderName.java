package com.hss01248.media.mymediastore.sort;

import com.hss01248.media.mymediastore.ISort;
import com.hss01248.media.mymediastore.bean.BaseMediaFolderInfo;
import com.hss01248.media.mymediastore.db.BaseMediaFolderInfoDao;

import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.query.QueryBuilder;

import java.util.Comparator;

public class SortByFolderName implements ISort<BaseMediaFolderInfo> {


    @Override
    public QueryBuilder<BaseMediaFolderInfo> orderBy(QueryBuilder<BaseMediaFolderInfo> builder) {
        return builder.orderDesc(BaseMediaFolderInfoDao.Properties.Name);
    }

    @Override
    public Comparator<BaseMediaFolderInfo> getListSort() {
      return   new Comparator<BaseMediaFolderInfo>() {
            @Override
            public int compare(BaseMediaFolderInfo o1, BaseMediaFolderInfo o2) {
                return o1.name.compareTo(o2.name);
            }
        };
    }
}
