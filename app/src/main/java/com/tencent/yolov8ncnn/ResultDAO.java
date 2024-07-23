package com.tencent.yolov8ncnn;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;


@Dao
public interface ResultDAO {


    @Insert
    public void addResult(Result result);

    @Update
    public void updatedResult(Result result);

    @Delete
    public void deletedResult(Result result);

    @Query("select * from result")
    public List<Result> getAllResult();

    @Query("select * from result where result_id==:result_id")
    public Result getResult(int result_id);


    // Circle methods
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    void addCircle(Circle circle);
//
//    @Query("SELECT * FROM Circle WHERE result_id = :resultId")
//    List<Circle> getCirclesForResult(int resultId);



}
