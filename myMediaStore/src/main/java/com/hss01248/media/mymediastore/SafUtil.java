package com.hss01248.media.mymediastore;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;

import androidx.core.os.EnvironmentCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.appcompat.app.AppCompatActivity;

import android.os.StatFs;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;


import com.hss01248.activityresult.ActivityResultListener;
import com.hss01248.activityresult.StartActivityUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.ArrayList;


public class SafUtil {
    public static DocumentFile sdRoot;
    public static String TAG = "SAF2";
    public static Context context;

    public static DocumentFile getRoot(Activity activity){
        SharedPreferences sp = activity.getSharedPreferences("DirPermission", Context.MODE_PRIVATE);
        String uriTree = sp.getString("uriTree", "");
        if(TextUtils.isEmpty(uriTree)){
            return null;
        }
        Uri uri = Uri.parse(uriTree);
        final int takeFlags = activity.getIntent().getFlags()
                & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            activity.getContentResolver().takePersistableUriPermission(uri, takeFlags);
            DocumentFile root = DocumentFile.fromTreeUri(activity.getApplicationContext(), uri);
            Log.d(SafUtil.TAG, uriTree);
            return root;
        }
        return null;
    }

    public static void getRootDir(FragmentActivity activity, ISdRoot callback){
        context = activity.getApplicationContext();

        ArrayList<StorageBean> storageData = getStorageData(activity.getApplicationContext());
        if(storageData ==null || storageData.size() <=0){
            Log.w(SafUtil.TAG,"没有额外sd卡");
            return;
        }

        //requestSaf(activity,callback);
        SharedPreferences sp = activity.getSharedPreferences("DirPermission", Context.MODE_PRIVATE);
        String uriTree = sp.getString("uriTree", "");
        Log.d(SafUtil.TAG,URLDecoder.decode(uriTree)+"<---");
        if (TextUtils.isEmpty(uriTree)) {
            // 重新授权
            Log.d(SafUtil.TAG,"重新授权申请");
            requestSaf(activity,callback);
        } else {
            try {
                Uri uri = Uri.parse(uriTree);
                Log.d(SafUtil.TAG,uri+"");
                final int takeFlags = activity.getIntent().getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    activity.getContentResolver().takePersistableUriPermission(uri, takeFlags);
                    DocumentFile root = DocumentFile.fromTreeUri(activity.getApplicationContext(), uri);
                    Log.d(SafUtil.TAG,uriTree);
                    if(root == null){
                        callback.onPermissionDenied(7,"DocumentFile.fromTreeUri return null");
                        return;
                    }
                    sdRoot = root;
                    callback.onPermissionGet(root);
                }else {
                    callback.onPermissionDenied(9,"android version is below 4.4");
                }

            } catch (SecurityException e) {
                e.printStackTrace();
                // 重新授权
                requestSaf(activity,callback);
            }
        }

    /*    作者：唯鹿
        链接：https://juejin.im/post/6844904058743078919
        来源：掘金
        著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。*/
    }

    private static void requestSaf(final FragmentActivity activity, final ISdRoot callback) {
        // 用户可以选择任意文件夹，将它及其子文件夹的读写权限授予APP。
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        StartActivityUtil.goOutAppForResult((AppCompatActivity) activity, intent, new ActivityResultListener() {
            @Override
            public void onActivityResult(int requestCode, int resultCode, Intent data) {
                Log.e(TAG,resultCode+"-"+data);
                //onActivityResult: req:3593,result:-1,data:Intent { dat=content://com.android.externalstorage.documents/tree/0123-4567: flg=0xc3 }
                if(resultCode == Activity.RESULT_OK){
                    Uri uriTree = null;
                    if (data != null) {
                        uriTree = data.getData();
                    }
                    if (uriTree != null) {
                        Log.d(TAG, URLDecoder.decode(uriTree.toString()));

                        try {
                            final int takeFlags = activity.getIntent().getFlags()
                                    & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                activity.getContentResolver().takePersistableUriPermission(uriTree, takeFlags);
                                DocumentFile root = DocumentFile.fromTreeUri(activity.getApplicationContext(), uriTree);


                                // 创建所选目录的DocumentFile，可以使用它进行文件操作
                                // DocumentFile root = //DocumentFile.fromTreeUri(activity.getApplicationContext(), uriTree);
                                // DocumentFile.fromSingleUri(activity.getApplicationContext(),uriTree);
                                // 比如使用它创建文件夹


                                Log.d(TAG,root.getUri()+"--");
                                if(root == null){
                                    callback.onPermissionDenied(7,"DocumentFile.fromTreeUri return null");
                                    return;
                                }
                                sdRoot = root;
                                // 保存获取的目录权限
                                SharedPreferences sp = activity.getSharedPreferences("DirPermission", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sp.edit();
                                editor.putString("uriTree", uriTree.toString());
                                //editor.putString("uriTree", uriTree.toString());
                                editor.commit();
                                callback.onPermissionGet(root);
                            }
                        }catch (Throwable throwable){
                            throwable.printStackTrace();
                            callback.onPermissionDenied(resultCode,throwable.getMessage());
                        }

                    }else {
                        Log.w(TAG,"uri == null");
                        callback.onPermissionDenied(resultCode,"data in intent of reaultback is null");
                    }
                }else {
                    callback.onPermissionDenied(resultCode,"onResultError");
                }
            }

            @Override
            public void onActivityNotFound(Throwable e) {

            }
        });
        Toast.makeText(activity,"请选择SD卡根目录并允许访问",Toast.LENGTH_LONG).show();

    }


    public interface ISdRoot{
        void onPermissionGet(DocumentFile dir);

        void onPermissionDenied(int resultCode, String msg);
    }


    private static String readFile(DocumentFile file,Context context) {
        try {
            //file.getUri()
            InputStreamReader reader = new InputStreamReader(context.getContentResolver().openInputStream(file.getUri()));
            BufferedReader bReader = new BufferedReader(reader);//new一个BufferedReader对象，将文件内容读取到缓存
            StringBuilder sb = new StringBuilder();//定义一个字符串缓存，将字符串存放缓存中
            String s = "";
            while ((s =bReader.readLine()) != null) {//逐行读取文件内容，不读取换行符和末尾的空格
                sb.append(s);//将读取的字符串添加换行符后累加存放在缓存中
                System.out.println(s);
            }
            bReader.close();
            String str = sb.toString();
            System.out.println(str );
            return str;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private static void alterDocument(Uri uri,String content,Context context) {
        try {
            ParcelFileDescriptor pfd = context.getContentResolver().
                    openFileDescriptor(uri, "w");
            FileOutputStream fileOutputStream =
                    new FileOutputStream(pfd.getFileDescriptor());
            fileOutputStream.write(content.getBytes());
            fileOutputStream.flush();
            // Let the document provider know you're done by closing the stream.
            fileOutputStream.close();
            pfd.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



   // private static final String TAG = StorageUtils.class.getSimpleName();

    public static ArrayList<StorageBean> getStorageData(Context pContext) {
        final StorageManager storageManager = (StorageManager) pContext.getSystemService(Context.STORAGE_SERVICE);
        try {
            //得到StorageManager中的getVolumeList()方法的对象
            final Method getVolumeList = storageManager.getClass().getMethod("getVolumeList");
            //---------------------------------------------------------------------

            //得到StorageVolume类的对象
            final Class<?> storageValumeClazz = Class.forName("android.os.storage.StorageVolume");
            //---------------------------------------------------------------------
            //获得StorageVolume中的一些方法
            final Method getPath = storageValumeClazz.getMethod("getPath");
            Method isRemovable = storageValumeClazz.getMethod("isRemovable");

            Method mGetState = null;
            //getState 方法是在4.4_r1之后的版本加的，之前版本（含4.4_r1）没有
            // （http://grepcode.com/file/repository.grepcode.com/java/ext/com.google.android/android/4.4_r1/android/os/Environment.java/）
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                try {
                    mGetState = storageValumeClazz.getMethod("getState");
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
            //---------------------------------------------------------------------

            //调用getVolumeList方法，参数为：“谁”中调用这个方法
            final Object invokeVolumeList = getVolumeList.invoke(storageManager);
            //---------------------------------------------------------------------
            final int length = Array.getLength(invokeVolumeList);
            ArrayList<StorageBean> list = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                final Object storageValume = Array.get(invokeVolumeList, i);//得到StorageVolume对象
                final String path = (String) getPath.invoke(storageValume);
                final boolean removable = (Boolean) isRemovable.invoke(storageValume);
                String state = null;
                if (mGetState != null) {
                    state = (String) mGetState.invoke(storageValume);
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        state = Environment.getStorageState(new File(path));
                    } else {
                        if (removable) {
                            state = EnvironmentCompat.getStorageState(new File(path));
                        } else {
                            //不能移除的存储介质，一直是mounted
                            state = Environment.MEDIA_MOUNTED;
                        }
                        final File externalStorageDirectory = Environment.getExternalStorageDirectory();
                        Log.e(TAG, "externalStorageDirectory==" + externalStorageDirectory);
                    }
                }
                long totalSize = 0;
                long availaleSize = 0;
                if (Environment.MEDIA_MOUNTED.equals(state)) {
                    totalSize = getTotalSize(path);
                    availaleSize = getAvailableSize(path);
                }
                final String msg = "path==" + path
                        + " ,removable==" + removable
                        + ",state==" + state
                        + ",total size==" + totalSize + "(" + fmtSpace(totalSize) + ")"
                        + ",availale size==" + availaleSize + "(" + fmtSpace(availaleSize) + ")";
                Log.e(TAG, msg);
                StorageBean storageBean = new StorageBean();
                storageBean.setAvailableSize(availaleSize);
                storageBean.setTotalSize(totalSize);
                storageBean.setMounted(state);
                storageBean.setPath(path);
                storageBean.setRemovable(removable);
                list.add(storageBean);
            }
            return list;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static long getTotalSize(String path) {
        try {
            final StatFs statFs = new StatFs(path);
            long blockSize = 0;
            long blockCountLong = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                blockSize = statFs.getBlockSizeLong();
                blockCountLong = statFs.getBlockCountLong();
            } else {
                blockSize = statFs.getBlockSize();
                blockCountLong = statFs.getBlockCount();
            }
            return blockSize * blockCountLong;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static long getAvailableSize(String path) {
        try {
            final StatFs statFs = new StatFs(path);
            long blockSize = 0;
            long availableBlocks = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                blockSize = statFs.getBlockSizeLong();
                availableBlocks = statFs.getAvailableBlocksLong();
            } else {
                blockSize = statFs.getBlockSize();
                availableBlocks = statFs.getAvailableBlocks();
            }
            return availableBlocks * blockSize;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static final long A_GB = 1073741824;
    public static final long A_MB = 1048576;
    public static final int A_KB = 1024;

    public static String fmtSpace(long space) {
        if (space <= 0) {
            return "0";
        }
        double gbValue = (double) space / A_GB;
        if (gbValue >= 1) {
            return String.format("%.2fGB", gbValue);
        } else {
            double mbValue = (double) space / A_MB;
            Log.e("GB", "gbvalue=" + mbValue);
            if (mbValue >= 1) {
                return String.format("%.2fMB", mbValue);
            } else {
                final double kbValue = space / A_KB;
                return String.format("%.2fKB", kbValue);
            }
        }
    }

    public static DocumentFile findFile(DocumentFile topDir,String uriString){
        try {
            String path2 = URLDecoder.decode(uriString);
            if(path2.contains(":")){
                path2 = path2.substring(path2.lastIndexOf(":")+1);
            }
            //Log.d(TAG,"pure path:"+path2);
            String[] paths = path2.split("/");
            DocumentFile dir = topDir;
            if(path2 != null && path2.length() > 0){
                for (String path : paths) {
                    dir = dir.findFile(path);

                }
            }
            if(dir.equals(topDir)){
                return null;
            }
            //Log.d(TAG,"dir path:"+URLDecoder.decode(dir.getUri().toString()));
            return dir;

        }catch (Throwable throwable){
            throwable.printStackTrace();
            return null;
        }
    }
}
