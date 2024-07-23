//
// Created by chenchen on 2024/5/5.
//

#ifndef NCNN_ANDROID_YOLOV8_OPENCV_H
#define NCNN_ANDROID_YOLOV8_OPENCV_H

#include <opencv2/core/core.hpp>


struct Point2i {
    int x;
    int y;
    Point2i(int _x, int _y) : x(_x), y(_y) {}
};

class Opencv{
public:
//    cv::Mat getMatrix(cv::Mat src, cv::Point2f tl, cv::Point2f tr, cv::Point2f bl, cv::Point2f br);
//
//    cv::Point2f calculateUpdatedPosition(cv::Point2f originalPos, cv::Mat transformationMatrix);

//    std::tuple<int, int, std::vector<uchar>> getMatrix(int srcWidth, int srcHeight, const uchar* srcData, Point2i tl, Point2i tr, Point2i bl, Point2i br);
//
//    std::pair<int, int> calculateUpdatedPosition(int originalX, int originalY, const std::vector<float>& transformationMatrix);

    std::tuple<cv::Point2f, float>
    convertCircleCoordinates(int srcWidth, int srcHeight, Point2i center, float radius, Point2i tl,
                             Point2i tr, Point2i bl, Point2i br);
};

#endif //NCNN_ANDROID_YOLOV8_OPENCV_H
