package com.tencent.yolov8ncnn;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.VideoView;

import java.util.List;



public class VideoActivity extends AppCompatActivity {

    private YoloAPI yolo_api = new YoloAPI();
    private Yolov8Ncnn yolov8ncnn =new Yolov8Ncnn();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        boolean initializationSuccessful = yolo_api.Init(getAssets(), 0);

        VideoView video_from_gallery = findViewById(R.id.videoV);

        reload();

        if (initializationSuccessful) {
            // Proceed with further operations
            if (getIntent().hasExtra("VIDEO_URI")) {
                String videoUriString = getIntent().getStringExtra("VIDEO_URI");
                assert videoUriString != null;
                Log.d("Video_path_VideoActivity",videoUriString);

                Uri videoUri = Uri.parse(videoUriString);

                try {
                    if (videoUri != null) {
                        // Extract frames from the video
                        List<Bitmap> frames = VideoProcessor.extractFramesFromVideo(getApplicationContext(),videoUriString);
                        Log.d("frame.size", String.valueOf(frames.size()));


//                        Log.e("VideoActivity","CAN'T reload()");
                        for (Bitmap frame : frames) {

//                            問題在這!!!!!!!!!!!!!!
                            YoloAPI.Obj[] detectedObjects = YoloAPI.Detect(frame,true);

//                            if (detectedObjects != null) {
//                                // Handle the case where no objects were detected
//                                try {
//                                    Log.d("detectedObjects", "label:"+String.valueOf(detectedObjects[0].label)+",prob:"+detectedObjects[0].prob);
//                                }
//                                catch (Exception e){
//                                    Log.e("YoloAPI.Detect",e.getMessage());
//                                }
//                            }

                            //detect somethings
                            if (detectedObjects.length != 0) {
                                int count = 0 ;
                                for (YoloAPI.Obj detectedObject : detectedObjects){
                                    count+=1;

                                    Log.d("detectedObjects", count+",label:"+YoloAPI.CLASSNAME[detectedObject.label]+",prob:"+detectedObject.prob);
                                }
                                // Handle the case where no objects were detected

                            }


                        }
                    }
                    else {
                        Log.e("VideoActivity", "No video URI found in intent");
                    }
                }
                catch (Exception ex) {
                        // Log any other unexpected exceptions
                        Log.e("VideoActivity", "Unexpected error: " + ex.getMessage());


                }

                //show video on screen
            video_from_gallery.setVideoURI(videoUri);
            video_from_gallery.start();
            }
        } else {
            // Handle initialization failure
            Log.e("VideoActivity", "Failed to initialize YoloAPI");
            return;
        }





        Button btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                onBackPressed();
                Intent intent =new Intent(VideoActivity.this,NavActivity.class);
                startActivity(intent);
                finish();
            }
        });


    }
    private void reload()
    {
        boolean ret_init = yolov8ncnn.loadModel(getAssets(), 0, 0);
        if (!ret_init)
        {
            Log.e("MainActivity", "yolov8ncnn loadModel failed");
        }
    }
}