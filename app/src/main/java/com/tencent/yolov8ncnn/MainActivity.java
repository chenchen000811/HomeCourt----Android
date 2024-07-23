// Tencent is pleased to support the open source community by making ncnn available.
//
// Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
//
// Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the License at
//
// https://opensource.org/licenses/BSD-3-Clause
//
// Unless required by applicable law or agreed to in writing, software distributed
// under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// CONDITIONS OF ANY KIND, either express or implied. See the License for the
// specific language governing permissions and limitations under the License.

package com.tencent.yolov8ncnn;


//first change

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;

import android.media.MediaPlayer;
//real-time
public class MainActivity extends Activity implements SurfaceHolder.Callback
{
    public static final int REQUEST_CAMERA = 100;

    private Yolov8Ncnn yolov8ncnn = new Yolov8Ncnn();

    //0: front, 1: back
    private int facing = 1;

//    private Spinner spinnerModel;
    private Spinner spinnerCPUGPU;


    //0:FULL,1:MID,2:PAINT
    private int mode = 0;
    private int current_model = 0;


//    GPU
    private int current_cpugpu = 1;

    private SurfaceView cameraView;

    private TextView scoreTextView,attemptTextView;

    private Spinner spinnerMode;
    private View darkOverlay;
    Handler handler = new Handler();
    Runnable runnable;

    //1s
    int delay = 1000;

    boolean isPause = false;

    ResultDatabase resultDB;


    private MediaPlayer scoreMediaPlayer;
    private MediaPlayer missMediaPlayer;

    static int current_score = 0;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);



        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        DrawHotSpotView drawHotSpotView = findViewById(R.id.drawHotSpotView);

        darkOverlay = findViewById(R.id.darkOverlay);


        View customLayout = LayoutInflater.from(this).inflate(R.layout.custom_alert_dialog, null);

// Get the layout params of the custom layout
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        );

// Set the desired height (in pixels) for the AlertDialog
//        layoutParams.height = 1000; // Set your desired height here

// Apply the layout params to the custom layout
        customLayout.setLayoutParams(layoutParams);

// Create AlertDialog.Builder with custom layout
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(customLayout);

// Create AlertDialog
        AlertDialog alertDialog = alertDialogBuilder.create();

// Show AlertDialog
        alertDialog.show();



        View stopLayout = LayoutInflater.from(this).inflate(R.layout.stop_record_alert, null);

// Get the layout params of the custom layout
        LinearLayout.LayoutParams layoutParams_1 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        );

// Set the desired height (in pixels) for the AlertDialog
//        layoutParams.height = 1000; // Set your desired height here

// Apply the layout params to the custom layout
        stopLayout.setLayoutParams(layoutParams_1);

// Create AlertDialog.Builder with custom layout
        AlertDialog.Builder alertDialogBuilder_1 = new AlertDialog.Builder(this);
        alertDialogBuilder_1.setView(stopLayout);


        ImageView stop_btn = findViewById(R.id.stop_imgV);
        stop_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

//         Show AlertDialog
                dialog();
            }

        });


//Mode

        // set spinner (mode)
        spinnerMode = findViewById(R.id.spinnerMode);
        String[] entries = {"FULL", "MID", "PAINT"};
// Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, entries);
// Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
// Apply the adapter to the spinner
        spinnerMode.setAdapter(adapter);

// Listen for item selection changes
        spinnerMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                mode = position;

                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) drawHotSpotView.getLayoutParams();
                switch (position) {

                    case 0: // 三分線後一步
                        layoutParams.width = dpToPx(170);
                        layoutParams.height = dpToPx(100);
                        break;

                    case 1: // 底線三分與paint中點、半圓弧頂點
                        layoutParams.width = dpToPx(100);
                        layoutParams.height = dpToPx(80);
                        break;

                    case 2: // Paint
                        layoutParams.width = dpToPx(60);
                        layoutParams.height = dpToPx(60);
                        break;
                    default:
                        break;
                }
                drawHotSpotView.setLayoutParams(layoutParams);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });



        cameraView = (SurfaceView) findViewById(R.id.cameraview);
        cameraView.getHolder().setFormat(PixelFormat.RGBA_8888);
        cameraView.getHolder().addCallback(this);

        scoreTextView = (TextView) findViewById(R.id.score_txt);
        attemptTextView =(TextView) findViewById(R.id.attempt_txt);





