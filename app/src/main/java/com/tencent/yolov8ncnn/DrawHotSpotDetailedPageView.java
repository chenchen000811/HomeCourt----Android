package com.tencent.yolov8ncnn;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DrawHotSpotDetailedPageView extends View {

    //current item circles
    public List<CustomDrawView.Circle> pos = new ArrayList<>() ;

    //detect by AI
    public List<CustomDrawView.Circle> pos1;

    //manual detect
    public List<CustomDrawView.Circle> pos_manual_attempt;
    public List<CustomDrawView.Circle> pos_manual_made;
    private final Paint redCirclePaint,greenCirclePaint;
    public static int desiredHeight = 0;

    public int midWidth = 0 ;
    public int midHeight = 0 ;
    ResultDatabase resultDB;
    Result result = new Result();

    public DrawHotSpotDetailedPageView(Context context, AttributeSet attrs) {
        super(context,attrs);


        RoomDatabase.Callback myCallBack = new RoomDatabase.Callback() {
            @Override
            public void onCreate(@NonNull SupportSQLiteDatabase db) {
                super.onCreate(db);
            }

            @Override
            public void onOpen(@NonNull SupportSQLiteDatabase db) {
                super.onOpen(db);
            }
        };

        Log.d("DrawDetailed","IN");

        try{
            resultDB = Room.databaseBuilder(context, ResultDatabase.class, "ResultDB")
                    .addCallback(myCallBack).build();

        }catch (Exception e){
            Log.e("DrawDetailed", "Error", e);
        }


        getResultListInBackground();

        Log.d("DrawDetailed","OUT");

 //        Log.d("desiredHeight", String.valueOf(desiredHeight));

//        if (Tab1Fragment.updatedCircles != null && !Tab1Fragment.updatedCircles.isEmpty()){
//            Log.d("currentPos", String.valueOf((Tab1Fragment.currentPos)));
//            Log.d("SIZE", String.valueOf((Tab1Fragment.updatedCircles.size())));
//            pos = Tab1Fragment.updatedCircles.get(Tab1Fragment.currentPos);
////            Log.d("pos(0).x", String.valueOf(pos.get(0).getX()));
//
////            why pos.size == 0
//        }


//        Source from "current" DrawHotSpotView
//        pos1 = DrawHotSpotView.detailedPagePlayerPos;
//        pos1 = DrawHotSpotView.transferToDetailedPage(DrawHotSpotView.updatedPlayerPos,desiredHeight);
//        pos_manual_attempt = DrawHotSpotView.transferToDetailedPage(DrawHotSpotView.manualAttemptCircles,desiredHeight);
//        pos_manual_made = DrawHotSpotView.transferToDetailedPage(DrawHotSpotView.manualMadeCircles,desiredHeight);

//        if (pos1 != null && !pos1.isEmpty()) {
//            // pos1 is not null and not empty
//            // Perform desired operations here
//            pos.addAll(pos1);
//        }
//
//        if (pos_manual_attempt != null && !pos_manual_attempt.isEmpty()) {
//            // pos_manual_attempt is not null and not empty
//            // Perform desired operations here
//            pos.addAll(pos_manual_attempt);
//        }
//
//        if (pos_manual_made != null && !pos_manual_made.isEmpty()) {
//            // pos_manual_made is not null and not empty
//            // Perform desired operations here
//            pos.addAll( pos_manual_made);
//        }





        redCirclePaint = new Paint();
        redCirclePaint.setColor(Color.RED);
        redCirclePaint.setStyle(Paint.Style.FILL);



        greenCirclePaint = new Paint();
        greenCirclePaint.setColor(Color.GREEN);
        greenCirclePaint.setStyle(Paint.Style.FILL);

//


    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int currentTabMode = HistoryActivity.currentTabMode;

        // Get the width size and mode from the widthMeasureSpec
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);


//        int measuredWidth = widthSize;

//        Log.d("onMeasure_detail (px)","("+ widthSize +","+desiredHeight+")");


        switch (currentTabMode) {

            //full  (px)
            case 0:
                desiredHeight = (int) (widthSize / 1.7);

                break;

            //mid
            case 1:
                widthSize /= 1.7;
                desiredHeight = (int) (widthSize * 0.8);

                break;

            //paint
            case 2:
                float full_to_paint =  (float) 170 / 60;
                widthSize /= full_to_paint;
                desiredHeight = widthSize;


        }
        setMeasuredDimension(widthSize , desiredHeight);



        desiredHeight = pxToDp(desiredHeight);
    }



    @Override
    protected void onDraw(@NonNull Canvas canvas) {

        super.onDraw(canvas);


        try {
            if ( pos != null && !pos.isEmpty()) {

            Log.d("pos.size", String.valueOf(pos.size()));

                for (CustomDrawView.Circle updatedPlayer : pos) {

                    Log.d("detailed","in");

                    int color = updatedPlayer.getPaint().getColor();
                    Log.d("Color", "Paint color: " + Integer.toHexString(color));

                    //made
                    if (color == greenCirclePaint.getColor()){
                        canvas.drawCircle(updatedPlayer.getX(), updatedPlayer.getY(), updatedPlayer.getRadius(), updatedPlayer.getPaint());

                    }
                    //attempt(miss)
                    else if (color == redCirclePaint.getColor()){
                        drawCross(canvas,updatedPlayer);

                    }

                }
            }else {
                Log.d("detailed","out");
            }

            invalidate();

        }catch(Exception e){
            Log.e("DrawDetailedOnDraw", "Error", e);
        }



    }


    private void drawCross(Canvas canvas, CustomDrawView.Circle updatedPlayer){
        float centerX = updatedPlayer.getX();
        float centerY = updatedPlayer.getY();
        float radius = updatedPlayer.getRadius();

        float crossLength = radius * 1.5f; // Adjust the length of the cross arms as needed

        Paint paint = updatedPlayer.getPaint();
        paint.setStrokeWidth(10);
        // Draw first line (top-left to bottom-right)
        canvas.drawLine(centerX - crossLength, centerY - crossLength, centerX + crossLength, centerY + crossLength, paint);

// Draw second line (top-right to bottom-left)
        canvas.drawLine(centerX + crossLength, centerY - crossLength, centerX - crossLength, centerY + crossLength, paint);

    }



    private int pxToDp(int px) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (px / density + 0.5f);
    }



    public void getResultListInBackground(){




            ExecutorService executorService = Executors.newSingleThreadExecutor();

            Handler handler = new Handler(Looper.getMainLooper());


            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    //background

                    Log.d("currentId", String.valueOf(Tab1Fragment.currentId));

                    //Mode
                    switch (HistoryActivity.currentTabMode) {
                        //Full
                        case 0:
                            try {

                                result = resultDB.getResultDAO().getResult(Tab1Fragment.currentId);
                            }catch (Exception e){
                                Log.e("DrawDetailedBg", "Error", e);
                            }
                            break;
                        // Mid
                        case 1:
                            try {
                                result = resultDB.getResultDAO().getResult(Tab2Fragment.currentId);
                            }catch (Exception e){
                                Log.e("DrawDetailedBg", "Error", e);
                            }
                            break;
                        //Paint
                        case 2:
                            try {
                                result = resultDB.getResultDAO().getResult(Tab3Fragment.currentId);
                            }catch (Exception e){
                                Log.e("DrawDetailedBg", "Error", e);
                            }

                    }




                    Log.d("resultId", String.valueOf(result.getId()));
                    Log.d("resultMade", String.valueOf(result.getMade()));
//                if (result != null) {
//                    List<Circle> circles = resultDB.getResultDAO().getCirclesForResult(Tab1Fragment.currentId);
//                    result.setCir   cles(circles);
//                }

                    //on finishing task
                    handler.post(new Runnable() {
                        @Override
                        public void run() {



//
                            if (result.getCircles() != null) {
                                Log.d("resultCircleSize", String.valueOf(result.getCircles().size()));
                                pos = Tab1Fragment.transferDBtoCustom(result.getCircles());
                            } else {
                                Log.d("resultCircleSize", "null");
                            }
//
//
//                        Log.d("pos size" , String.valueOf(pos.size()));

                            resultDB.close();
                        }
                    });
                }
            });

    }

}
