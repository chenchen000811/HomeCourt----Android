package com.tencent.yolov8ncnn;

import android.graphics.Paint;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "Circle")
//@TypeConverters(PaintConverter.class)
public class Circle {
    @PrimaryKey(autoGenerate = true)
    int id;

    @ColumnInfo(name = "result_id")
    int resultId;

    @ColumnInfo(name = "x")
    float x;

    @ColumnInfo(name = "y")
    float y;

    @ColumnInfo(name = "radius")
    float radius;

    @ColumnInfo(name = "color")
    int color; // Store color as integer

    @Ignore
    public Circle() {}

    public Circle(int resultId, float x, float y, float radius, int color) {
        this.resultId = resultId;
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = color;
    }

    public Circle(int resultId, float x, float y, float radius, Paint paint) {
        this(resultId, x, y, radius, paint.getColor());
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public int getResultId() {
        return resultId;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getRadius() {
        return radius;
    }

    public int getColor() {
        return color;
    }

    public Paint getPaint() {
        Paint paint = new Paint();
        paint.setColor(color);
        return paint;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void setColor(Paint paint) {
        this.color = paint.getColor();
    }

    public void setResultId(int resultId) {
        this.resultId = resultId;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public void setId(int id) {
        this.id = id ;
    }
}