//        Button buttonSwitchCamera = (Button) findViewById(R.id.buttonSwitchCamera);
//        buttonSwitchCamera.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View arg0) {
//
//                int new_facing = 1 - facing;
//
//                yolov8ncnn.closeCamera();
//
//                yolov8ncnn.openCamera(new_facing);
//
//                facing = new_facing;
//            }
//        });


//        Button btnBack = (Button) findViewById(R.id.btnBack);
//
//        btnBack.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
////                onBackPressed();
//                Intent intent =new Intent(MainActivity.this,NavActivity.class);
//                startActivity(intent);
//                finish();
//            }
//        });

        ImageView restart_btn= findViewById(R.id.restart_imgV);
       //restart
        restart_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                onBackPressed();
//                Intent intent =new Intent(MainActivity.this,NavActivity.class);
//                startActivity(intent);
//                finish();


                try {
                    resetViews();
                    resetVariables();
                    restartActivity();


                } catch (Exception e) {
                    Log.e("MainActivity", "Error restarting activity", e);
                    Toast.makeText(MainActivity.this, "Error restarting activity", Toast.LENGTH_LONG).show();
                }

            }
        });



        TextView pause_txt = findViewById(R.id.pause_txt);
        ImageView pause_btn = findViewById(R.id.pause_imgV);
        pause_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               isPause = !isPause;
               if(isPause){
                   yolov8ncnn.closeCamera();
                   darkOverlay.setVisibility(View.VISIBLE);
                   pause_txt.setVisibility(View.VISIBLE);

               }else{
                   yolov8ncnn.openCamera(facing);
                   darkOverlay.setVisibility(View.GONE);
                   pause_txt.setVisibility(View.GONE);
               }
            }
        });


//        spinnerModel = (Spinner) findViewById(R.id.spinnerModel);
//        spinnerModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id)
//            {
//                if (position != current_model)
//                {
//                    current_model = position;
//                    reload();
//                }
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> arg0)
//            {
//            }
//        });

//        spinnerCPUGPU = (Spinner) findViewById(R.id.spinnerCPUGPU);
//        spinnerCPUGPU.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id)
//            {
//                if (position != current_cpugpu)
//                {
//                    current_cpugpu = position;
//                    reload();
//                }
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> arg0)
//            {
//            }
//        });

        Log.d("database create","before");
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

        Log.d("database create","after");

        reload();





        resultDB = Room.databaseBuilder(getApplicationContext(),ResultDatabase.class,"ResultDB")
                .addCallback(myCallBack).build();

        Log.d("database create","ok");



        //audio
        initMediaPlayers();

    }
    private void initMediaPlayers() {
        scoreMediaPlayer = MediaPlayer.create(this, R.raw.score);
//        missMediaPlayer = MediaPlayer.create(context, R.raw.miss);
    }


//end record -> save to database
    private void dialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View customView = LayoutInflater.from(this).inflate(R.layout.stop_record_alert, null);

//        View customDrawView =
        builder.setView(customView);

        // Show the dialog
        AlertDialog dialog = builder.create();
        // Find the elements in the custom layout
        TextView titleTextView = customView.findViewById(R.id.dialog_title);
        Button negativeButton = customView.findViewById(R.id.negative_button);
        Button positiveButton = customView.findViewById(R.id.positive_button);

        yolov8ncnn.closeCamera();
        darkOverlay.setVisibility(View.VISIBLE);

        // Set the title text
        titleTextView.setText("確定要結束紀錄嗎 ?");

        // Set the positive button action
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Perform positive button action
                dialog.dismiss();
                darkOverlay.setVisibility(View.GONE);

                //save to database
                int score = yolov8ncnn.getScore();
                int manual_score = CustomDrawView.mades_manual;
                score += manual_score;

                int attempt = yolov8ncnn.getAttempt_ob();
                int manual_attempt = CustomDrawView.attempts_manual;
                attempt += manual_attempt;

                float percent = 0;

                if (attempt!=0) {
                    percent = (float) score /attempt;
                }


                //mode



                //hot spot
                List<CustomDrawView.Circle> pos = new ArrayList<>() ;
