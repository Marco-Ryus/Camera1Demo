package com.example.camera1demo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.io.File;
import java.util.Vector;

import static java.lang.Math.abs;
import static org.opencv.core.CvType.CV_8UC3;
import static org.opencv.imgproc.Imgproc.circle;
import static org.opencv.imgproc.Imgproc.line;
import static org.opencv.imgproc.Imgproc.moments;

public class DataPro {
    private static final String TAG = "DataPro";
    private String[][] data;
    private File file;
    private int tmp = 0;

    public DataPro setData(String[][] data) {
        this.data = data;
        return this;
    }

    public DataPro setFile(File file) {
        this.file = file;
        return this;
    }

    public String[] processData() {
        Bitmap bitmap = BitmapFactory.decodeFile(file.toString());
        double[] point = newFingerScan(bitmap);
        if (point.length > 0) {
            int res = prePocessText(point, data);
            if (res != -1) {
                return data[res];
            }
        }
        return null;
    }
    //  测试回滚
    private int prePocessText(double[] point, String[][] data) {
        double px = point[0], py = point[1];
        for (int i = 0; i < data.length; i++) {
            //将文字区域转换为int
            int top = Integer.parseInt(data[i][0]), left = Integer.parseInt(data[i][1]), width = Integer.parseInt(data[i][2]), height = Integer.parseInt(data[i][3]);
            //手指完全在区域内
            if ((left - tmp) < px && px < (left + width + tmp)) {
                if (top - tmp < py && py < top + height + tmp) {
                    return i;
                }
            }
        }
        int cnt = -1;
        double min = Double.MAX_VALUE;
        double distance;
        //指尖坐标未找到与文字区域重合的，需要计算最短距离
        for (int i = 0; i < data.length; i++) {
            int top = Integer.parseInt(data[i][0]), left = Integer.parseInt(data[i][1]), width = Integer.parseInt(data[i][2]), height = Integer.parseInt(data[i][3]);
            //先判断当前文字选框在指尖点上方还是下方
            if (top + height > py) {
                //判断当前文字选框是否在指尖完全左侧
                if (px > left + width) {
                    distance = Math.pow((px - left - width), 2) + Math.pow((py - top - height), 2);
                    if (distance < min) {
                        min = distance;
                        cnt = i;
                        continue;
                    }
                } else if (px < left) {
                    distance = Math.pow((px - left), 2) + Math.pow((py - top - height), 2);
                    if (distance < min) {
                        min = distance;
                        cnt = i;
                        continue;
                    }
                } else {
                    distance = Math.pow((py - top - height), 2);
                    if (distance < min) {
                        min = distance;
                        cnt = i;
                        continue;
                    }
                }
            } else if (top > py) {
                if (px > left + width) {
                    distance = Math.pow((px - left - width), 2) + Math.pow((py - top - height), 2);
                    if (distance < min) {
                        min = distance;
                        cnt = i;
                        continue;
                    }
                } else if (px < left) {
                    distance = Math.pow((px - left), 2) + Math.pow((py - top), 2);
                    if (distance < min) {
                        min = distance;
                        cnt = i;
                        continue;
                    }
                } else {
                    distance = Math.pow((py - top), 2);
                    if (distance < min) {
                        min = distance;
                        cnt = i;
                        continue;
                    }
                }
            } else {
                if (px > left + width) {
                    distance = Math.pow((px - left - width), 2);
                    if (distance < min) {
                        min = distance;
                        cnt = i;
                        continue;
                    }
                } else {
                    distance = Math.pow((px - left), 2);
                    if (distance < min) {
                        min = distance;
                        cnt = i;
                        continue;
                    }
                }
            }
        }
        return cnt;
    }

    private double[] newFingerScan(Bitmap bitmap) {
        Mat mat_src = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);//获取原图(8位无符号的四通道，带透明的RGB图像)
        Utils.bitmapToMat(bitmap, mat_src);//将bitmap转化成mat_src
        Mat mat_gray = new Mat(mat_src.cols(), mat_src.rows(), CvType.CV_8UC1);//灰度图，单通道
        Imgproc.cvtColor(mat_src, mat_gray, Imgproc.COLOR_BGRA2GRAY, 1);//转变颜色  RGB转灰度图

