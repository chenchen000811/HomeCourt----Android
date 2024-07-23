package com.tencent.yolov8ncnn;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class NavActivity extends AppCompatActivity {

    ActivityResultLauncher<Intent> resultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nav);

//        Button realTime_btn = findViewById(R.id.realTime_btn);
//
//        realTime_btn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent =new Intent(NavActivity.this,MainActivity.class);
//                startActivity(intent);
//                finish();
//            }
//        });

        ImageView start_btn = findViewById(R.id.start_btn);
        start_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent =new Intent(NavActivity.this,HistoryActivity.class);
                startActivity(intent);
                finish();
            }
        });

        //registerResult();



//        Button video_btn = findViewById(R.id.video_btn);
//        video_btn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//                intent.setType("video/*");
//                //startActivityForResult(intent,1);
//                //finish();
//
//                //Intent intent = new Intent(MediaStore);
//                resultLauncher.launch(intent);
//            }
//        });
//
//
//        Button image_btn = findViewById(R.id.image_btn);
//        image_btn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//                intent.setType("image/*");
//                //startActivityForResult(intent,1);
//                //finish();
//
//                //Intent intent = new Intent(MediaStore);
//                resultLauncher.launch(intent);
//            }
//        });


    }


//    private void registerResult(){
//        resultLauncher = registerForActivityResult(
//                new ActivityResultContracts.StartActivityForResult(),
//                new ActivityResultCallback<ActivityResult>() {
//                    @Override
//                    public void onActivityResult(ActivityResult result) {
//                        try {
//                            Uri mediaUri = result.getData().getData();
//                            String mimeType = getContentResolver().getType(mediaUri);
//
//                            if (mimeType != null && mimeType.startsWith("image/")) {
//                                // The selected media is an image
////                                Toast.makeText(NavActivity.this, "Please select a video file", Toast.LENGTH_SHORT).show();
//                                Intent intent =new Intent(NavActivity.this,ImageActivity.class);
//                                intent.putExtra("IMAGE_URI", mediaUri.toString());
//                                startActivity(intent);
//                                finish();
//                            }
//                            else if (mimeType != null && mimeType.startsWith("video/")) {
//                                // The selected media is an image
////                                Toast.makeText(NavActivity.this, "Please select a video file", Toast.LENGTH_SHORT).show();
//                                Intent intent =new Intent(NavActivity.this,VideoActivity.class);
//                                intent.putExtra("VIDEO_URI", mediaUri.toString());
//                                startActivity(intent);
//                                finish();
//                            }
//
//
//
//                        }catch (Exception e){
//                            Toast.makeText(NavActivity.this,"no media select",Toast.LENGTH_SHORT).show();
//
//                        }
//                    }
//                }
//        );
//    }




}