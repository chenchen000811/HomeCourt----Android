// Created by chenchen on 2024/5/5.

#include "opencv.h"
#include <opencv2/opencv.hpp>

//cv::Mat Opencv::getMatrix(cv::Mat src, cv::Point2f tl, cv::Point2f tr, cv::Point2f bl, cv::Point2f br) {
//    // Check if all points are defined
//    if (tl.x != 0 && tl.y != 0 && tr.x != 0 && tr.y != 0 && bl.x != 0 && bl.y != 0 && br.x != 0 && br.y != 0) {
//        // Define the points for perspective transformation
//        std::vector<cv::Point2f> srcPoints = {tl, bl, tr, br};
//        std::vector<cv::Point2f> destPoints = {
//                cv::Point2f(0, 0),
//                cv::Point2f(0, 480),
//                cv::Point2f(864, 0),
//                cv::Point2f(864, 480)
//        };
//
//        // Calculate the perspective transformation matrix
//        cv::Mat matrix = cv::getPerspectiveTransform(srcPoints, destPoints);
//
//        // Apply the perspective transformation to obtain bird's-eye view
//        cv::Mat transformedFrame;
//        cv::warpPerspective(src, transformedFrame, matrix, cv::Size(864, 480));
//
//        // Return the transformed matrix
//        return transformedFrame;
//    } else {
//        // Return an empty matrix if any of the points are undefined
//        return cv::Mat();
//    }
//}
//
//
//cv::Point2f Opencv::calculateUpdatedPosition(cv::Point2f originalPos, cv::Mat transformationMatrix) {
//    // Convert the original point to a matrix of size 1x1
//    cv::Mat originalPointMat = (cv::Mat_<float>(1, 1) << originalPos.x, originalPos.y);
//
//    // Apply the perspective transformation to the original point
//    cv::Mat updatedPointMat;
//    cv::perspectiveTransform(originalPointMat, updatedPointMat, transformationMatrix);
//
//    // Extract the updated point from the matrix
//    cv::Point2f updatedPoint = cv::Point2f(updatedPointMat.at<float>(0, 0), updatedPointMat.at<float>(0, 1));
//
//    return updatedPoint;
//}


std::tuple<cv::Point2f, float> Opencv::convertCircleCoordinates(int srcWidth, int srcHeight, Point2i center, float radius, Point2i tl, Point2i tr, Point2i bl, Point2i br) {
    // Check if all points are defined
    if (tl.x != -1 && tl.y != -1 && tr.x != -1 && tr.y != -1 && bl.x != -1 && bl.y != -1 && br.x != -1 && br.y != -1) {
        // Define the points for perspective transformation
        std::vector<cv::Point2f> srcPoints = {
                cv::Point2f(tl.x, tl.y),
                cv::Point2f(bl.x, bl.y),
                cv::Point2f(tr.x, tr.y),
                cv::Point2f(br.x, br.y)
        };
        std::vector<cv::Point2f> destPoints = {
                cv::Point2f(0, 0),                         // Top-left corner
                cv::Point2f(0, srcHeight),                 // Bottom-left corner
                cv::Point2f(srcWidth, 0),                  // Top-right corner
                cv::Point2f(srcWidth, srcHeight)           // Bottom-right corner
        };
       //custom
//        std::vector<cv::Point2f> destPoints = {
//
//                cv::Point2f(0, 0),
//                cv::Point2f(0, 480),
//                cv::Point2f(300, 0),
//                cv::Point2f(300,480)
//
////                3:5(三分線後+100m)
////                cv::Point2f(0, 0),
////                cv::Point2f(0, 300),
////                cv::Point2f(500, 0),
////                cv::Point2f(500,300)
//
//        };

        // Calculate the perspective transformation matrix
        cv::Mat matrix = cv::getPerspectiveTransform(srcPoints, destPoints);

        // Transform the center of the circle to the destination coordinate system
        std::vector<cv::Point2f> srcCenter = { cv::Point2f(center.x, center.y) };
        std::vector<cv::Point2f> destCenter;
        cv::perspectiveTransform(srcCenter, destCenter, matrix);

        // Calculate the radius in the destination coordinate system
//        float destRadius = radius * (destCenter[0].y - srcPoints[0].y) / srcHeight;
        float destRadius = 5;
        // Return the center and radius of the circle in the destination coordinate system
        return std::make_tuple(destCenter[0], destRadius);
    } else {
        // Return empty data if any of the points are undefined
        return std::make_tuple(cv::Point2f(-1, -1), -1);
    }
}




//std::tuple<int, int, std::vector<uchar>>
//Opencv::getMatrix(int srcWidth, int srcHeight, const uchar *srcData, Point2i tl, Point2i tr,
//                  Point2i bl, Point2i br) {
//    // Check if all points are defined
//    if (tl.x != 0 && tl.y != 0 && tr.x != 0 && tr.y != 0 && bl.x != 0 && bl.y != 0 && br.x != 0 && br.y != 0) {
//        // Define the points for perspective transformation
//        std::vector<cv::Point2f> srcPoints = {
//                cv::Point2f(tl.x, tl.y),
//                cv::Point2f(bl.x, bl.y),
//                cv::Point2f(tr.x, tr.y),
//                cv::Point2f(br.x, br.y)
//        };
//        std::vector<cv::Point2f> destPoints = {
//                cv::Point2f(0, 0),
//                cv::Point2f(0, 480),
//                cv::Point2f(864, 0),
//                cv::Point2f(864, 480)
//        };
//
//        // Calculate the perspective transformation matrix
//        cv::Mat matrix = cv::getPerspectiveTransform(srcPoints, destPoints);
//
//        // Apply the perspective transformation to obtain bird's-eye view
//        cv::Mat transformedFrame;
//        cv::warpPerspective(cv::Mat(srcHeight, srcWidth, CV_8UC1, const_cast<uchar*>(srcData)), transformedFrame, matrix, cv::Size(864, 480));
//
//        // Return the transformed image width, height, and data
//        std::vector<uchar> transformedData;
//        cv::imencode(".png", transformedFrame, transformedData);
//        return std::make_tuple(transformedFrame.cols, transformedFrame.rows, transformedData);
//    } else {
//        // Return empty data if any of the points are undefined
//        return std::make_tuple(0, 0, std::vector<uchar>());
//    }
//}
//
//
//std::pair<int, int> Opencv::calculateUpdatedPosition(int originalX, int originalY, const std::vector<float>& transformationMatrix) {
//    // Convert the original point to a homogeneous coordinate
//    cv::Mat originalPoint = (cv::Mat_<float>(3, 1) << originalX, originalY, 1);
//
//    // Convert the transformation matrix to a cv::Mat
//    cv::Mat transMat(3, 3, CV_32F);
//    memcpy(transMat.data, transformationMatrix.data(), transformationMatrix.size() * sizeof(float));
//
//    // Apply the perspective transformation to the original point
//    cv::Mat updatedPointMat = transMat * originalPoint;
//
//    // Extract the updated point coordinates
//    int updatedX = static_cast<int>(updatedPointMat.at<float>(0, 0) / updatedPointMat.at<float>(2, 0));
//    int updatedY = static_cast<int>(updatedPointMat.at<float>(1, 0) / updatedPointMat.at<float>(2, 0));
//
//    return std::make_pair(updatedX, updatedY);
//}
//