        Mat frame = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);//取出原图
        bitmap.recycle();
        mat_src.copyTo(frame);//复制，不会牵连改变

        Mat frameHSV = new Mat(mat_src.cols(), mat_src.rows(), CV_8UC3);//三通道的RGB图像
        // hsv空间(色调，饱和度，明度)
        Mat mask = new Mat(mat_src.cols(), mat_src.rows(), CvType.CV_8UC1);
        Mat dst = new Mat(mat_src.cols(), mat_src.rows(), CV_8UC3); // 输出图像
        //dst.copyTo(frame);
        // 中值滤波，去除椒盐噪声
        Imgproc.medianBlur(frame, frame, 5);
        Imgproc.cvtColor(frame, frameHSV, Imgproc.COLOR_RGB2HSV, 3);//把frame的颜色空间转换后复制到frameHSV
        Mat dstTemp1 = new Mat(mat_src.cols(), mat_src.rows(), CvType.CV_8UC1);//两个单通道图像
        Mat dstTemp2 = new Mat(mat_src.cols(), mat_src.rows(), CvType.CV_8UC1);
        // 对HSV空间进行量化，得到二值图像，亮的部分为手的形状
        Core.inRange(frameHSV, new Scalar(5, 10, 20), new Scalar(20, 170, 256), mask);//比较三个通道中的元素是否在相应的区间类，不在的画的则改成255，即符合肤色的就转换为黑色

        // 形态学操作，去除噪声，并使手的边界更加清晰
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));//定义一个合适大小的核
        Imgproc.erode(mask, mask, element);//扩大暗区，腐蚀
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN, element);//对输入图像执行开运算，先腐蚀再膨胀，通过去除图像的毛刺凸起进行滤波
        Imgproc.dilate(mask, mask, element);//扩大亮区，膨胀
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, element);//执行闭运算，先膨胀再腐蚀，通过填充图像的凹角进行滤波

        frame.copyTo(dst, mask);

        Vector<MatOfPoint> contours = new Vector<MatOfPoint>();

        MatOfInt4 hierarchy = new MatOfInt4();

        // 得到手的轮廓
        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);//将轮廓的数据存入到contours动态数组中

        Moments moment = moments(mask, false);
        Point center = new Point(moment.m10 / moment.m00, moment.m01 / moment.m00);//计算图形重心；m00为零阶矩阵，m10为一阶矩阵，计算结果为图像重心
        circle(mat_src, center, 8, new Scalar(0, 0, 255), -1);//绘制图像重心
        //图像显示
        Vector<MatOfPoint2f> newcont = new Vector<MatOfPoint2f>();
        for (MatOfPoint point : contours) {
            MatOfPoint2f newPoint = new MatOfPoint2f(point.toArray());
            newcont.add(newPoint);
        }
        Vector<MatOfPoint2f> cont = new Vector<>();   //用于存储各轮廓相应的拟合曲线
        for (int i = 0; i < newcont.size(); i++) {
            MatOfPoint2f first = newcont.get(i);
            MatOfPoint2f second = new MatOfPoint2f();
            Imgproc.approxPolyDP(first, second, 80, true);   //用指定的精度逼近多边形曲线，第一个参数为输入的轮廓点集合，第二个参数为输出的逼近曲线的轮廓点集合，第三为逼近精度，最后是是否闭合
            cont.add(second);
        }
        Vector<MatOfPoint> polyedges = new Vector<>();
        for (MatOfPoint2f point : cont) {
            MatOfPoint nPoint = new MatOfPoint(point.toArray());//将各个轮廓中的点作为数组导出
            polyedges.add(nPoint);
        }

        for (int j = 0; j < polyedges.size(); j++) {
            for (int i = 0; i < polyedges.get(j).toArray().length - 1; i++) {
                line(mat_src, polyedges.get(j).toArray()[i], polyedges.get(j).toArray()[i + 1], new Scalar(255, 255, 255), 5);  //将各个轮廓的点用直线连接起来，用于观察算法效果，实际运行可取消该循环步骤
            }
            line(mat_src, polyedges.get(j).toArray()[polyedges.get(j).toArray().length - 1], polyedges.get(j).toArray()[0], new Scalar(255, 255, 255), 5);//将该轮廓第一个与最后一个点相连，即封闭操作
        }
