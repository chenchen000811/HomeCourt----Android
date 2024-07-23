package com.tencent.yolov8ncnn;

import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

public class DetailedActivity extends AppCompatActivity {

    DrawHotSpotDetailedPageView drawHotSpotDetailedPageView ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detailed);

        Log.d("detailed.java","in");
        drawHotSpotDetailedPageView = findViewById(R.id.drawHotSpotDetailedPageView);
        if (drawHotSpotDetailedPageView != null) {

            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) drawHotSpotDetailedPageView.getLayoutParams();
            Log.d("drawHotSpotDetailedPageView","1");

            int currentWidth = layoutParams.width; // Get the current width of the view
//            int currentWidth = 200;
            int desiredHeight = (int) (currentWidth / 1.7);
            Log.d("drawHotSpotDetailedPageView","2");
            layoutParams.height = dpToPx(desiredHeight);
            Log.d("drawHotSpotDetailedPageView","3");
            drawHotSpotDetailedPageView.setLayoutParams(layoutParams);
            Log.d("drawHotSpotDetailedPageView","4");
        }
    }

    // Helper method to convert dp to pixels
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
