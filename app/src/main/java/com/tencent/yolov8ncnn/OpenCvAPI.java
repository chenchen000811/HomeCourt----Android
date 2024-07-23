package com.tencent.yolov8ncnn;

import android.graphics.Point;
import android.graphics.PointF;
import android.util.Pair;

public class OpenCvAPI {

    // Load the native library
//    static {
//        System.loadLibrary("opencv");
//    }


//    public native Matrix getMatrix(int srcWidth, int srcHeight, byte[] srcData, Point tl, Point tr, Point bl, Point br);
//    public native Pair<Integer, Integer> calculateUpdatedPosition(int originalX, int originalY, float[] transformationMatrix);

    public native Pair<PointF, Float> convertCircleCoordinates(int srcWidth, int srcHeight, Point center, float radius, Point tl, Point tr, Point bl, Point br);
}

