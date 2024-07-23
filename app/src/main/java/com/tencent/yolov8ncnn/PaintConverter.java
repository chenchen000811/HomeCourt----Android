package com.tencent.yolov8ncnn;

import android.graphics.Paint;

import androidx.room.TypeConverter;

public class PaintConverter {

    @TypeConverter
    public static Integer fromPaint(Paint paint) {
        return paint == null ? null : paint.getColor();
    }

    @TypeConverter
    public static Paint toPaint(Integer color) {
        if (color == null) {
            return null;
        }
        Paint paint = new Paint();
        paint.setColor(color);
        return paint;
    }
}