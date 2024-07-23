package com.tencent.yolov8ncnn;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import com.tencent.yolov8ncnn.CustomDrawView.Circle;


public class DrawHotSpotView extends View {


    public static Circle srcTopLeft,srcTopRight,srcBottomLeft,srcBottomRight;
    private Circle destTopLeft,destTopRight,destBottomLeft,destBottomRight;
//    pts2 = np.float32([[0, 0], [0, 480], [864, 0], [864, 480]])
    private float[] destPoints = new float[8]; // Array to store destination points

//    player shooting pos
    public static List<Circle> blueCircles,updatedCircles;

    public static List<Circle> manualAttemptCircles ;
    public static List<Circle> manualMadeCircles ;
    public static Paint current_paint;

    boolean thisShoot = true;


    static int[] player_pos; //current
    public static List<Circle> detailedPagePlayerPos;
    public static List<Circle> updatedPlayerPos,originalPlayerPos ,madePos,missPos;
    private final OpenCvAPI opencv_api;
    private static int srcWidth ;
    private static int srcHeight ;
    private final Paint redCirclePaint,greenCirclePaint;



    public static boolean startRecord = false;

    public DrawHotSpotView(Context context, AttributeSet attrs) {
        super(context, attrs);
//        setWillNotDraw(false);
//        drawHotSpotView = new DrawHotSpotView(context, null);

        redCirclePaint = new Paint();
        redCirclePaint.setColor(Color.RED);
        redCirclePaint.setStyle(Paint.Style.FILL);

        greenCirclePaint = new Paint();
        greenCirclePaint.setColor(Color.GREEN);
        greenCirclePaint.setStyle(Paint.Style.FILL);

        Paint blueCirclePaint = new Paint();
        blueCirclePaint.setColor(Color.BLUE);
        blueCirclePaint.setStyle(Paint.Style.FILL);


        current_paint = new Paint();
        current_paint.setColor(Color.RED);
        current_paint.setStyle(Paint.Style.FILL);
//        destTopLeft= new Circle(0,0);
//        destTopRight= new Circle(864,0);
//        destBottomLeft= new Circle(0,480);
//        destBottomRight= new Circle(864,480);

        opencv_api = new OpenCvAPI();

        originalPlayerPos = new ArrayList<>();
        updatedPlayerPos = new ArrayList<>();

        detailedPagePlayerPos = new ArrayList<>();

        manualAttemptCircles = new ArrayList<>();
        manualMadeCircles = new ArrayList<>();
    }

//original pos
    public void setBlueCircles(List<Circle> blueCircles) {
        Log.d("setBlueCircles", "in");


//        DrawHotSpotView.blueCircles = blueCircles ;

        DrawHotSpotView.updatedCircles = transferCircle(blueCircles);

        current_paint = blueCircles.get(0).getPaint();
        Log.d("current_paint:", String.valueOf(current_paint));
//***************redraw CustomDrawView not itself
//        postInvalidate(); // Redraw the view to reflect the changes
//        invalidate();
    }


//    manual
    public void setAttemptCircles(List<Circle> circles) {
        manualAttemptCircles = transferCircle(circles);
        current_paint = circles.get(0).getPaint();
    }
    public void setMadeCircles(List<Circle> circles) {
        manualMadeCircles = transferCircle(circles);
        current_paint = circles.get(0).getPaint();
    }



    private void showOriginalPlayerPos(){
        int i =0 ;
        for (Circle originalPlayer : originalPlayerPos) {

            Log.d("originalPlayer:", i +":"+(originalPlayer.getX()) + "," + (originalPlayer.getY())+"," + originalPlayer.getRadius());
            i++;
        }
    }


//    @Override
//    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
//        super.onSizeChanged(w, h, oldw, oldh);
//
//        // Define the destination points for the four corners
//        destPoints[0] = 0;          // Top-left corner X
//        destPoints[1] = 0;          // Top-left corner Y
//        destPoints[2] = w;          // Top-right corner X
//        destPoints[3] = 0;          // Top-right corner Y
//        destPoints[4] = 0;          // Bottom-left corner X
//        destPoints[5] = h;          // Bottom-left corner Y
//        destPoints[6] = w;          // Bottom-right corner X
//        destPoints[7] = h;          // Bottom-right corner Y
//    }


    public static Point convertCircleToPoint(Circle circle) {
        // Get the center coordinates of the circle
        if (circle!= null){
            int centerX = (int) circle.getX();
            int centerY = (int) circle.getY();
            return new Point(centerX, centerY);
        }

        return new Point(-1, -1);
        // Create a new PointF object with the center coordinates


    }


