package com.example.camera1demo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.baidu.ocr.sdk.OCR;
import com.baidu.ocr.sdk.OnResultListener;
import com.baidu.ocr.sdk.exception.OCRError;
import com.baidu.ocr.sdk.model.AccessToken;
import com.baidu.ocr.sdk.model.GeneralParams;
import com.baidu.ocr.sdk.model.GeneralResult;
import com.baidu.ocr.sdk.model.Word;
import com.baidu.ocr.sdk.model.WordSimple;
import com.baidu.tts.client.SpeechError;
import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.SpeechSynthesizerListener;
import com.baidu.tts.client.TtsMode;
import com.example.camera1demo.gson.Bean;
import com.example.camera1demo.util.DataUtils;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.abs;


public class MainActivity extends AppCompatActivity{

    private static final String TAG = "MainActivity";
    private Button takePhoto;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Camera camera;
    private Bundle bundle;
    private ProgressDialog progressDialog;  //用于匹配图片时提醒用户等待
    private int viewWidth, viewHeight;
    public static SpeechSynthesizer mSpeechSynthesizer = SpeechSynthesizer.getInstance();

    //用于申请权限
    private static final int REQUEST_PERMISSIONS_CODE = 1;
    private static final String[] REQUIRED_PERMISSIONS = {
                Manifest.permission.CAMERA,
                Manifest.permission.INTERNET,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE
    };

    Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
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
            resource.recycle();
            if (bitmap != null) {
                camera.stopPreview();
//                Toast.makeText(MainActivity.this, "拍照成功", Toast.LENGTH_SHORT).show();
                Text(DataUtils.getFile(bitmap,"/result.jpg"));
                bitmap.recycle();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 动态权限检查
        if(!isRequiredPermissionsGranted() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //以下是AppCompat的一个方法，输入需要申请的权限的字符数组，会自动调用函数弹窗询问用户是否允许权限使用；
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_PERMISSIONS_CODE);
        }
        while(!isRequiredPermissionsGranted()){};
        setContentView(R.layout.activity_main);
        initBaidu();
        initView();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                Toast.makeText(this, "拒绝授权将无法使用本软件", Toast.LENGTH_LONG).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    //动态加载opencv库
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
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
        //动态加载opencv库
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private void initBaidu() {
        //初始化ocr
        OCR.getInstance(MainActivity.this).initAccessTokenWithAkSk(new OnResultListener<AccessToken>() {
            @Override
            public void onResult(AccessToken result) {
                // 调用成功，返回AccessToken对象
                String token = result.getAccessToken();
            }

            @Override
            public void onError(OCRError error) {
                // 调用失败，返回OCRError子类SDKError对象
            }
        }, getApplicationContext(), "Bc1oC0mlhLuGjU3tCFW63DBv", "1zGwRnUhDh5F1MGMLj3BZUjmWCpRq78R");

        //初始化百度语音合成
        mSpeechSynthesizer.setContext(this); // this 是Context的之类，如Activity
        mSpeechSynthesizer.setSpeechSynthesizerListener(new SpeechSynthesizerListener() {
            @Override
            public void onSynthesizeStart(String s) {

            }

            @Override
            public void onSynthesizeDataArrived(String s, byte[] bytes, int i, int i1) {

            }

            @Override
            public void onSynthesizeFinish(String s) {

            }

            @Override
            public void onSpeechStart(String s) {

            }

            @Override
            public void onSpeechProgressChanged(String s, int i) {

            }

            @Override
            public void onSpeechFinish(String s) {

            }

            @Override
            public void onError(String s, SpeechError speechError) {

            }
        });
        mSpeechSynthesizer.setAppId("23579832"/*这里只是为了让Demo运行使用的APPID,请替换成自己的id。*/);
        mSpeechSynthesizer.setApiKey("7fCouzZzRgfvszB1aDXDz2ly", "KEYrjowmQFvv5RWbhXNYzc5bh8E3uL5m"/*这里只是为了让Demo正常运行使用APIKey,请替换成自己的APIKey*/);
        mSpeechSynthesizer.initTts(TtsMode.ONLINE);
        // 设置在线发声音人： 0 普通女声（默认） 1 普通男声  3 情感男声<度逍遥> 4 情感儿童声<度丫丫>
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER, "3");
        // 设置合成的音量，0-15 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_VOLUME, "15");
        // 设置合成的语速，0-15 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEED, "5");
        // 设置合成的语调，0-15 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_PITCH, "5");
    }

    //用于预测文字及其位置
    private void Text(final File file){
        final double[] point = DataUtils.newFingerScan(BitmapFactory.decodeFile(file.toString()));
        if (point.length > 0) {
            GeneralParams param = new GeneralParams();
            param.setDetectDirection(true);
//        Log.d(TAG, "运行到param");
            param.setImageFile(file);
//        Log.d(TAG, "运行到setImage");
            // 调用通用文字识别服务（含位置信息版）
                OCR.getInstance(MainActivity.this).recognizeGeneral(param, new OnResultListener<GeneralResult>() {
                        @Override
                        public void onResult(GeneralResult result) {
                            // 调用成功，返回GeneralResult对象，通过getJsonRes方法获取API返回字符串
                            String jsonRes = result.getJsonRes();
                            Bean bean = new Gson().fromJson(jsonRes, Bean.class);
                            Bean.WordResult[] words_result = bean.getWords_result();
                            int i = DataUtils.prePocessText(point, words_result);
                            if(i!=-1){
                                bundle.putString("result",words_result[i].getWords());
                                Bean.MyLocation location = words_result[i].getLocation();
                                int[] res = new int[]{location.getTop(), location.getLeft(), location.getWidth(), location.getHeight()};
                                bundle.putIntArray("location", res);
                            } else {
//                                Toast.makeText(MainActivity.this, "无匹配结果", Toast.LENGTH_LONG).show();
                                bundle.putString("result", null);
                            }
                            Intent intent = new Intent(MainActivity.this, ImageShow.class);
                            intent.putExtras(bundle);
                            progressDialog.dismiss();
                            startActivity(intent);
                        }
                    @Override
                    public void onError(OCRError error) {
                        // 调用失败，返回OCRError对象
                    }
            });
        } else {
            //没有找到指尖不进行文字识别
            bundle.putString("result", null);
            Intent intent = new Intent(MainActivity.this, ImageShow.class);
            intent.putExtras(bundle);
            progressDialog.dismiss();
            startActivity(intent);
        }
    }

    private String[][] parseJSON(String jsonRes) {
        String[][] res = null;
        try{
            JSONObject jsonArray = new JSONObject(jsonRes);
            String words = jsonArray.getString("words_result");
//            Log.e(TAG, "words: " + words);
            JSONArray jsonArray1 = new JSONArray(words);
            //使用一个数组存放位置信息
            res = new String[jsonArray1.length()][5];
            String top, left, width, height;
            for (int i = 0; i < jsonArray1.length(); i++) {
                JSONObject jsonObject = jsonArray1.getJSONObject(i);
                String charactor = jsonObject.getString("words");
                String loc = jsonObject.getString("location");
//                Log.e(TAG, "文字是 ：" + charactor + "; 位置 ：" + loc);
                //再具体获取location
                JSONObject location = new JSONObject(loc);
                top = location.getString("top");
                left = location.getString("left");
                width = location.getString("width");
                height = location.getString("height");
//                Log.d(TAG, "top: " + top + "; left:" + left + "; width: " + width + "height:" + height);
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

    //初始化
    private void initView() {
        bundle = new Bundle();
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Loading...");
        progressDialog.setTitle("正在识别，请稍等......");
        progressDialog.setCancelable(true);
        takePhoto = findViewById(R.id.take_photo);
//        iv_show = findViewById(R.id.image_show);
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
                mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
                    @SuppressLint("ClickableViewAccessibility")
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        return manualFocuse(event);
                    }
                });
//                Log.d(TAG, "SurfaceView已生成");
                //设置点击监听
                takePhoto.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        camera.takePicture(new Camera.ShutterCallback() {
                            @Override
                            public void onShutter() {       //快门按下一瞬间会发生的操作
                                progressDialog.show();
                            }
                        }, new Camera.PictureCallback() {
                            @Override
                            public void onPictureTaken(byte[] data, Camera camera) {

                            }
                        },pictureCallback);
                    }
                });
                mSpeechSynthesizer.speak("请将手指指向文字识别区域进行拍摄");
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                // 销毁，释放资源
                if (camera != null) {
                    camera.stopPreview();
                    camera.setPreviewCallback(null);
                    camera.release();
                    camera = null;
                }
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
                final Camera.Parameters parameters = camera.getParameters();
                //设置预览照片的大小
                parameters.setPreviewFpsRange(viewWidth, viewHeight);
                //设置相机预览照片帧数
                parameters.setPreviewFpsRange(4, 10);
                //设置图片格式
                parameters.setPictureFormat(ImageFormat.JPEG);
                //图片质量
                parameters.set("jpeg-quality", 90);
                //设置自动对焦
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                //设置照片的大小
                parameters.setPictureSize(viewWidth, viewHeight);
                camera.cancelAutoFocus();
                //通过SurfaceView显示预览
                camera.setPreviewDisplay(mSurfaceHolder);
                //开始预览
                camera.startPreview();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static final int FOCUS_METERING_AREA_WEIGHT_DEFAULT = 1000;
    public static final int FOCUS_AREA_SIZE_DEFAULT = 300;

    private boolean manualFocuse( MotionEvent event) {
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            String focusMode = parameters.getFocusMode();
            Rect rect = calculateFocusArea(event.getX(), event.getY());
            List<Camera.Area> meteringAreas = new ArrayList<>();
            meteringAreas.add(new Camera.Area(rect, FOCUS_METERING_AREA_WEIGHT_DEFAULT));

            if (parameters.getMaxNumFocusAreas() != 0 && focusMode != null &&
                    (focusMode.equals(Camera.Parameters.FOCUS_MODE_AUTO) ||
                            focusMode.equals(Camera.Parameters.FOCUS_MODE_MACRO) ||
                            focusMode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) ||
                            focusMode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
            ) {
                if(!parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    return false; //cannot autoFocus
                }
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                parameters.setFocusAreas(meteringAreas);
                if (parameters.getMaxNumMeteringAreas() > 0) {
                    parameters.setMeteringAreas(meteringAreas);
                }
//                Log.e(TAG, "执行到手动对焦");
                camera.setParameters(parameters);
            } else if (parameters.getMaxNumMeteringAreas() > 0) {
                if(!parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    return false; //cannot autoFocus
                }
                camera.setParameters(parameters);

            } else {
            }
        }
        return false;
    }

    private Rect calculateFocusArea(float x, float y) {
        int buffer = FOCUS_AREA_SIZE_DEFAULT / 2;
        int centerX = calculateCenter(x, mSurfaceView.getWidth(), buffer);
        int centerY = calculateCenter(y, mSurfaceView.getHeight(), buffer);
        return new Rect(
                centerX - buffer,
                centerY - buffer,
                centerX + buffer,
                centerY + buffer
        );
    }

    private static int calculateCenter(float coord, int dimen, int buffer) {
        int normalized = (int) ((coord / dimen) * 2000 - 1000);
        if (Math.abs(normalized) + buffer > 1000) {
            if (normalized > 0) {
                return 1000 - buffer;
            } else {
                return -1000 + buffer;
            }
        } else {
            return normalized;
        }
    }


}
