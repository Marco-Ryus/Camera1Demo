package com.example.camera1demo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.baidu.ocr.sdk.OCR;
import com.baidu.ocr.sdk.OnResultListener;
import com.baidu.ocr.sdk.exception.OCRError;
import com.baidu.ocr.sdk.model.AccessToken;
import com.baidu.ocr.sdk.model.GeneralParams;
import com.baidu.ocr.sdk.model.GeneralResult;
import com.baidu.ocr.sdk.model.Word;
import com.baidu.ocr.sdk.model.WordSimple;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static java.lang.Math.abs;

/*
    要在Android中调用相机功能，一是调用系统相机，二是利用Camera与SurfaceView进行处理
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
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.INTERNET,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private int no;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }


    //用于预测文字及其位置
    private void Text(final File file){
        //初始化ocr
        OCR.getInstance(MainActivity.this).initAccessTokenWithAkSk(new OnResultListener<AccessToken>() {
            @Override
            public void onResult(AccessToken result) { // 调用成功，返回AccessToken对象
                String token = result.getAccessToken();
                Log.e(TAG, "初始化Access成功");
            }

            @Override
            public void onError(OCRError error) { // 调用失败，返回OCRError子类SDKError对象
                Log.e(TAG, "初始化Access失败");
            }
        }, getApplicationContext(), "Bc1oC0mlhLuGjU3tCFW63DBv", "1zGwRnUhDh5F1MGMLj3BZUjmWCpRq78R");
        //通用文字识别参数设置
        GeneralParams param = new GeneralParams();
        param.setDetectDirection(true);
        Log.d(TAG, "运行到param");
        param.setImageFile(file);
        Log.d(TAG, "运行到setImage");
        // 调用通用文字识别服务（含位置信息版）
        OCR.getInstance(MainActivity.this).recognizeGeneral(param, new OnResultListener<GeneralResult>() {
            @Override
            public void onResult(GeneralResult result) {
                StringBuilder sb = new StringBuilder();
                for (WordSimple wordSimple : result.getWordList()) {
                    // word包含位置
                    Word word = (Word) wordSimple;
                    sb.append(word.getWords());
                    sb.append(word.getLocation().toString());
                    sb.append("\n");
                }
                // 调用成功，返回GeneralResult对象，通过getJsonRes方法获取API返回字符串
                String jsonRes = result.getJsonRes();
                String[][] res = parseJSON(jsonRes);//处理返回的json，返回文字的位置信息
                Log.d(TAG, "res size: " + res.length);
                Log.e(TAG, jsonRes);
                DataPro dataPro = new DataPro();
                dataPro.setData(res).setFile(file);
                String finalRes = dataPro.processData();
                if(finalRes!=null){
                    Log.w(TAG, "识别的最终结果: " + finalRes);
                }
            }

            @Override
            public void onError(OCRError error) {
                // 调用失败，返回OCRError对象
            }
        });
    }

    private String[][] parseJSON(String jsonRes) {
        String[][] res = null;
        try{
            JSONObject jsonArray = new JSONObject(jsonRes);
            String words = jsonArray.getString("words_result");
            Log.e(TAG, "words: " + words);
            JSONArray jsonArray1 = new JSONArray(words);
            //使用一个数组存放位置信息
            res = new String[jsonArray1.length()][5];
            for (int i = 0; i < jsonArray1.length(); i++) {
                JSONObject jsonObject = jsonArray1.getJSONObject(i);
                String charactor = jsonObject.getString("words");
                String loc = jsonObject.getString("location");
                Log.e(TAG, "文字是 ：" + charactor + "; 位置 ：" + loc);
                //再具体获取location
                JSONObject location = new JSONObject(loc);
                String top = location.getString("top");
                String left = location.getString("left");
                String width = location.getString("width");
                String height = location.getString("height");
                Log.d(TAG, "top: " + top + "; left:" + left + "; width: " + width + "height:" + height);
                res[i][0] = top;
                res[i][1] = left;
                res[i][2] = width;
                res[i][3] = height;
                res[i][4]= charactor;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    //在这里抽取了一个方法   可以封装到自己的工具类中...
    public File getFile(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        File file = new File(Environment.getExternalStorageDirectory() + "/temp.jpg");
        try {
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            InputStream is = new ByteArrayInputStream(baos.toByteArray());
            int x = 0;
            byte[] b = new byte[1024 * 100];
            while ((x = is.read(b)) != -1) {
                fos.write(b, 0, x);
            }
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }
    //初始化
    private void initView() {
        takePhoto = findViewById(R.id.take_photo);
        iv_show = findViewById(R.id.image_show);
        mSurfaceView = findViewById(R.id.photo_preview);
//        viewWidth = mSurfaceView.getWidth();
//        viewHeight = mSurfaceView.getHeight();
        mSurfaceHolder = mSurfaceView.getHolder();
        //不需要自己的缓冲区
        /*表明该Surface不包含原生数据，Surface用到的数据由其他对象提供，在Camera图像预览中就使用该类型的
        Surface，有Camera负责提供给预览Surface数据，这样图像预览会比较流畅。如果设置这种类型则就不能调用
        lockCanvas来获取Canvas对象了。需要注意的是，在高版本的Android SDK中，setType这个方法已经被depreciated了。*/
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);    //因为显示画面采用的是Camera回调画面，而不是Surface的信息
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
        camera.setDisplayOrientation(90);   //相对于预览画面而言，否则预览画面会是外的
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
                //设置自动对焦
                parameters.setFocusMode(Camera.Parameters.FLASH_MODE_AUTO);
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
            Log.d(TAG, "生成bitmap");
            Bitmap resource = BitmapFactory.decodeByteArray(data, 0, data.length);
            resource = resource.copy(Bitmap.Config.ARGB_4444, true);
            if (resource == null) {
                Toast.makeText(MainActivity.this, "拍照失败", Toast.LENGTH_SHORT).show();
            }
            //矩阵
            final Matrix matrix = new Matrix();
            matrix.setRotate(90);   //用于旋转，否则最终拍照效果会旋转90度；
            final Bitmap bitmap = Bitmap.createBitmap(resource, 0, 0, resource.getWidth(),
                    resource.getHeight(), matrix, true);
            if (bitmap != null && iv_show != null && iv_show.getVisibility() == View.GONE) {
                camera.stopPreview();
                Log.d(TAG, "生成成功");
                iv_show.setVisibility(View.VISIBLE);
                mSurfaceView.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "拍照成功", Toast.LENGTH_SHORT).show();
                iv_show.setImageBitmap(bitmap);
                Text(getFile(bitmap));
            }
        }
    };

    //动态加载opencv库
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
//                    mOpenCvCameraView.enableView();
//                    mOpenCvCameraView.setOnTouchListener(ColorBlobDetectionActivity.this);
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
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
        //动态加载opencv库
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
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
