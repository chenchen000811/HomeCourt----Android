package com.tencent.yolov8ncnn;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CircleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addCircle(Circle circle);

    @Delete
    void deleteCircle(Circle circle);

    @Update
    void updateCircle(Circle circle);

    @Query("select* from Circle where result_id = :resultId")
    List<Circle> getCirclesForResult(int resultId);
}