//                List<Circle> pos1;
                List<CustomDrawView.Circle> pos1;
                List<CustomDrawView.Circle> pos_manual_attempt;
                List<CustomDrawView.Circle> pos_manual_made;

                int desiredHeight = 210;
                pos1 = DrawHotSpotView.transferToDetailedPage(DrawHotSpotView.updatedPlayerPos,desiredHeight);
                pos_manual_attempt = DrawHotSpotView.transferToDetailedPage(DrawHotSpotView.manualAttemptCircles,desiredHeight);
                pos_manual_made = DrawHotSpotView.transferToDetailedPage(DrawHotSpotView.manualMadeCircles,desiredHeight);


                if (!pos1.isEmpty()) {
                    // pos1 is not null and not empty
                    // Perform desired operations here
                    pos.addAll(pos1);
                }

                if (!pos_manual_attempt.isEmpty()) {
                    // pos_manual_attempt is not null and not empty
                    // Perform desired operations here
                    pos.addAll(pos_manual_attempt);
                }

                if (!pos_manual_made.isEmpty()) {
                    // pos_manual_made is not null and not empty
                    // Perform desired operations here
                    pos.addAll( pos_manual_made);
                }

//                Log.d("pos_size", String.valueOf(pos.size()));

                Result r1 = new Result(getCurrentDate(),score,attempt,percent,mode);

                if(!pos.isEmpty()){
                    r1.setCircles(transferCircleToDB(pos,r1.getId()));
                    Log.d("r1.getCircles.size", String.valueOf( r1.getCircles().size()));
                }


                addResultInBackground(r1);

