package com.hss01248.media.mymediastore;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.hss01248.media.mymediastore.bean.BaseMediaFolderInfo;
import com.hss01248.media.mymediastore.bean.BaseMediaInfo;
import com.hss01248.media.mymediastore.db.BaseMediaFolderInfoDao;
import com.hss01248.media.mymediastore.db.BaseMediaInfoDao;
import com.hss01248.media.mymediastore.db.DaoMaster;
import com.hss01248.media.mymediastore.db.DaoSession;
import com.hss01248.media.mymediastore.sort.SortByFileName;
import com.hss01248.media.mymediastore.sort.SortByFolderName;

import java.util.List;

public class DbUtil {

    public static ISort folderSort  = new SortByFolderName();
    public static ISort contentSort  = new SortByFileName();

     static void init(Context context){
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(context, "mymedia.db");
        SQLiteDatabase db = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();
    }

    private volatile static DaoSession daoSession;
    public static DaoSession getDaoSession() {
        if(daoSession == null){
            synchronized (DbUtil.class){
                if(daoSession == null){
                    init(SafUtil.context);
                }
            }
        }
        return daoSession;
    }

    public static List<BaseMediaFolderInfo> getAllFolders(){
        return getAllFolders(BaseMediaInfo.TYPE_IMAGE);
    }

    public static List<BaseMediaFolderInfo> getAllFolders(int type){
        long start = System.currentTimeMillis();
        List<BaseMediaFolderInfo> infos =  getDaoSession().getBaseMediaFolderInfoDao().queryBuilder()
                .where(BaseMediaFolderInfoDao.Properties.Type.eq(type))
                .orderDesc(BaseMediaFolderInfoDao.Properties.FileSize)
                .list();
        Log.v(SafUtil.TAG, " getAllFolders 耗时(ms):"+(System.currentTimeMillis() - start)+", size:"+infos.size());
        //耗时(ms):103
        return infos;
    }

    public static List<BaseMediaFolderInfo> getAllImageAndVideoFolders(){
        long start = System.currentTimeMillis();
        List<BaseMediaFolderInfo> infos =  getDaoSession().getBaseMediaFolderInfoDao().queryBuilder()
                .whereOr(BaseMediaFolderInfoDao.Properties.Type.eq(1),BaseMediaFolderInfoDao.Properties.Type.eq(2))
                .orderDesc(BaseMediaFolderInfoDao.Properties.FileSize)
                .list();
        Log.v(SafUtil.TAG, " getAllImageAndVideoFolders 耗时(ms):"+(System.currentTimeMillis() - start)+", size:"+infos.size());
        return infos;
    }

    public static List<BaseMediaInfo> getAllContentInFolders(String dir, int type){
        long start = System.currentTimeMillis();
        List<BaseMediaInfo> infos =  getDaoSession().getBaseMediaInfoDao().queryBuilder()
                .where(BaseMediaInfoDao.Properties.Type.eq(type),BaseMediaInfoDao.Properties.FolderPathOrUri.eq(dir))
                //最新的排最前
                .orderDesc(BaseMediaInfoDao.Properties.UpdatedTime)
                .list();
        Log.v(SafUtil.TAG, " getAllContentInFolders 耗时(ms):"+(System.currentTimeMillis() - start)+", size:"+infos.size() +", dir:"+dir);
        return infos;
    }


}
