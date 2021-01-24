package com.example.camera1demo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.camera1demo.fragment.FingerFragment;

import java.io.FileNotFoundException;

public class ImageShow extends AppCompatActivity implements View.OnClickListener {
    public ImageView imageView;
    private TextView textView;
    private String result;
    private Button fingerShow;
    private Button reTake;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_show);
        initView();
    }

    //初始化界面
    private void initView() {
        imageView = findViewById(R.id.show_image);
        textView = findViewById(R.id.result);
        fingerShow = findViewById(R.id.show_finger);
        fingerShow.setOnClickListener(this);
        reTake = findViewById(R.id.re_take_photo);
        reTake.setOnClickListener(this);

        Bundle bundle = getIntent().getExtras();
        result = bundle.getString("result");
        String[] locations = bundle.getStringArray("location");
        Bitmap bitmap = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory()+"/result.jpg");
        if (result!=null) {
            int top = Integer.parseInt(locations[0]);
            int left = Integer.parseInt(locations[1]);
            int width = Integer.parseInt(locations[2]);
            int height = Integer.parseInt(locations[3]);
            bitmap = Bitmap.createBitmap(bitmap, left,top, width, height);
            textView.setText(result);
            int i = MainActivity.mSpeechSynthesizer.speak(result);
            Log.e("ImageShow", String.valueOf(i));
        } else {
            textView.setText("无识别结果");
            int i = MainActivity.mSpeechSynthesizer.speak("无识别结果");
            Log.e("ImageShow", String.valueOf(i));
        }
        imageView.setImageBitmap(bitmap);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.show_finger:
                showFragement();
                break;
            case R.id.re_take_photo:
                reTakePhoto();
                break;
            default:
                break;
        }
    }

    private void reTakePhoto() {
//        Intent intent = new Intent(ImageShow.this, MainActivity.class);
//        startActivity(intent);
        finish();
    }

    private void showFragement() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.finger_layout, new FingerFragment());
        transaction.addToBackStack(null);
        transaction.commit();
    }
}