package com.tencent.yolov8ncnn;


import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.List;

@Entity(tableName = "Result")
public class Result {

    @ColumnInfo(name = "result_id")
    @PrimaryKey(autoGenerate = true)
    int id =0 ;

    @ColumnInfo(name="date")
    String  date  ;

    @ColumnInfo(name="made")
    int made  ;

    @ColumnInfo(name="attempt")
    int attempt  ;

    @ColumnInfo(name="percent")
    float percent ;


    @ColumnInfo(name="mode")
    int mode;


    @TypeConverters(Converter.class)
    @ColumnInfo(name = "circles")
    List<Circle> circles ;



    @Ignore
    public Result(){

    }

    @Ignore
    public Result(int made, int attempt, float percent) {

        this.made = made;
        this.attempt = attempt;
        this.percent = percent;

    }

    @Ignore
    public Result(String date, int made, int attempt, float percent) {

        this.date = date;
        this.made = made;
        this.attempt = attempt;
        this.percent = percent;

    }
    public Result(int made, int attempt, float percent, List<Circle> circles) {

        this.made = made;
        this.attempt = attempt;
        this.percent = percent;
        this.circles = circles ;
    }

    @Ignore
    public Result(String date, int made, int attempt, float percent, List<Circle> circles) {

        this.date = date;
        this.made = made;
        this.attempt = attempt;
        this.percent = percent;
        this.circles = circles ;
    }
    @Ignore
    public Result(String date, int made, int attempt, float percent,int mode) {

        this.date = date;
        this.made = made;
        this.attempt = attempt;
        this.percent = percent;
        this.mode = mode;
    }



    @Ignore
    public Result(String date, int made, int attempt, float percent,int mode,List<Circle> circles) {

        this.date = date;
        this.made = made;
        this.attempt = attempt;
        this.percent = percent;
        this.mode = mode;
        this.circles = circles ;
    }



    public int getMode(){return mode;}

    public void setMode(int mode){this.mode = mode;}

    public int getId(){return id;}

    public String getDate(){return date;}

    public int getMade() {
        return made;
    }

    public void setMade(int made) {
        this.made = made;
    }

    public int getAttempt() {
        return attempt;
    }

    public void setAttempt(int attempt) {
        this.attempt = attempt;
    }

    public float getPercent() {
        return percent;
    }

    public void setPercent(float percent) {
        this.percent = percent;
    }

    public List<Circle> getCircles() {
        return circles;
    }

    public void setCircles(List<Circle> circles) {
        this.circles = circles;
    }


}


