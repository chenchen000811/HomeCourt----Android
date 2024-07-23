package com.tencent.yolov8ncnn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.util.Log;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class CircleDaoTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private ResultDatabase database;
    private CircleDao circleDao;
    private ResultDAO resultDAO;
    @Before
    public void initDb() {
        database = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                ResultDatabase.class
        ).allowMainThreadQueries().build();

        circleDao = database.circleDao();
        resultDAO = database.getResultDAO();
    }

    @After
    public void closeDb() {
        database.close();
    }

    @Test
    public void testInsertAndRetrieveCircle() {
        Circle circle = new Circle(1, 1.0f, 50.0f, 10.0f, 0xFFFF0000);
        circleDao.addCircle(circle);

        List<Circle> circles = circleDao.getCirclesForResult(1);
        assertNotNull(circles);
        assertEquals(1, circles.size());
        assertEquals(1.0f, circles.get(0).getX(), 0.01);
        assertEquals(50.0f, circles.get(0).getY(), 0.01);
        assertEquals(10.0f, circles.get(0).getRadius(), 0.01);
        assertEquals(0xFFFF0000, circles.get(0).getColor());
    }


    @Test
    public void testUpdateCircle() {
        Circle circle = new Circle(1, 50.0f, 50.0f, 10.0f, 0xFFFF0000);
        circleDao.addCircle(circle);


        List<Circle> circles = circleDao.getCirclesForResult(1);
        Log.d("circleSize", String.valueOf(circles.size()));

        // Set the retrieved IDs to the circle objects
        circle.setId(circles.get(0).getId());
        circle.setX(60.0f);
        Log.d("circleX", String.valueOf(circle.getX()));
        circleDao.updateCircle(circle);

        circles = circleDao.getCirclesForResult(1);

        assertEquals(60.0f, circles.get(0).getX(), 0.01);
    }

    @Test
    public void testDeleteCircle() {
        Circle circle = new Circle(1, 50.0f, 50.0f, 10.0f, 0xFFFF0000);
        circleDao.addCircle(circle);



        List<Circle> circles = circleDao.getCirclesForResult(1);

        // Set the retrieved IDs to the circle objects
        circle.setId(circles.get(0).getId());

        circleDao.deleteCircle(circle);
        circles = circleDao.getCirclesForResult(1);

        assertEquals(0, circles.size());
    }

    @Test
    public void test() {
        Circle circle_1 = new Circle(1, 1.0f, 50.0f, 10.0f, 0xFFFF0000);

        circleDao.addCircle(circle_1);
        Circle circle_2 = new Circle(1, 2.0f, 50.0f, 10.0f, 0xFFFF0000);

        circleDao.addCircle(circle_2);
        List<Circle> circles = circleDao.getCirclesForResult(1);
        assertEquals(2, circles.size());


//         Set the retrieved IDs to the circle objects
        circle_1.setId(circles.get(0).getId());
        circle_2.setId(circles.get(1).getId());


        assertEquals(1, circles.get(0).getId());
        assertEquals(2, circles.get(1).getId());

        assertEquals(1, circles.get(0).getX(),0.01);
        assertEquals(2, circles.get(1).getX(),0.01);

        circle_1.setX(10.0f);
        Log.d("circleX", String.valueOf(circle_1.getX()));


        circle_2.setX(20.0f);
        Log.d("circleX", String.valueOf(circle_2.getX()));

        circleDao.updateCircle(circle_1);
        circleDao.updateCircle(circle_2);
        circles = circleDao.getCirclesForResult(1);
        assertEquals(10, circles.get(0).getX(),0.01);
        assertEquals(20, circles.get(1).getX(),0.01);

    }

    @Test
    public void testAddResult() {
        Result result = new Result(5, 10, 50.0f);
        resultDAO.addResult(result);

        List<Result> results = resultDAO.getAllResult();
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(5, results.get(0).getMade());
        assertEquals(10, results.get(0).getAttempt());
        assertEquals(50.0f, results.get(0).getPercent(), 0.01);
    }

    @Test
    public void testUpdateResult() {
        Result result = new Result(5, 10, 50.0f);
        resultDAO.addResult(result);

        List<Result> results = resultDAO.getAllResult();
        result = results.get(0);
        result.setMade(6);
        resultDAO.updatedResult(result);

        results = resultDAO.getAllResult();
        assertEquals(6, results.get(0).getMade());
    }

    @Test
    public void testDeleteResult() {
        List<Result> results ;


        // create r1
        Result r1 = new Result(5, 10, 50.0f);
        Circle circle_1 = new Circle(r1.getId(), 1.0f, 50.0f, 10.0f, 0xFFFF0000);
        Circle circle_2 = new Circle(r1.getId(), 1.0f, 50.0f, 10.0f, 0xFFFF0000);

        List<Circle> circles = new ArrayList<>();;
        circles.add(circle_1);
        circles.add(circle_2);

        r1.setCircles(circles);


        //add r1 to room
        resultDAO.addResult(r1);
        results = resultDAO.getAllResult();

        Result r1_db = results.get(results.size()-1);



        // create r2
        Result r2 = new Result(5, 10, 50.0f);
        r2.setCircles(circles);
        //add r2 to room
        resultDAO.addResult(r2);

        //delete r1

        resultDAO.deletedResult(r1_db);
//        resultDAO.deletedResult(r1);


        results = resultDAO.getAllResult();
        assertEquals(1, results.size());

        assertEquals(5, results.get(0).getMade());
        assertEquals(2, results.get(0).getCircles().size());
        assertEquals(1, results.get(0).getCircles().get(0).getX(),0.01);
    }

    @Test
    public void testGetResultById() {
        Result result = new Result(5, 10, 50.0f);
        resultDAO.addResult(result);

        List<Result> results = resultDAO.getAllResult();
        result = results.get(0);

        Result retrievedResult = resultDAO.getResult(result.getId());
        assertNotNull(retrievedResult);
        assertEquals(result.getId(), retrievedResult.getId());
    }


    //set result(no circles) to room,then add circles into room
    @Test
    public void test_2(){



        Result result = new Result(5, 10, 50.0f);


        resultDAO.addResult(result);


        List<Result> results = resultDAO.getAllResult();

        Result retrievedResult = results.get(results.size()-1);


        Circle circle_1 = new Circle(result.getId(), 1.0f, 50.0f, 10.0f, 0xFFFF0000);
        circleDao.addCircle(circle_1);

        Circle circle_2 = new Circle(result.getId(), 1.0f, 50.0f, 10.0f, 0xFFFF0000);
        circleDao.addCircle(circle_2);

        List<Circle> circles  = circleDao.getCirclesForResult(result.getId());
        Log.d("circle_size", String.valueOf(circles.size()));


        retrievedResult.setCircles(circles);


        assertEquals(2,retrievedResult.getCircles().size());

        assertEquals(1.0f,retrievedResult.getCircles().get(0).getX(),0.01);

    }


    //add circles to result,then add to room
    @Test
    public void test_3(){



        Result result = new Result(5, 10, 50.0f);

        Circle circle_1 = new Circle(result.getId(), 1.0f, 50.0f, 10.0f, 0xFFFF0000);
        Circle circle_2 = new Circle(result.getId(), 1.0f, 50.0f, 10.0f, 0xFFFF0000);

        List<Circle> circles = new ArrayList<>();;
        circles.add(circle_1);
        circles.add(circle_2);

        result.setCircles(circles);

        resultDAO.addResult(result);


        List<Result> results = resultDAO.getAllResult();

        Result retrievedResult = results.get(results.size()-1);


        assertEquals(2,retrievedResult.getCircles().size());

        assertEquals(1.0f,retrievedResult.getCircles().get(0).getX(),0.01);

    }
}