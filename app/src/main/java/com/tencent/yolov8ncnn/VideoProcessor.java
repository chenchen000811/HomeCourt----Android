package com.tencent.yolov8ncnn;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VideoProcessor {

    // Function to extract frames from a video and convert them to Bitmaps
    public static List<Bitmap> extractFramesFromVideo(Context context, String videoUri) {

        List<Bitmap> frames = new ArrayList<>();


        MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        try {
                Log.d("videoUri",videoUri);
                retriever.setDataSource(context, Uri.parse(videoUri));

        } catch (Exception e) {
                Log.e("VideoProcessor", "Error setting data source: " + e.getMessage());
                e.printStackTrace();
                return frames;
        }


        // Get the total duration of the video
        String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        if (duration == null) {
                Log.e("VideoProcessor", "Failed to retrieve video duration");


                return frames; // Return empty list if duration retrieval fails
        }
        long durationMillis = Long.parseLong(duration);

        // Extract frames at regular intervals (e.g., every second)
        long intervalMillis = 1000; // 1 second
        for (long time = 0; time < durationMillis; time += intervalMillis) {
                // Extract frame at the current time
                Bitmap frame = retriever.getFrameAtTime(time * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                if (frame != null) {
                    // Convert the frame to RGBA_8888 format
                    Bitmap rgbaFrame = frame.copy(Bitmap.Config.ARGB_8888, false);
                    frames.add(rgbaFrame);
                    frame.recycle(); // Recycle the original frame bitmap to save memory
                }
        }

        try {
                // Your code that may throw an IOException
            retriever.release();
        } catch (IOException e) {
            Log.e("VideoProcessor", "Error retriever.release: " + e.getMessage());
            // Handle the IOException, such as logging an error message or taking corrective action
            e.printStackTrace();
        }


        return frames;
    }
}
