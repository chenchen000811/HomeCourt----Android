package com.tencent.yolov8ncnn;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import androidx.room.TypeConverters;





@Database(entities = {Result.class, Circle.class},version = 2, exportSchema = false)
@TypeConverters(Converter.class)
public abstract class ResultDatabase extends RoomDatabase {

//    private static ResultDatabase instance;
    public abstract ResultDAO getResultDAO();

    public abstract CircleDao circleDao();
//    public static synchronized ResultDatabase getInstance(Context context) {
//        if (instance == null) {
//            instance = Room.databaseBuilder(context.getApplicationContext(),
//                            ResultDatabase.class, "result_database")
//                    .fallbackToDestructiveMigration()
//                    .build();
//        }
//        return instance;
//    }

    private static volatile ResultDatabase INSTANCE;

    public static ResultDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (ResultDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    ResultDatabase.class, "result_database")
                            .fallbackToDestructiveMigration()  // This will recreate the database if a migration is missing
                            .build();
                }
            }
        }
        return INSTANCE;
    }


}
