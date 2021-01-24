package com.example.camera1demo.fragment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.camera1demo.R;

/**
 * @ProjectName : Camera1Demo
 * @Author : MarcoRys
 * @Time : 2021-01-24 14:27
 * @Description : 文件描述
 */
public class FingerFragment extends Fragment {
    private ImageView imageView;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.finger_fragment, container, false);
        Bitmap bitmap = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory()+"/finger.jpg");
        imageView = view.findViewById(R.id.finger_result);
        imageView.setImageBitmap(bitmap);
        return view;
    }
}
