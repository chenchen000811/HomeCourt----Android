package com.tencent.yolov8ncnn;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

public class YoloAPI {

    public static String[] CLASSNAME ={"ball","made", "person", "rim", "shoot"};

//    public static String[] CLASSNAME ={"person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat", "traffic light",
//                    "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep", "cow",
//                    "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee",
//                    "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove", "skateboard", "surfboard",
//                    "tennis racket", "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
//                    "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
//                    "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote", "keyboard", "cell phone",
//                    "microwave", "oven", "toaster", "sink", "refrigerator", "book", "clock", "vase", "scissors", "teddy bear",
//                    "hair drier", "toothbrush"};

    public native boolean Init(AssetManager mgr, int cpugpu);

    public static class Obj {
        public float x, y, w, h, prob;
        public int label;

    }

    //bitmap (for image/video)
    public static native Obj[] Detect(Bitmap bitmap, boolean use_gpu);





}