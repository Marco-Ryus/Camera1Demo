package com.example.camera1demo.gson;

/**
 * @ProjectName : Camera1Demo
 * @Author : MarcoRys
 * @Time : 2021-03-25 21:48
 * @Description : 文件描述
 */
public class Bean {

    WordResult[] words_result;

    public WordResult[] getWords_result() {
        return words_result;
    }

    public static class WordResult {
        MyLocation location;

        String words;

        public MyLocation getLocation() {
            return location;
        }

        public String getWords() {
            return words;
        }
    }

    public static class MyLocation {
        int left;
        int top;
        int width;
        int height;

        public int getLeft() {
            return left;
        }

        public int getTop() {
            return top;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }
}
