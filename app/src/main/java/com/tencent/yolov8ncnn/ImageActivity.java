package com.tencent.yolov8ncnn;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;

public class ImageActivity extends AppCompatActivity {

    private YoloAPI yolo_api = new YoloAPI();
    private Yolov8Ncnn yolov8ncnn =new Yolov8Ncnn();

    private int current_model = 0;
    private int current_cpugpu = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        ImageView image_from_gallery = findViewById(R.id.imageView);

        boolean initializationSuccessful = yolo_api.Init(getAssets(), 0);

// crush
        reload();

        if (initializationSuccessful) {
            if (getIntent().hasExtra("IMAGE_URI")) {
                String imageUriString = getIntent().getStringExtra("IMAGE_URI");
                assert imageUriString != null;
                Log.d("Image_path_VideoActivity", imageUriString);

                Uri imageUri = Uri.parse(imageUriString);
                image_from_gallery.setImageURI(imageUri);

                Bitmap bitmap = null;
                try {
                    bitmap = uriToBitmap(imageUri);
                    // Now you have the Bitmap object, you can use it as needed
                } catch (IOException e) {
                    Log.d("uriToBitmap",e.getMessage());
                    // Handle any errors that may occur during the conversion
                }

                YoloAPI.Obj[] detectedObjects = YoloAPI.Detect(bitmap,true);
//                Log.d("detectedObjects", String.valueOf(detectedObjects.length));

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


        Button btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                onBackPressed();
                Intent intent =new Intent(ImageActivity.this,NavActivity.class);
                startActivity(intent);
                finish();
            }
        });

    }

    public Bitmap uriToBitmap(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        inputStream.close();
        return bitmap;
    }



    private void reload()
    {
        boolean ret_init = yolov8ncnn.loadModel(getAssets(), current_model, current_cpugpu);
        if (!ret_init)
        {
            Log.e("ImageActivity", "yolov8ncnn loadModel failed");
        }
    }



}