package com.hss01248.mediax.demo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.hss01248.media.metadata.ExifUtil;

import org.devio.takephoto.wrap.TakeOnePhotoListener;
import org.devio.takephoto.wrap.TakePhotoUtil;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView textView = findViewById(R.id.tv);
        TakePhotoUtil.startPickOneWitchDialog(this, new TakeOnePhotoListener() {
            @Override
            public void onSuccess(String path) {
                try {
                    String str =  ExifUtil.getExifStr(path);
                    Log.d("d",str);
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
}