//                Toast.makeText(MainActivity.this,"Saved",Toast.LENGTH_LONG).show();

                resetViews();
                resetVariables();

                showSavedToast();



            }
        });

        // Set the negative button action
        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Perform negative button action
                yolov8ncnn.openCamera(facing);
                dialog.dismiss();
                darkOverlay.setVisibility(View.GONE);
            }
        });

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                // Perform dialog close action
                yolov8ncnn.openCamera(facing);
                darkOverlay.setVisibility(View.GONE);
            }
        });

        dialog.show();
    }



    private List<Circle> transferCircleToDB(List<CustomDrawView.Circle> circles,int resultId){


        List<Circle> circleDB = new ArrayList<>();
        if (circles!= null && !circles.isEmpty()){
            for (CustomDrawView.Circle circle: circles){
                Circle db = new Circle(resultId,circle.getX(),circle.getY(),circle.getRadius(),circle.getPaint());
                circleDB.add(db);

            }

            Log.d("circleDB.size", String.valueOf(circleDB.size()));
        }

        return  circleDB;
    }


    // Helper method to convert dp to pixels
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void reload()
    {
        boolean ret_init = yolov8ncnn.loadModel(getAssets(), current_model, current_cpugpu);
//        boolean ret_init2 = yolov8ncnn.loadModel2(getAssets(), current_model, current_cpugpu);
//        boolean ret_init2 = yolov8ncnn.loadModel2(getAssets(), 1, current_cpugpu);
        if (!ret_init)
        {
            Log.e("MainActivity", "yolov8ncnn loadModel ob failed");
        }
//        if (!ret_init2)
//        {
//            Log.e("MainActivity", "yolov8ncnn loadModel pose detect failed");
//        }
    }



    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        yolov8ncnn.setOutputWindow(holder.getSurface());
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
    }

    @Override
    public void onResume()
    {
        handler.postDelayed(runnable = new Runnable() {
            public void run() {
                handler.postDelayed(runnable, delay);
                updateAttempt();
                updateScore();
                lockShootMode();
//                Toast.makeText(MainActivity.this, "This method is run every 10 seconds",
//                        Toast.LENGTH_SHORT).show();
            }
        }, delay);

        super.onResume();

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED)
        {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA);
        }

        yolov8ncnn.openCamera(facing);
    }

    @Override
    public void onPause()
    {
        handler.removeCallbacks(runnable); //stop handler when activity not visible super.onPause();
        super.onPause();

        yolov8ncnn.closeCamera();
    }
    public void updateScore() {

        int score = yolov8ncnn.getScore();
        int manual = CustomDrawView.mades_manual;

        //score
        if (current_score != score + manual){

            current_score = score + manual;
            scoreTextView.setText(String.valueOf( current_score));
            playScoreSound();
        }

//        scoreTextView.setText(String.valueOf(manual));
    }

    public void updateAttempt() {
//        int attempt = yolov8ncnn.getAttempt();
        int attempt_ob = yolov8ncnn.getAttempt_ob();
        int manual = CustomDrawView.attempts_manual;

//        attemptTextView.setText(String.valueOf(attempt+manual));
        attemptTextView.setText(String.valueOf(attempt_ob + manual));
//        attemptTextView.setText(String.valueOf( manual));
    }

    public void lockShootMode(){
        if (DrawHotSpotView.startRecord){
            spinnerMode.setEnabled(false);

        }
    }

    public void addResultInBackground(Result result){
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Handler handler = new Handler(Looper.getMainLooper());

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                //background

//                Log.d("result.getCircles.size", String.valueOf( result.getCircles().size()));

                try {

                    resultDB.getResultDAO().addResult(result);
                    Log.d("DatabaseOperation", "Result added successfully");
                } catch (Exception e) {
                    Log.e("DatabaseOperation", "Error adding result", e);
                }




//                if (result.getCircles() != null) {
//                    for (Circle circle : result.getCircles()) {
//                        circle.setResultId(result.getId());
//                        resultDB.getResultDAO().addCircle(circle);
//                    }
//                }
                //on finishing task
                handler.post(new Runnable() {
                    @Override
                    public void run() {
//                        Toast.makeText(MainActivity.this,"Added to database",Toast.LENGTH_LONG).show();
                    }
                });
                resultDB.close();
            }
        });
    }


    void showSavedToast(){
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.toast_saved, findViewById(R.id.toast_saved_layout));

        TextView toastText = layout.findViewById(R.id.toast_text);
        toastText.setText("Saved");

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
        Intent intent =new Intent(MainActivity.this,HistoryActivity.class);
        startActivity(intent);
        finish();

    }



    private void resetViews() {
        scoreTextView.setText("0");
        attemptTextView.setText("0");
        CustomDrawView.mades_manual = 0;
        CustomDrawView.attempts_manual = 0;

        DrawHotSpotView.startRecord = false;
        spinnerMode.setEnabled(true);

        if (DrawHotSpotView.updatedPlayerPos != null) {
            DrawHotSpotView.updatedPlayerPos.clear();
        } else {
            DrawHotSpotView.updatedPlayerPos = new ArrayList<>();
        }

        if (DrawHotSpotView.manualAttemptCircles != null) {
            DrawHotSpotView.manualAttemptCircles.clear();
        } else {
            DrawHotSpotView.manualAttemptCircles = new ArrayList<>();
        }

        if (DrawHotSpotView.manualMadeCircles != null) {
            DrawHotSpotView.manualMadeCircles.clear();
        } else {
            DrawHotSpotView.manualMadeCircles = new ArrayList<>();
        }

        darkOverlay.setVisibility(View.GONE);
    }

    private void resetVariables() {
        yolov8ncnn.resetScore();
        yolov8ncnn.resetAttempt_ob();
    }

    private void restartActivity() {
        Intent intent = new Intent(MainActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private String getCurrentDate(){
        // Get the current date
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1; // Note: Month starts from 0
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        return String.format("%02d/%02d", month, day);

    }


    private void playScoreSound() {
        if (scoreMediaPlayer != null) {
            scoreMediaPlayer.start();
        }
    }

    public void releaseMediaPlayers() {
        if (scoreMediaPlayer != null) {
            scoreMediaPlayer.release();
            scoreMediaPlayer = null;
        }


    }
}