    private Circle transferCircle_each(Circle originalCircle){
        Pair<PointF, Float> updatedCoordinate = opencv_api.convertCircleCoordinates(srcWidth, srcHeight,  convertCircleToPoint(originalCircle),  originalCircle.getRadius(), convertCircleToPoint(srcTopLeft),
                convertCircleToPoint(srcTopRight),convertCircleToPoint(srcBottomLeft),convertCircleToPoint(srcBottomRight));

        PointF updatedCenter = updatedCoordinate.first;
        float updatedRadius = 5;
        Paint paint = originalCircle.getPaint();

        return(new Circle(updatedCenter.x, updatedCenter.y, updatedRadius,paint));

    }

    //project to hotSpot view
    private List<Circle> transferCircle(List<Circle> originalCircles){
        List<Circle> updatedCircles =  new ArrayList<>();

        int i = 0;
        for (Circle circle : originalCircles){
            Log.d("originalPos", i +":"+(circle.getX()) + "," + (circle.getY())+"," + circle.getRadius()+','+ circle.getPaint());
//            Circle updatedCircle = new Circle(circle.getX(), circle.getY(), circle.getPaint());
//            Circle updatedCircle = new Circle(0, 0, circle.getPaint());


            Log.d("transferCircle",DrawHotSpotView.srcWidth+","+DrawHotSpotView.srcHeight);
            Pair<PointF, Float> updatedCoordinate = opencv_api.convertCircleCoordinates(srcWidth, srcHeight,  convertCircleToPoint(circle),  circle.getRadius(), convertCircleToPoint(srcTopLeft),
                    convertCircleToPoint(srcTopRight),convertCircleToPoint(srcBottomLeft),convertCircleToPoint(srcBottomRight));


            PointF updatedCenter = updatedCoordinate.first;

            //default radius = 5 (declare in opencv.cpp)
//            float updatedRadius = updatedCoordinate.second;
            float updatedRadius = 5;
            Paint paint = circle.getPaint();

//            Log.d("updated data","("+updatedCenter.x + ","+updatedCenter.y+"),+radius:"+updatedRadius+",circle get paint:"+circle.getPaint());


//            Circle updatedCircle = new Circle(updatedCenter.x, updatedCenter.y, updatedRadius, circle.getPaint());
            Circle updatedPlayer = new Circle(updatedCenter.x, updatedCenter.y, updatedRadius,paint);
            Log.d("updatedPos", i +":"+(updatedPlayer.getX()) + "," + (updatedPlayer.getY())+"," +updatedPlayer.getRadius()+','+updatedPlayer.getPaint());
            updatedCircles.add(updatedPlayer);
            i++;
        }

         return updatedCircles;
    }

    public static List<Circle> transferToDetailedPage(List<Circle> pos , int detailedPage_height){

        List<Circle> updatedCircles =  new ArrayList<>();

//        DrawHotSpotDetailedPageView Height
//        int detailedPage_height = 230 ;
        int original_height = 100 ;

        float ratio = (float) detailedPage_height /original_height ;


        if (pos != null && !pos.isEmpty()) {
            for (Circle p : pos){

//                float landscapeX = p.getX(); // X coordinate in landscape mode
//                float landscapeY = p.getY(); // Y coordinate in landscape mode
//
//                float portraitX = landscapeY * ratio;
//                float portraitY = (srcHeight - landscapeX) * ratio;
//
//                Circle c = new Circle(portraitX,portraitY,p.getRadius(),p.getPaint());


                float originalX = p.getX();
                float originalY = p.getY();

                Circle c = new Circle(originalX * ratio,originalY * ratio,p.getRadius()*3 ,p.getPaint());

                updatedCircles.add(c);
            }

        }
        return updatedCircles;

    }



    private void addNewPlayerPos_manual(){

        player_pos = Yolov8Ncnn.getPlayerPos();

        if (player_pos != null){

            Log.d("original_pos(cv) :", "("+player_pos[0]+","+player_pos[1]+")");
            // convert 座標系 from cv to canvas
            player_pos = convertCvToCanvas(player_pos);

            Log.d("original_pos(canvas):", "("+player_pos[0]+","+player_pos[1]+")");


            Circle player_pos_circle = new Circle(player_pos[0],player_pos[1],current_paint);
            originalPlayerPos.add(player_pos_circle);
            updatedPlayerPos.add(transferCircle_each(player_pos_circle));

        }
//        updatedPlayerPos = transferCircle(originalPlayerPos);


    }

    private void addNewPlayerPos(){

//        player_pos = Yolov8Ncnn.getPlayerPos();
        player_pos = Yolov8Ncnn.getPlayerPos_ob();

        if (player_pos != null){

            Log.d("original_pos(cv) :", "("+player_pos[0]+","+player_pos[1]+")");
            // convert 座標系 from cv to canvas
            player_pos = convertCvToCanvas(player_pos);

            Log.d("original_pos(canvas):", "("+player_pos[0]+","+player_pos[1]+")");


            Circle player_pos_circle = new Circle(player_pos[0],player_pos[1],current_paint);
            originalPlayerPos.add(player_pos_circle);
            updatedPlayerPos.add(transferCircle_each(player_pos_circle));

        }
//        updatedPlayerPos = transferCircle(originalPlayerPos);

    }

