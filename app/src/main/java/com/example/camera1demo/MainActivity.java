package com.example.camera1demo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import static java.lang.Math.abs;

/*
    因为现在其实现在好像已经出到CameraX，还有Camera2，但因为Camera2的配置比较复杂而且对不同机型适配不同，
    这里介绍比较简单的Camera1，算是一个入门的操作，之后再去涉及其他camera都会容易些,但是因为Camera1的版本
    比较久远，很多方法会被AS认为需要淘汰更新，因此会被划上横线
 */

public class MainActivity extends AppCompatActivity{
    private static final String TAG = "MainActivity";
    private Button takePhoto;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Camera camera;
    private ImageView iv_show;
    private int viewWidth, viewHeight;      //mSurfaceView的宽和高
    //用于申请权限
    private static final int REQUEST_PERMISSIONS_CODE = 1;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private int no;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }
    //初始化
    private void initView() {
        takePhoto = findViewById(R.id.take_photo);
        iv_show = findViewById(R.id.image_show);
        mSurfaceView = findViewById(R.id.photo_preview);
        viewWidth = mSurfaceView.getWidth();
        viewHeight = mSurfaceView.getHeight();
        mSurfaceHolder = mSurfaceView.getHolder();
        //不需要自己的缓冲区
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        //添加回调
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                //初始化Camera
                initCamera();
                Log.d(TAG, "SurfaceView已生成");
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                //销毁，释放资源
                Log.d(TAG, "SurfaceView已被销毁");
                if (camera != null) {
                    camera.stopPreview();
                    camera.setPreviewCallback(null);
                    camera.release();
                    camera = null;
                }
            }
        });
        //设置点击监听
        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                camera.takePicture(new Camera.ShutterCallback() {
                    @Override
                    public void onShutter() {       //快门按下一瞬间会发生的操作

                    }
                }, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {

                    }
                },pictureCallback);
                takePhoto.setVisibility(View.GONE);
            }
        });
    }
    private void initCamera() {
        //开启
        camera = Camera.open();
        //设置旋转角度
        camera.setDisplayOrientation(90);
        if (camera != null) {
            try {
                Camera.Parameters parameters = camera.getParameters();
                //设置预览照片的大小
                parameters.setPreviewFpsRange(viewWidth, viewHeight);
                //设置相机预览照片帧数
                parameters.setPreviewFpsRange(4, 10);
                //设置图片格式
                parameters.setPictureFormat(ImageFormat.JPEG);
                //图片质量
                parameters.set("jpeg-quality", 90);
                //设置照片的大小
                parameters.setPictureSize(viewWidth, viewHeight);
                //通过SurfaceView显示预览
                camera.setPreviewDisplay(mSurfaceHolder);
                //开始预览
                camera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            final Bitmap resource = BitmapFactory.decodeByteArray(data, 0, data.length);
            if (resource == null) {
                Toast.makeText(MainActivity.this, "拍照失败", Toast.LENGTH_SHORT).show();
            }
            //矩阵
            final Matrix matrix = new Matrix();
            matrix.setRotate(90);
            final Bitmap bitmap = Bitmap.createBitmap(resource, 0, 0, resource.getWidth(),
                    resource.getHeight(), matrix, true);
            if (bitmap != null && iv_show != null && iv_show.getVisibility() == View.GONE) {
                camera.stopPreview();
                iv_show.setVisibility(View.VISIBLE);
                mSurfaceView.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "拍照成功", Toast.LENGTH_SHORT).show();
                iv_show.setImageBitmap(bitmap);
            }
        }
    };


    @Override
    protected void onResume() {
        super.onResume();
        // 动态权限检查
        if (!isRequiredPermissionsGranted() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //以下是AppCompat的一个方法，输入需要申请的权限的字符数组，会自动调用函数弹窗询问用户是否允许权限使用；
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_PERMISSIONS_CODE);
        }
    }

    /**
     * 判断我们需要的权限是否被授予，只要有一个没有授权，我们都会返回 false。
     *
     * @return true 权限都被授权
     */
    private boolean isRequiredPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

}
