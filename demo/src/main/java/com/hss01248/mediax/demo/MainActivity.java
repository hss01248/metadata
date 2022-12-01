package com.hss01248.mediax.demo;

import android.Manifest;
import android.app.Dialog;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.hss.utils.enhance.api.MyCommonCallback;
import com.hss01248.image.dataforphotoselet.ImgDataSeletor;
import com.hss01248.media.metadata.ExifUtil;
import com.hss01248.media.metadata.MetaDataUtil;
import com.hss01248.media.pick.MediaPickOrCaptureUtil;
import com.hss01248.media.pick.MediaPickUtil;
import com.hss01248.permission.MyPermissions;
import com.hss01248.toast.MyToast;

import org.devio.takephoto.wrap.TakeOnePhotoListener;
import org.devio.takephoto.wrap.TakePhotoUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    TextView textView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
         textView = findViewById(R.id.tv_desc);




        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},900);
        }*/
        /*SafUtil.getRootDir(this, new SafUtil.ISdRoot() {
            @Override
            public void onPermissionGet(DocumentFile dir) {
                Log.e(SafUtil.TAG,"get root success:"+ URLDecoder.decode(dir.getUri().toString()));
                DefaultScanFolderCallback callback = new DefaultScanFolderCallback() {
                    @Override
                    protected void notifyDataSetChanged() {
                        Log.e(SafUtil.TAG,"infos notifyDataSetChanged: "+ getInfos().size());

                    }
                };
                SafFileFinder.listAllAlbum(callback);
            }

            @Override
            public void onPermissionDenied(int resultCode, String msg) {

            }
        });*/
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MyPermissions.requestByMostEffort(false, true
                    , new PermissionUtils.FullCallback() {
                        @Override
                        public void onGranted(@NonNull List<String> granted) {

                        }

                        @Override
                        public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {

                        }
                    }, Manifest.permission.ACCESS_MEDIA_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
       /* FileFinder.listAllAlbum(new Observer<List<BaseMediaFolderInfo>>() {
            @Override
            public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {

            }

            @Override
            public void onNext(@io.reactivex.annotations.NonNull List<BaseMediaFolderInfo> baseMediaFolderInfos) {

            }

            @Override
            public void onError(@io.reactivex.annotations.NonNull Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });*/
    }

    public void select(View view) {
        TakePhotoUtil.startPickOneWitchDialog(this, new TakeOnePhotoListener() {
            @Override
            public void onSuccess(String path) {
                try {
                    long start = System.currentTimeMillis();
                   // String str =  ExifUtil.getExifStr(new FileInputStream(path));
                    String str =  GsonUtils.toJson(ExifUtil.getBasicMap(path));
                    //Log.d("d",str);
                    String cost = "getExifStr cost:"+(System.currentTimeMillis() - start)+"ms\n";
                    str = cost + str;
                    textView.setText(str);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onFail(String path, String msg) {

            }

            @Override
            public void onCancel() {

            }
        });
    }
    Dialog dialog;
    private void showDesc(boolean userUri,String uri){
        LogUtils.i(uri);
        long start = System.currentTimeMillis();

        if(uri.startsWith("http")){
             dialog = MyToast.showLoadingDialog("");
        }

        ThreadUtils.executeByIo(new ThreadUtils.SimpleTask<String>() {
            @Override
            public String doInBackground() throws Throwable {
               return userUri ?  MetaDataUtil.getDes2(Uri.parse(uri)) : MetaDataUtil.getDes(uri);
            }

            @Override
            public void onSuccess(String result) {
                if(dialog != null) dialog.dismiss();
                String cost = "getExifStr cost:"+(System.currentTimeMillis() - start)+"ms\n";
               String  des = cost + result;
                textView.setText(des);
            }

            @Override
            public void onFail(Throwable t) {
                super.onFail(t);
                if(dialog != null) dialog.dismiss();
                LogUtils.w(t);
                ToastUtils.showLong(t.getMessage());
            }
        });
    }

    public void selectfile(View view) {

        ImgDataSeletor.startPickOneWitchDialog(this, new TakeOnePhotoListener() {
            @Override
            public void onSuccess(String path) {
                showDesc(false,path);
            }

            @Override
            public void onFail(String path, String msg) {

            }

            @Override
            public void onCancel() {

            }
        });


    }

    public void findDateByFileName(View view) {
        List<File> files = new ArrayList<>();
        String root = "/storage/xxxx/";
        files.add(new File(root+"VID_20210302_16304170-xxxx.mp4"));
        files.add(new File(root+"Screenshot_20190619_105917_包名.jpg"));
        files.add(new File(root+"20190619_105917.jpg"));
        files.add(new File(root+"2019-06-19_10-59-17_2545184214851555.jpg"));
        files.add(new File(root+"2019-06-19-10-59-17-xxxx.jpg"));
        files.add(new File(root+"IMG_20190619_105917.jpg"));
        files.add(new File(root+"IMG_2019pi0619_105917.jpg"));
         SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        for (File file : files) {
            try {
                long time = MetaDataUtil.timeGuessFromFileName(file.getName());
                Log.i("final data",sdf.format(new Date(time))) ;
            }catch (Throwable throwable){
                throwable.printStackTrace();
            }

        }
        //VID_20210302_16304170-xxxx.mp4
        //Screenshot_20190619_105917_包名.jpg/png
        //20190619_105917.jpg
        //2019-06-19_10-59-17_2545184214851555.jpg
        //2019-06-19-10-59-17-xxxx.jpg
        //IMG_20190619_105917.jpg
    }

    public void pickBySys(View view) {
        MediaPickOrCaptureUtil.pickOrCaptureImageOrVideo(15, new MyCommonCallback<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                showDesc(true,uri.toString());
            }
        });

    }

    public void httpImage(View view) {
        String url2 = "http://examples-1251000004.cos.ap-shanghai.myqcloud.com/sample.jpeg";
        String url = "https://upload.wikimedia.org/wikipedia/commons/thumb/9/9e/Exif_JPEG_PICTURE_%2824999915302%29.jpg/800px-Exif_JPEG_PICTURE_%2824999915302%29.jpg?20180503235201";
        showDesc(true,url2);

    }

    public void httpVideo(View view) {
        String url = "https://www.runoob.com/try/demo_source/mov_bbb.mp4";
        showDesc(true,url);
    }

    public void pickBySys2(View view) {
        MediaPickUtil.pickPdf(new MyCommonCallback<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                showDesc(true,uri.toString());
            }
        });
    }
}