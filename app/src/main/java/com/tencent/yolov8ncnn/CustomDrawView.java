package com.tencent.yolov8ncnn;



import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
//import androidx.core.view.GestureDetectorCompat;
import java.util.ArrayList;
import java.util.List;


public class CustomDrawView extends  View
{
    private List<Circle> redCircles;
    private List<Circle> blueCircles;
    private List<Circle> attemptCircles;
    private Paint circlePaint;
    Paint blueCirclePaint,greenCirclePaint;
    private Paint textPaint;
    private int maxRedCircles = 4;

    private DrawHotSpotView drawHotSpotView;

    private Yolov8Ncnn yolov8ncnn = new Yolov8Ncnn();

    public static int [] CanvasDimension = {-1,-1} ;

    public static int attempts_manual = 0 ,mades_manual= 0 ;

    private GestureDetector mDetector;
//    private GestureDetectorCompat mDetector;
    public CustomDrawView(Context context, AttributeSet attrs) {
        super(context, attrs);

        drawHotSpotView = new DrawHotSpotView(context, attrs);

        redCircles = new ArrayList<>();
        blueCircles = new ArrayList<>();
        attemptCircles = new ArrayList<>();

        circlePaint = new Paint();
        circlePaint.setColor(Color.RED);
        circlePaint.setStyle(Paint.Style.FILL);

        blueCirclePaint = new Paint();
        blueCirclePaint.setColor(Color.BLUE);
        blueCirclePaint.setStyle(Paint.Style.FILL);

        greenCirclePaint = new Paint();
        greenCirclePaint.setColor(Color.GREEN);
        greenCirclePaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(30);
        textPaint.setTextAlign(Paint.Align.CENTER);

//        mDetector = new GestureDetectorCompat(this.getContext(), new MyGestureListener());
        mDetector = new GestureDetector(this.getContext(), new MyGestureListener());

    }


    @Override
    public boolean onTouchEvent(MotionEvent event){
        if (this.mDetector.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }


    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final String DEBUG_TAG = "Gestures";


        @Override
        public boolean onDown(MotionEvent event) {
            Log.d(DEBUG_TAG,"onDown: " + event.toString());
            return true;
        }


        //made
        @Override
        public boolean onDoubleTap(@NonNull MotionEvent e) {
            Log.d(DEBUG_TAG, "onLongPress: " + e.toString() + e.toString());
            super.onLongPress(e);
            if (redCircles.size() == maxRedCircles)
            {
                float x = e.getX();
                float y = e.getY();

                // Add a blue circle at the double click position
                blueCircles.add(new Circle(x, y,greenCirclePaint));
//                drawHotSpotView.setBlueCircles(blueCircles);
                drawHotSpotView.setMadeCircles(blueCircles);
                attempts_manual ++;
                mades_manual ++;
                // Redraw the view
//                    invalidate();
            }
            return  false;
         }

        //miss
        @Override
        public void onLongPress(@NonNull MotionEvent e) {


            Log.d(DEBUG_TAG, "onDoubleTapEvent: " + e.toString() + e.toString());
            float x = e.getX();
            float y = e.getY();
            boolean circleRemoved = false;

            // Check if the touch event is within any existing red circle
            for (int i = 0; i < redCircles.size(); i++) {
                Circle circle = redCircles.get(i);
                if (Math.pow(x - circle.getX(), 2) + Math.pow(y - circle.getY(), 2) <= Math.pow(circle.getRadius(), 2)) {
                    // Remove the circle if it's touched again
                    redCircles.remove(i);
                    circleRemoved = true;
                    break;
                }
            }

            // Add a new red circle if there are less than maxRedCircles and no circle was removed
            if (!circleRemoved && redCircles.size() < maxRedCircles) {
                redCircles.add(new Circle(x, y));
            }

            // Check if the four red circles have been added, and assign them as source points


            if (redCircles.size() == maxRedCircles && flag){
                attempts_manual ++;
                // Add a blue circle at the double click position
                attemptCircles.add(new Circle(x, y,circlePaint));
                //drawHotSpotView.setBlueCircles(attemptCircles);
                drawHotSpotView.setAttemptCircles(attemptCircles);
            }

            if (redCircles.size() == maxRedCircles) {

                DrawHotSpotView.srcTopLeft = redCircles.get(0);
                DrawHotSpotView.srcTopRight = redCircles.get(1);
                DrawHotSpotView.srcBottomLeft = redCircles.get(2);
                DrawHotSpotView.srcBottomRight = redCircles.get(3);


                flag = true;

            }

            // Redraw the view
            invalidate();

        }
    }



    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);


        Log.d("Canvas dimension", "("+getWidth()+","+getHeight()+")");
        CanvasDimension[0] = getWidth();
        CanvasDimension[1] = getHeight();

        // Draw red circles and text
        for (Circle circle : redCircles) {
            canvas.drawCircle(circle.getX(), circle.getY(), circle.getRadius(), circlePaint);
            canvas.drawText("("+circle.getX()+","+circle.getY()+")", circle.getX(), circle.getY() - circle.getRadius() - 20, textPaint);
//            canvas.drawText(getCirclePositionText(circle), circle.getX(), circle.getY() - circle.getRadius() - 20, textPaint);
        }

        // Draw blue circles
//        for (Circle circle : blueCircles) {
//            canvas.drawCircle(circle.getX(), circle.getY(), circle.getRadius(), circle.getPaint());
//        }
    }


    public void updateAttempt() {
        String attempt = String.valueOf(yolov8ncnn.getAttempt()+1);

    }
    public static boolean flag = false ;


    private String getCirclePositionText(Circle circle) {
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        if (circle.getX() < centerX && circle.getY() < centerY) {
            return "Left-Top";
        } else if (circle.getX() > centerX && circle.getY() < centerY) {
            return "Right-Top";
        } else if (circle.getX() < centerX && circle.getY() > centerY) {
            return "Left-Bottom";
        } else if (circle.getX() > centerX && circle.getY() > centerY) {
            return "Right-Bottom";
        }

        return "";
    }

//    @Override
//    public void onClick(View view) {

//    }

//    @Override
//    public boolean onLongClick(View view) {
//        Log.i("MyGesture", "onLongclick");
//        if (redCircles.size() == maxRedCircles){
//            float x = view.getX();
//            float y = view.getY();
//
//            // Add a blue circle at the double click position
//            blueCircles.add(new Circle(x, y,greenCirclePaint));
//            drawHotSpotView.setBlueCircles(blueCircles);
//            attempts_manual ++;
//            mades_manual ++;
//            // Redraw the view
////                    invalidate();
////
//        }
//        return true;
//    }

    public static class Circle {
        private float x, y;
        private float radius = 30;
        private Paint paint;

        public Circle(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public Circle(float x, float y, Paint paint) {
            this.x = x;
            this.y = y;
            this.paint = paint;
        }

        public Circle(float x, float y, float updatedRadius, Paint paint) {
            this.x = x;
            this.y = y;
            this.radius = updatedRadius;
            this.paint = paint;
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
        public Paint getPaint() {
            return paint;
        }
        public void setPaint(Paint paint) {
            this.paint = paint;
        }

        public void setX(float x){this.x = x;}

        public void setY(float y){this.y = y;}

        public void setRadius(float radius){this.radius = radius;}
    }

    public Circle convertIntArrayToCircle(int[] centerCoordinates) {
        if (centerCoordinates.length != 2) {
            throw new IllegalArgumentException("Center coordinates must contain exactly 2 values.");
        }

        float x = centerCoordinates[0];
        float y = centerCoordinates[1];

        return new Circle(x, y);
    }
}
