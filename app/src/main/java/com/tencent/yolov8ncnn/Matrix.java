package com.tencent.yolov8ncnn;

import java.util.List;

public class Matrix {
    private int width;
    private int height;
    private List<Byte> transformedData;
//    private Byte transformedData;

    public Matrix(int width, int height, List<Byte> transformedData) {
        this.width = width;
        this.height = height;
        this.transformedData = transformedData;
    }
//    public Matrix(int width, int height, Byte transformedData) {
//        this.width = width;
//        this.height = height;
//        this.transformedData = transformedData;
//    }
}
