package com.example.camera1demo;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

public class ImageShow extends AppCompatActivity {
    public ImageView imageView;
    private TextView textView;
    private String result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_show);

        imageView = findViewById(R.id.show_image);
        textView = findViewById(R.id.result);

        Bundle bundle = getIntent().getExtras();
        result = bundle.getString("result");
        String[] locations = bundle.getStringArray("location");
        Bitmap bitmap = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory() + "/temp.jpg");
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
}