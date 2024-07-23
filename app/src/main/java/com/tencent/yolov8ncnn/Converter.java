package com.tencent.yolov8ncnn;
import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class Converter {
    @TypeConverter
    public static String fromCircleList(List<Circle> circles) {
        if (circles == null) {
            return (null);
        }
        Gson gson = new Gson();
        return gson.toJson(circles);
    }

    @TypeConverter
    public static List<Circle> toCircleList(String data) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Circle>>() {}.getType();
        return gson.fromJson(data, listType);
    }
}