//        找出最大轮廓
        int index = 0;
        double area = 0, maxArea = 0;
        for (int i = 0; i < polyedges.size(); i++) {
            area = Imgproc.contourArea(polyedges.get(i));
            if (area > maxArea) {
                maxArea = area;
                index = i;
            }
        }
        MatOfPoint couPoint = polyedges.get(index);//确定最大轮廓
        Point[] a = couPoint.toArray();

        Vector<Point> fingerTips = new Vector<Point>();     //用于储存指尖点
        Point tmp = new Point();
        double max = 0;
        int notice = 0;
        for (int i = 0; i < a.length; i++) {
            tmp = a[i];
            double dist = (tmp.x - center.x) * (tmp.x - center.x) + (tmp.y - center.y) * (tmp.y - center.y);//计算两点距离
            if (dist > max) {
                max = dist;
                notice = i;
            }
            // 挑选可能为指尖的点
            if (dist != max) {
                max = 0;
                boolean flag = false;
                // 低于手心的点不算
                if (center.y < a[notice].y)
                    continue;
                // 离得太近的不算
                for (int j = 0; j < fingerTips.size(); j++) {
                    if (abs(a[notice].x - fingerTips.get(j).x) < 40) {
                        flag = true;
                        break;
                    }
                }
                fingerTips.add(a[notice]);
//                circle(dst, a[notice], 6, new Scalar(255, 255, 255), -1);   //绘制指尖
                circle(mat_src, a[notice], 10, new Scalar(0, 255, 0), -1);//用绿色圈描绘可能为指尖的点
//                line(dst, center, a[notice], new Scalar(0, 255, 255), 5);
            }
        }
        double dist1 = 0;
