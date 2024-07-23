package com.tencent.yolov8ncnn;


import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.app.AlertDialog;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Tab1Fragment extends Fragment {

    private ListView listView;
    ResultDatabase resultDB;
    List<Result> resultList = new ArrayList<>();

    public static List <List <CustomDrawView.Circle>> updatedCircles = new ArrayList<>();

    public static int currentPos = 0 ;
    public static int currentId = 0 ;

    //currentId = idList.get(currentPos);

    List<Integer> idList = new ArrayList<>();
    ArrayList<HashMap<String, String>> dataList = new ArrayList<>();
    SimpleAdapter adapter;

    DrawHotSpotDetailedPageView drawHotSpotDetailedPageView;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab1, container, false);
        listView = view.findViewById(R.id.lvShow);


//        int [] makes,attempts;
//        float [] percents;
//
//        // Create the ArrayAdapter and set it to the ListView
////        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, data);
////        listView.setAdapter(adapter);
//
//
//        HashMap<String, String> data1 = new HashMap<>();
//        data1.put("date", "2024-05-01");
//        data1.put("makes", "10");
//        data1.put("attempts", "15");
//        data1.put("percents", "66.67%");
//        dataList.add(data1);
//
//        HashMap<String, String> data2 = new HashMap<>();
//        data2.put("date", "2024-05-02");
//        data2.put("makes", "8");
//        data2.put("attempts", "12");
//        data2.put("percents", "66.67%");
//        dataList.add(data2);

        adapter = new SimpleAdapter(requireContext(), dataList, R.layout.list_item,
                new String[]{"date", "makes", "attempts", "percents"},
                new int[]{R.id.dateTextView, R.id.madesTextView, R.id.attemptsTextView, R.id.percentsTextView});

        adapter.setViewBinder(new SimpleAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Object data, String textRepresentation) {
                if (view.getId() == R.id.percentsTextView) {
                    TextView percentsTextView = (TextView) view;
                    float floatValue = Float.parseFloat(textRepresentation);
                    int intValue = (int) (floatValue * 100);
                    String percentString = String.format("%d%%", intValue);
                    percentsTextView.setText(percentString);



                    return true;
                }
                return false;
            }
        });



        listView.setAdapter(adapter);

        // Optional: Set an OnItemClickListener for the ListView
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //String selectedItem = (String) parent.getItemAtPosition(position);
                //Toast.makeText(requireContext(), "Selected: " + selectedItem, Toast.LENGTH_SHORT).show();
                try {
                    currentPos = position;

                    currentId = idList.get(currentPos);

//                String selectedItem = data[position];
                    HashMap<String, String> selectedItem = dataList.get(position);

                    // Show the item details in a dialog
                    showItemDetailsDialog(selectedItem, idList.get(position));

                    Log.d("ListViewClick","ok");
                }catch (Exception e){
                    Log.e("ListViewClickError", "Error occurred while handling item click", e);
                    // Show a user-friendly error message
                    Toast.makeText(view.getContext(), "An error occurred. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }
        });


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


        resultDB = Room.databaseBuilder(requireContext(), ResultDatabase.class, "ResultDB")
                .addCallback(myCallBack).build();
        Log.d("resultDB","open db");

        getResultListInBackground();

        return view;
    }


    public void getResultListInBackground(){
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Handler handler = new Handler(Looper.getMainLooper());

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                //background
                Log.d("tab1 get result","before");

                // Fetch results in a try-catch block
                try {
                    //resultList = resultDB.getResultDAO().getAllResult();
                    List<Result> all_resultList = resultDB.getResultDAO().getAllResult();
                    //sort FULL COURT

                    for (Result result : all_resultList){
                        //FULL COURT
                        if (result.getMode() == 0){
                            resultList.add(result);
                        }

                    }


                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("Tab1", "Error fetching results: " + e.getMessage());
                }


                Log.d("tab1 get result","after");
                //on finishing task
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        StringBuilder sb = new StringBuilder();

                        idList.clear();

                        for(Result r: resultList){


                            sb.append("made:").append(r.getMade()).append("attempt:").append(r.getAttempt()).append("percent:").append(r.getPercent());
                            sb.append("\n");

                            HashMap<String, String> data1 = new HashMap<>();

                            // Get the current date
//                            Calendar calendar = Calendar.getInstance();
//                            int month = calendar.get(Calendar.MONTH) + 1; // Note: Month starts from 0
//                            int day = calendar.get(Calendar.DAY_OF_MONTH);
//                            String currentDate = String.format("%02d/%02d", month, day);


                            data1.put("date", r.getDate());
                            data1.put("makes", String.valueOf(r.getMade()));
                            data1.put("attempts", String.valueOf(r.getAttempt()));
                            data1.put("percents", String.valueOf(r.getPercent()));
                            data1.put("mode",String.valueOf(r.getMode()));
                            dataList.add(data1);
                            idList.add(r.getId());


//                            updatedCircles.add(transferDBtoCustom(r.getCircles()));

                        }
                        resultDB.close();

//                        Log.d("SIZEE", String.valueOf(updatedCircles.size()));

                        String finalData = sb.toString();
//                        Toast.makeText(requireContext(), finalData, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }


    public static List<CustomDrawView.Circle> transferDBtoCustom(List<Circle> circles){

        Log.d("transferDBtoCustom", "in");

        List<CustomDrawView.Circle> updatedCircles = new ArrayList<>();

        if(circles!= null && !circles.isEmpty()  ){

            Log.d("circles.size", String.valueOf(circles.size()));

            for (Circle circle: circles){
                CustomDrawView.Circle c = new CustomDrawView.Circle(circle.getX(),circle.getY(),circle.getRadius(),circle.getPaint());
                updatedCircles.add(c);
            }


        }

        return updatedCircles;
    }


    private void showItemDetailsDialog(HashMap<String, String> data,int position) {


        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.detailed, null);

        // Find the TextViews and ImageView in the custom layout

        TextView makeTextView = dialogView.findViewById(R.id.make_txt);
        TextView attemptTextView = dialogView.findViewById(R.id.attempt_txt);
        TextView percentageTextView = dialogView.findViewById(R.id.percentage_txt);


        String date = data.get("date"); // Retrieve the value of the "date" key
        String makes = data.get("makes"); // Retrieve the value of the "makes" key
        String attempts = data.get("attempts"); // Retrieve the value of the "attempts" key
        String percents = data.get("percents"); // Retrieve the value of the "percents" key
        String mode = data.get("mode"); // Retrieve the value of the "mode" key
        assert mode != null;
        Log.d("MODE",mode);

        assert percents != null;
        double decimalValue = Double.parseDouble(percents);
        int new_percents = (int) (decimalValue * 100);

        // Set the text and image for the dialog

        makeTextView.setText(makes);
        attemptTextView.setText(attempts);
        percentageTextView.setText(Integer.toString(new_percents));
//        imageView.setImageResource(R.drawable.your_image);

        builder.setView(dialogView)
                .setTitle(date)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        AlertDialog detailedDialog = builder.create();
        detailedDialog.show();

        deleteWorkout(dialogView,position,detailedDialog);



    }


    private void deleteWorkout(View view,int position,AlertDialog detailedDialog){


        ImageView deleteImgV = view.findViewById(R.id.deleteImgV);
        deleteImgV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                showDeleteDialog(position,detailedDialog);

            }
        });
    }



    void showDeleteDialog(int position,AlertDialog detailedDialog){

        Handler handler = new Handler(Looper.getMainLooper());
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());


        builder.setTitle("Delete")
                .setMessage("Are you sure want to delete ?")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Toast.makeText(requireContext(),"DELETE", Toast.LENGTH_LONG).show();
                        // Positive button clicked
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                // Call the deleteWorkout() method to delete the record
                                Result resultToDelete = resultDB.getResultDAO().getResult(position);

                                if (resultToDelete != null) {
                                    resultDB.getResultDAO().deletedResult (resultToDelete);

//                                    List <Result> r = resultDB.getResultDAO().getAllResult();
//                                    Log.d("r.size", String.valueOf(r.size()));

                                    dataList.remove(idList.indexOf(position));





                                    idList.clear();
                                    resultList = resultDB.getResultDAO().getAllResult();
                                    for(Result r: resultList){

                                        idList.add(r.getId());

                                    }
//                                    resultDB.close();






                                    Log.d("idList.size", String.valueOf(idList.size()));
                                    // Notify the adapter about the data change
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {

                                            adapter.notifyDataSetChanged();

                                           // resultDB.close();

                                        }
                                    });
                                }


                            }
                        }).start();

                        detailedDialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Negative button clicked
                        //detailedDialog.dismiss();
                    }
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


}