    int[] convertCvToCanvas(int[] original_pos){
        //need to check isn't (-1,-1)
        int [] cv_dim = Yolov8Ncnn.getCVdimension();
        int [] canvas_dim = CustomDrawView.CanvasDimension;

        Log.d("dimensionnn", String.format("cv(%d,%d), canvas(%d,%d)", cv_dim[0], cv_dim[1], canvas_dim[0], canvas_dim[1]));

        //放大率 (canvas/cv)
        float times = (float) canvas_dim[0] /cv_dim[0];

        return new int[]{ (canvas_dim[0]-(int)(original_pos[1]*times)), (int) (original_pos[0]*times)};
    }

    //miss
    private void drawCross(Canvas canvas,Circle updatedPlayer){
        float centerX = updatedPlayer.getX();
        float centerY = updatedPlayer.getY();
        float radius = updatedPlayer.getRadius();

        float crossLength = radius * 1.5f; // Adjust the length of the cross arms as needed


        // Draw first line (top-left to bottom-right)
        canvas.drawLine(centerX - crossLength, centerY - crossLength, centerX + crossLength, centerY + crossLength, updatedPlayer.getPaint());

// Draw second line (top-right to bottom-left)
        canvas.drawLine(centerX + crossLength, centerY - crossLength, centerX - crossLength, centerY + crossLength, updatedPlayer.getPaint());

    }

    private void showUpdatedPlayerPos(Canvas canvas){

        if (updatedPlayerPos != null && !updatedPlayerPos.isEmpty()) {

            //now can't change mode (paint,mid,three)
            startRecord = true;

            Log.d("updatedPlayerPos.size", String.valueOf(updatedPlayerPos.size()));
            int i = 0;
            for (Circle updatedPlayer : updatedPlayerPos) {

                Log.d("updatedPlayerPos","("+updatedPlayer.getX()+","+updatedPlayer.getY()+")");

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
//                canvas.drawCircle(updatedPlayer.getX(), updatedPlayer.getY(), updatedPlayer.getRadius(),redCirclePaint );


            }

        }
    }


//manual
    private void showTestPos(Canvas canvas ,List<Circle> circles){

        if (circles != null && !circles.isEmpty()) {
            startRecord = true;
//            Log.d(".length", "in");
//            Log.d("updatedCircles.size", String.valueOf(updatedCircles.size()));
            int i = 0;
            for (Circle updatedPlayer : circles) {

                Log.d("updatedPlayerPos","("+updatedPlayer.getX()+","+updatedPlayer.getY()+")");

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

                i++;


            }
        }
    }

    static int score = 0;
    private boolean isScore(){
        int s = Yolov8Ncnn.getScore();
        if (score != s) {
            score = s;
            Log.d("isScore","yes");
            return true;
        }
        else return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        Log.d("HotSpot_onDraw", "in");

        srcWidth = getWidth();
        srcHeight = getHeight();
        Log.d("HotSpot Canvas dimension", "("+getWidth()+","+getHeight()+")");
//        canvas.drawCircle(0, 0, 30,blueCirclePaint);


        //pose detect shot
//會莫名進入,導致current paint didn't set
//        if ( Yolov8Ncnn.isShooting() && isShoot){
//
//
//            isShoot = false;
//            Log.d("Yolov8Ncnn.isShooting()","shooting");
//            addNewPlayerPos();
//
//        }
//        else if (!Yolov8Ncnn.isShooting()) isShoot = true;


        //Yolov8Ncnn.isShooting_ob() 從出手到再次持球,都是回傳true,持球後回傳false
        //ob detect shot
        boolean is_shoot = Yolov8Ncnn.isShooting_ob();

        //出手的瞬間 (default miss)
        if (is_shoot && thisShoot){

            Log.d("Yolov8Ncnn.isShooting()","shooting");
            addNewPlayerPos();
            thisShoot = false;
        }

        //持球的瞬間
        else if (!is_shoot) thisShoot = true;



        if (!thisShoot && isScore() && updatedPlayerPos != null && !updatedPlayerPos.isEmpty())
        {
            Log.d("SET GREEN","IN");
            updatedPlayerPos.get(updatedPlayerPos.size() - 1).setPaint(greenCirclePaint);
            showUpdatedPlayerPos(canvas);

            //detailedPagePlayerPos = transferToDetailedPage(updatedPlayerPos);
        }



        showUpdatedPlayerPos(canvas);

        showTestPos(canvas,manualAttemptCircles);
        showTestPos(canvas,manualMadeCircles);

        //if (manualAttemptCircles != null && !manualAttemptCircles.isEmpty())
            //detailedPagePlayerPos = transferToDetailedPage(manualAttemptCircles);

        //if (manualMadeCircles != null && !manualMadeCircles.isEmpty())
            //detailedPagePlayerPos = transferToDetailedPage(manualMadeCircles);



        invalidate();




    }



}