//        Log.v(TAG,fingerTips.get(0).toString());

        if (fingerTips.size() > 0 && !fingerTips.get(0).toString().isEmpty()) {
            double min = fingerTips.get(0).y;
            int notice1 = 0;
            for (int i = 0; i < fingerTips.size(); i++) {
                dist1 = fingerTips.get(i).y;
                if (dist1 < min) {
                    min = dist1;
                    notice1 = i;
                }
            }
            circle(mat_src, fingerTips.get(notice1), 10, new Scalar(255, 0, 0), -1);//绘制最终粉色点为指尖
            Log.e(TAG, "指尖坐标x: " + fingerTips.get(notice1).x + "指尖坐标y: " + fingerTips.get(notice1).y);
            double[] result = {fingerTips.get(notice1).x, fingerTips.get(notice1).y};
            return result;
        } else {
            Log.e(TAG, "未找到指尖");
            return new double[0];
        }

    }
   /* private double[] fingerSkin(Bitmap bitmap) {
        Mat mat_src = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);//获取原图(8位无符号的四通道，带透明的RGB图像)
        Utils.bitmapToMat(bitmap, mat_src);//将bitmap转化成mat_src
        Mat mat_gray = new Mat(mat_src.cols(), mat_src.rows(), CvType.CV_8UC1);//灰度图，单通道
        Imgproc.cvtColor(mat_src, mat_gray, Imgproc.COLOR_BGRA2GRAY, 1);//转变颜色  RGB转灰度图
        Mat frame = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);//取出原图
        mat_src.copyTo(frame);//复制，不会牵连改变

        Mat frameHSV = new Mat(mat_src.cols(), mat_src.rows(), CV_8UC3);//三通道的RGB图像

        // hsv空间(色调，饱和度，明度)
        Mat mask = new Mat(mat_src.cols(), mat_src.rows(), CvType.CV_8UC1);
        Mat dst = new Mat(mat_src.cols(), mat_src.rows(), CV_8UC3); // 输出图像\

        // 中值滤波，去除椒盐噪声
        Imgproc.medianBlur(frame, frame, 5);
        Imgproc.cvtColor(frame, frameHSV, Imgproc.COLOR_RGB2HSV, 3);//把frame的颜色空间转换后复制到frameHSV
        Mat dstTemp1 = new Mat(mat_src.cols(), mat_src.rows(), CvType.CV_8UC1);//两个单通道图像
        Mat dstTemp2 = new Mat(mat_src.cols(), mat_src.rows(), CvType.CV_8UC1);
        Core.inRange(frameHSV, new Scalar(5, 10, 20), new Scalar(26, 170, 256), mask);//比较三个通道中的元素是否在相应的区间类，不在的画的则改成255，即符合肤色的就转换为黑色

        // 形态学操作，去除噪声，并使手的边界更加清晰
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));//定义一个合适大小的核
        Imgproc.erode(mask, mask, element);//扩大暗区，腐蚀
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN, element);//对输入图像执行开运算，先腐蚀再膨胀，通过去除图像的毛刺凸起进行滤波
        Imgproc.dilate(mask, mask, element);//扩大亮区，膨胀
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, element);//执行闭运算，先膨胀再腐蚀，通过填充图像的凹角进行滤波

        frame.copyTo(dst, mask);
        Vector<MatOfPoint> contours=new Vector<MatOfPoint>();
        MatOfInt4 hierarchy=new MatOfInt4();

        // 得到手的轮廓
        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);//将轮廓的数据存入到contours动态数组中

        Moments moment = moments(mask,false);
        Point center = new Point(moment.m10 / moment.m00, moment.m01 / moment.m00);//计算图形重心；m00为零阶矩阵，m10为一阶矩阵，计算结果为图像重心
        circle(mat_src, center, 8, new Scalar(0, 0, 255), -1);//绘制图像重心
        //图像显示
        Vector<MatOfPoint2f> newcont=new Vector<MatOfPoint2f>();
        for(MatOfPoint point : contours) {
            MatOfPoint2f newPoint = new MatOfPoint2f(point.toArray());
            newcont.add(newPoint);
        }
        Vector<MatOfPoint2f> cont=new Vector<>();   //用于存储各轮廓相应的拟合曲线
        for(int i=0;i<newcont.size();i++)
        {
            MatOfPoint2f first=newcont.get(i);
            MatOfPoint2f second=new MatOfPoint2f();
            Imgproc.approxPolyDP(first,second, 80, true);   //用指定的精度逼近多边形曲线，第一个参数为输入的轮廓点集合，第二个参数为输出的逼近曲线的轮廓点集合，第三为逼近精度，最后是是否闭合
            cont.add(second);
        }
        Vector<MatOfPoint> polyedges = new Vector<>();
        for(MatOfPoint2f point : cont) {
            MatOfPoint nPoint = new MatOfPoint(point.toArray());//将各个轮廓中的点作为数组导出
            polyedges.add(nPoint);
        }

        *//*for(int j=0;j<polyedges.size();j++) {
            for (int i = 0; i < polyedges.get(j).toArray().length - 1; i++) {
                line(mat_src, polyedges.get(j).toArray()[i], polyedges.get(j).toArray()[i + 1], new Scalar(255, 255, 255), 5);  //将各个轮廓的点用直线连接起来，用于观察算法效果，实际运行可取消该循环步骤
            }
            line(mat_src,polyedges.get(j).toArray()[polyedges.get(j).toArray().length - 1],polyedges.get(j).toArray()[0], new Scalar(255,255,255), 5);//将该轮廓第一个与最后一个点相连，即封闭操作

        }*//*

//        找出最大轮廓
        int index = 0;
        double area = 0, maxArea=0;
        for (int i=0;i < polyedges.size(); i++)
        {
            area = Imgproc.contourArea(polyedges.get(i));
            if (area > maxArea)
            {
                maxArea = area;
                index = i;
            }
        }
        MatOfPoint couPoint = polyedges.get(index);//确定最大轮廓
        Point[] a=couPoint.toArray();

        Vector<Point> fingerTips = new Vector<Point>();     //用于储存指尖点
        Point tmp = new Point();
        double max = 0;
        int notice = 0;
        for (int i = 0; i < a.length; i++) {
            tmp = a[i];
            double dist = (tmp.x - center.x) * (tmp.x - center.x) + (tmp.y - center.y) * (tmp.y - center.y);//计算两点距离
            if (dist > max) {
                max = dist;
                notice = i;
            }
            // 挑选可能为指尖的点
            if (dist != max) {
                max = 0;
                boolean flag = false;
                // 低于手心的点不算
                if (center.y < a[notice].y)
                    continue;
                // 离得太近的不算
                for (int j = 0; j < fingerTips.size(); j++) {
                    if (abs(a[notice].x - fingerTips.get(j).x) < 40) {
                        flag = true;
                        break;
                    }
                }
                fingerTips.add(a[notice]);
//                circle(mat_src, a[notice], 10, new Scalar(0, 255, 0), -1);//用绿色圈描绘可能为指尖的点
            }
        }
        double dist1=0;
        if(!fingerTips.get(0).toString().isEmpty()){
            double min=fingerTips.get(0).y;
            int notice1=0;
            for(int i=0;i<fingerTips.size();i++){
                dist1=fingerTips.get(i).y;
                if(dist1<min){
                    min=dist1;
                    notice1=i;
                }
            }
//            circle(mat_src,fingerTips.get(notice1),10,new Scalar(255, 0, 0), -1);//绘制最终粉色点为指尖
            Log.d(TAG,"指尖坐标x: "+ fingerTips.get(notice1).x +"指尖坐标y: "+fingerTips.get(notice1).y);
            double[] result = {fingerTips.get(notice1).x, fingerTips.get(notice1).y};
            return result;
*//*            Bitmap bmp_gray = Bitmap.createBitmap(mat_gray.cols(), mat_gray.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mat_src, bmp_gray);*//*
        }else{
            Log.e(TAG, "未找到指尖");
            return null;
        }

    }*/
}
