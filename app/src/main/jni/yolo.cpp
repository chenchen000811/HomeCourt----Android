
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

#include "yolo.h"

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#include "cpu.h"

static float fast_exp(float x)
{
    union {
        uint32_t i;
        float f;
    } v{};
    v.i = (1 << 23) * (1.4426950409 * x + 126.93490512f);
    return v.f;
}

static float sigmoid(float x)
{
    return 1.0f / (1.0f + fast_exp(-x));
}
static float intersection_area(const Object& a, const Object& b)
{
    cv::Rect_<float> inter = a.rect & b.rect;
    return inter.area();
}

static void qsort_descent_inplace(std::vector<Object>& faceobjects, int left, int right)
{
    int i = left;
    int j = right;
    float p = faceobjects[(left + right) / 2].prob;

    while (i <= j)
    {
        while (faceobjects[i].prob > p)
            i++;

        while (faceobjects[j].prob < p)
            j--;

        if (i <= j)
        {
            // swap
            std::swap(faceobjects[i], faceobjects[j]);

            i++;
            j--;
        }
    }

    //     #pragma omp parallel sections
    {
        //         #pragma omp section
        {
            if (left < j) qsort_descent_inplace(faceobjects, left, j);
        }
        //         #pragma omp section
        {
            if (i < right) qsort_descent_inplace(faceobjects, i, right);
        }
    }
}

static void qsort_descent_inplace(std::vector<Object>& faceobjects)
{
    if (faceobjects.empty())
        return;

    qsort_descent_inplace(faceobjects, 0, faceobjects.size() - 1);
}

static void nms_sorted_bboxes(const std::vector<Object>& faceobjects, std::vector<int>& picked, float nms_threshold)
{
    picked.clear();

    const int n = faceobjects.size();

    std::vector<float> areas(n);
    for (int i = 0; i < n; i++)
    {
        areas[i] = faceobjects[i].rect.width * faceobjects[i].rect.height;
    }

    for (int i = 0; i < n; i++)
    {
        const Object& a = faceobjects[i];

        int keep = 1;
        for (int j = 0; j < (int)picked.size(); j++)
        {
            const Object& b = faceobjects[picked[j]];

            // intersection over union
            float inter_area = intersection_area(a, b);
            float union_area = areas[i] + areas[picked[j]] - inter_area;
            // float IoU = inter_area / union_area
            if (inter_area / union_area > nms_threshold)
                keep = 0;
        }

        if (keep)
            picked.push_back(i);
    }
}
static void generate_grids_and_stride(const int target_w, const int target_h, std::vector<int>& strides, std::vector<GridAndStride>& grid_strides)
{
    for (int i = 0; i < (int)strides.size(); i++)
    {
        int stride = strides[i];
        int num_grid_w = target_w / stride;
        int num_grid_h = target_h / stride;
        for (int g1 = 0; g1 < num_grid_h; g1++)
        {
            for (int g0 = 0; g0 < num_grid_w; g0++)
            {
                GridAndStride gs;
                gs.grid0 = g0;
                gs.grid1 = g1;
                gs.stride = stride;
                grid_strides.push_back(gs);
            }
        }
    }
}
static void generate_proposals(std::vector<GridAndStride> grid_strides, const ncnn::Mat& pred, float prob_threshold, std::vector<Object>& objects)
{
    const int num_points = grid_strides.size();
    // fix
//        const int num_class = 3;
    const int num_class = 5;
//    const int num_class = 80;
    const int reg_max_1 = 16;

    for (int i = 0; i < num_points; i++)
    {
        const float* scores = pred.row(i) + 4 * reg_max_1;

        // find label with max score
        int label = -1;
        float score = -FLT_MAX;
        for (int k = 0; k < num_class; k++)
        {
            float confidence = scores[k];
            if (confidence > score)
            {
                label = k;
                score = confidence;
            }
        }
        float box_prob = sigmoid(score);
        if (box_prob >= prob_threshold)
        {
            ncnn::Mat bbox_pred(reg_max_1, 4, (void*)pred.row(i));
            {
                ncnn::Layer* softmax = ncnn::create_layer("Softmax");

                ncnn::ParamDict pd;
                pd.set(0, 1); // axis
                pd.set(1, 1);
                softmax->load_param(pd);

                ncnn::Option opt;
                opt.num_threads = 1;
                opt.use_packing_layout = false;

                softmax->create_pipeline(opt);

                softmax->forward_inplace(bbox_pred, opt);

                softmax->destroy_pipeline(opt);

                delete softmax;
            }

            float pred_ltrb[4];
            for (int k = 0; k < 4; k++)
            {
                float dis = 0.f;
                const float* dis_after_sm = bbox_pred.row(k);
                for (int l = 0; l < reg_max_1; l++)
                {
                    dis += l * dis_after_sm[l];
                }

                pred_ltrb[k] = dis * grid_strides[i].stride;
            }

            float pb_cx = (grid_strides[i].grid0 + 0.5f) * grid_strides[i].stride;
            float pb_cy = (grid_strides[i].grid1 + 0.5f) * grid_strides[i].stride;

            float x0 = pb_cx - pred_ltrb[0];
            float y0 = pb_cy - pred_ltrb[1];
            float x1 = pb_cx + pred_ltrb[2];
            float y1 = pb_cy + pred_ltrb[3];

            Object obj;
            obj.rect.x = x0;
            obj.rect.y = y0;
            obj.rect.width = x1 - x0;
            obj.rect.height = y1 - y0;
            obj.label = label;
            obj.prob = box_prob;

            objects.push_back(obj);
        }
    }
}


// Function to find the orientation of three points (p, q, r)
int orientation(cv::Point p, cv::Point q, cv::Point r)
{
    int val = (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y);

    if (val == 0) return 0; // Colinear
    return (val > 0) ? 1 : 2; // Clockwise or counterclockwise
}

// Function to check if point q lies on line segment 'pr'
bool onSegment(cv::Point p, cv::Point q, cv::Point r)
{
    if (q.x <= std::max(p.x, r.x) && q.x >= std::min(p.x, r.x) &&
        q.y <= std::max(p.y, r.y) && q.y >= std::min(p.y, r.y))
        return true;

    return false;
}

// Function to check if two line segments intersect
bool doIntersect(cv::Point p1, cv::Point q1, cv::Point p2, cv::Point q2)
{
    // Find the four orientations needed for general and special cases
    int o1 = orientation(p1, q1, p2);
    int o2 = orientation(p1, q1, q2);
    int o3 = orientation(p2, q2, p1);
    int o4 = orientation(p2, q2, q1);

    // General case
    if (o1 != o2 && o3 != o4)
        return true;

    // Special cases (colinear segments)
    if (o1 == 0 && onSegment(p1, p2, q1)) return true;
    if (o2 == 0 && onSegment(p1, q2, q1)) return true;
    if (o3 == 0 && onSegment(p2, p1, q2)) return true;
    if (o4 == 0 && onSegment(p2, q1, q2)) return true;

    return false; // Doesn't fall in any of the above cases
}




Yolo::Yolo()
{
    blob_pool_allocator.set_size_compare_ratio(0.f);
    workspace_pool_allocator.set_size_compare_ratio(0.f);
}

int Yolo::load(AAssetManager* mgr, const char* modeltype, int _target_size, const float* _mean_vals, const float* _norm_vals, bool use_gpu)
{
    yolo.clear();
    blob_pool_allocator.clear();
    workspace_pool_allocator.clear();

    ncnn::set_cpu_powersave(2);
    ncnn::set_omp_num_threads(ncnn::get_big_cpu_count());

    yolo.opt = ncnn::Option();

#if NCNN_VULKAN
    yolo.opt.use_vulkan_compute = use_gpu;
#endif

    yolo.opt.num_threads = ncnn::get_big_cpu_count();
    yolo.opt.blob_allocator = &blob_pool_allocator;
    yolo.opt.workspace_allocator = &workspace_pool_allocator;

    char parampath[256];
    char modelpath[256];
    //fix
//    sprintf(parampath, "yolov8%s.param", modeltype);
//    sprintf(modelpath, "yolov8%s.bin", modeltype);
    sprintf(parampath, "%s.param", modeltype);
    sprintf(modelpath, "%s.bin", modeltype);

    yolo.load_param(mgr, parampath);
    yolo.load_model(mgr, modelpath);

    target_size = _target_size;
    mean_vals[0] = _mean_vals[0];
    mean_vals[1] = _mean_vals[1];
    mean_vals[2] = _mean_vals[2];
    norm_vals[0] = _norm_vals[0];
    norm_vals[1] = _norm_vals[1];
    norm_vals[2] = _norm_vals[2];

    return 0;
}

int Yolo::detect(const cv::Mat& rgb, std::vector<Object>& objects, float prob_threshold, float nms_threshold)
{
    int width = rgb.cols;
    int height = rgb.rows;

    // pad to multiple of 32
    int w = width;
    int h = height;
    float scale = 1.f;
    if (w > h)
    {
        scale = (float)target_size / w;
        w = target_size;
        h = h * scale;
    }
    else
    {
        scale = (float)target_size / h;
        h = target_size;
        w = w * scale;
    }

    ncnn::Mat in = ncnn::Mat::from_pixels_resize(rgb.data, ncnn::Mat::PIXEL_RGB2BGR, width, height, w, h);

    // pad to target_size rectangle
    int wpad = (w + 31) / 32 * 32 - w;
    int hpad = (h + 31) / 32 * 32 - h;
    ncnn::Mat in_pad;
    ncnn::copy_make_border(in, in_pad, hpad / 2, hpad - hpad / 2, wpad / 2, wpad - wpad / 2, ncnn::BORDER_CONSTANT, 0.f);

    in_pad.substract_mean_normalize(0, norm_vals);

    ncnn::Extractor ex = yolo.create_extractor();

    ex.input("images", in_pad);

    std::vector<Object> proposals;
    
    ncnn::Mat out;
    ex.extract("output0", out);

    std::vector<int> strides = {8, 16, 32}; // might have stride=64
    std::vector<GridAndStride> grid_strides;
    generate_grids_and_stride(in_pad.w, in_pad.h, strides, grid_strides);
    generate_proposals(grid_strides, out, prob_threshold, proposals);

    // sort all proposals by score from highest to lowest
    qsort_descent_inplace(proposals);

    // apply nms with nms_threshold
    std::vector<int> picked;
    nms_sorted_bboxes(proposals, picked, nms_threshold);

    int count = picked.size();

    objects.resize(count);
    for (int i = 0; i < count; i++)
    {
        objects[i] = proposals[picked[i]];

        // adjust offset to original unpadded
        float x0 = (objects[i].rect.x - (wpad / 2)) / scale;
        float y0 = (objects[i].rect.y - (hpad / 2)) / scale;
        float x1 = (objects[i].rect.x + objects[i].rect.width - (wpad / 2)) / scale;
        float y1 = (objects[i].rect.y + objects[i].rect.height - (hpad / 2)) / scale;

        // clip
        x0 = std::max(std::min(x0, (float)(width - 1)), 0.f);
        y0 = std::max(std::min(y0, (float)(height - 1)), 0.f);
        x1 = std::max(std::min(x1, (float)(width - 1)), 0.f);
        y1 = std::max(std::min(y1, (float)(height - 1)), 0.f);

        objects[i].rect.x = x0;
        objects[i].rect.y = y0;
        objects[i].rect.width = x1 - x0;
        objects[i].rect.height = y1 - y0;
    }

    // sort objects by area
    struct
    {
        bool operator()(const Object& a, const Object& b) const
        {
            return a.rect.area() > b.rect.area();
        }
    } objects_area_greater;
    std::sort(objects.begin(), objects.end(), objects_area_greater);

    return 0;
}


 int Yolo::detectPicture(JNIEnv *env ,jobject bitmap , int width ,int height,std::vector<Object>& objects,float prob_threshold,float nms_threshold)
{

    AndroidBitmapInfo info;
    AndroidBitmap_getInfo(env, bitmap, &info);
//    info.format = ANDROID_BITMAP_FORMAT_RGBA_8888;
    // Log the bitmap formatyolo::detectPicture
    __android_log_print(ANDROID_LOG_DEBUG, "yolo::detectPicture", "Bitmap format: %d", info.format);

    try{



        int w= width ;
        int h= height ;

        // pad to multiple of 32
        float scale = 1.f;
        if (w > h)
        {
            scale = (float)target_size / w;
            w = target_size;
            h = h * scale;
        }
        else
        {
            scale = (float)target_size / h;
            h = target_size;
            w = w * scale;
        }
        ncnn::Mat in = ncnn::Mat::from_android_bitmap_resize(env, bitmap, ncnn::Mat::PIXEL_RGB, w, h);
//        ncnn::Mat in = ncnn::Mat::from_android_bitmap_resize(env, bitmap, ncnn::Mat::PIXEL_RGBA, w, h);
        // pad to target_size rectangle
        int wpad = (w + 31) / 32 * 32 - w;
        int hpad = (h + 31) / 32 * 32 - h;
        ncnn::Mat in_pad;
        ncnn::copy_make_border(in, in_pad, hpad / 2, hpad - hpad / 2, wpad / 2, wpad - wpad / 2, ncnn::BORDER_CONSTANT, 0.f);
        in_pad.substract_mean_normalize(0, norm_vals);

        ncnn::Extractor ex = yolo.create_extractor();

        ex.input("images", in_pad);

        std::vector<Object> proposals;

        ncnn::Mat out;
        __android_log_print(ANDROID_LOG_DEBUG, "yolo::detectPicture", "above ok ");

// in VideoActivity cause app crush but in ImageActivity is ok
        int extract_correct = ex.extract("output0", out);
//        int extract_correct = ex.extract("output", out);
        __android_log_print(ANDROID_LOG_DEBUG, "yolo::detectPicture", "extract_correct:%d",extract_correct);

        __android_log_print(ANDROID_LOG_ERROR, "yolo::detectPicture", "output0 :OK");
        std::vector<int> strides = {8, 16, 32}; // might have stride=64
        std::vector<GridAndStride> grid_strides;
        generate_grids_and_stride(in_pad.w, in_pad.h, strides, grid_strides);
        generate_proposals(grid_strides, out, prob_threshold, proposals);

        // sort all proposals by score from highest to lowest
        qsort_descent_inplace(proposals);

        // apply nms with nms_threshold
        std::vector<int> picked;
        nms_sorted_bboxes(proposals, picked, nms_threshold);

        int count = picked.size();

        objects.resize(count);
        for (int i = 0; i < count; i++)
        {
            objects[i] = proposals[picked[i]];

            // adjust offset to original unpadded
            float x0 = (objects[i].rect.x - (wpad / 2)) / scale;
            float y0 = (objects[i].rect.y - (hpad / 2)) / scale;
            float x1 = (objects[i].rect.x + objects[i].rect.width - (wpad / 2)) / scale;
            float y1 = (objects[i].rect.y + objects[i].rect.height - (hpad / 2)) / scale;

            // clip
            x0 = std::max(std::min(x0, (float)(width - 1)), 0.f);
            y0 = std::max(std::min(y0, (float)(height - 1)), 0.f);
            x1 = std::max(std::min(x1, (float)(width - 1)), 0.f);
            y1 = std::max(std::min(y1, (float)(height - 1)), 0.f);

            objects[i].rect.x = x0;
            objects[i].rect.y = y0;
            objects[i].rect.width = x1 - x0;
            objects[i].rect.height = y1 - y0;
        }

        // sort objects by area
        struct
        {
            bool operator()(const Object& a, const Object& b) const
            {
                return a.rect.area() > b.rect.area();
            }
        } objects_area_greater;
        std::sort(objects.begin(), objects.end(), objects_area_greater);
        __android_log_print(ANDROID_LOG_DEBUG, "yolo::detectPicture", "return 0");
        return 0;

    }catch (const std::exception& e) {
        __android_log_print(ANDROID_LOG_ERROR, "yolo::detectPicture", "ERROR");
        return -1;
//            e.what() ;
    }


}



double calculateDistance(int x1, int y1, int x2, int y2) {
    return sqrt(pow(x2 - x1, 2) + pow(y2 - y1, 2));
}




static int score = 0;
static int attempt = 0;
static int miss = 0;
static bool flag_miss = true;
static bool isShooting = false ;
static cv::Point player_pos ;

int larger_box_width = 0;
int larger_box_height = 0;
int larger_box_x = 0;
int larger_box_y = 0;

int rim_x = 0;
int rim_y = 0;
int rim_width = 0;
int upper_rim_y = 0;


int player_x = 0;
int player_y = 0;

int baller_x = 0;
int baller_y = 0;


//int ball_x = 0;
//int ball_y = 0;

cv::Scalar cc_red(0, 0, 255);
cv::Scalar cc_blue(255, 0, 0);

// Define a vector to store the previous positions of the ball
std::vector<cv::Point> ball_trajectory,key_ball_trajectory;

int cv_dim[2] = {-1,-1} ;


//find the last point above the rim, and the first point under the rim
std::vector<cv::Point> find_key_point(std::vector<cv::Point> all_ball) {

    std::vector<cv::Point> over_rim_ball,under_rim_ball,key_ball,points(2, cv::Point(-1, -1));

    for (cv::Point ball : all_ball){

//        __android_log_print(ANDROID_LOG_DEBUG, "upper_rim_y", "%d",upper_rim_y);
            //over rim
             if (ball.y < upper_rim_y){

                over_rim_ball.push_back(ball);
                 __android_log_print(ANDROID_LOG_DEBUG, "ball_over", "(%d, %d)", ball.x, ball.y);
            }
            //under rim
            else if (ball.y > upper_rim_y){
                under_rim_ball.push_back(ball);
                 __android_log_print(ANDROID_LOG_DEBUG, "ball_under", "(%d, %d)", ball.x, ball.y);
            }
    }

    if (!over_rim_ball.empty() && !under_rim_ball.empty()) {
//    if (over_rim_ball.size()>0 && under_rim_ball.size()>0) {
//        key_ball.push_back(over_rim_ball[-1]);
//        key_ball.push_back(under_rim_ball[0]);
        key_ball.push_back(over_rim_ball.back()); // Use back() to get the last element
        key_ball.push_back(under_rim_ball.front()); // Use front() to get the first element

        return key_ball;
    }
    else {
        return points;
    }


}

int Yolo::draw(cv::Mat& rgb, const std::vector<Object>& objects)
{

    cv_dim[0] = rgb.rows; // Height of the image (number of rows)
    cv_dim[1] = rgb.cols; // Width of the image (number of columns)

    __android_log_print(ANDROID_LOG_DEBUG, "cv dimension", "(%d, %d)", cv_dim[0], cv_dim[1]);

    static const char* class_names[] = {
            "ball","made","person","rim","shoot"
    };

//    static const char* class_names[] = {
//            "ball","person","rim",
//    };

    static const unsigned char colors[19][3] = {
            { 54,  67, 244},
            { 99,  30, 233},
            {176,  39, 156},
            {183,  58, 103},
            {181,  81,  63},
            {243, 150,  33},
            {244, 169,   3},
            {212, 188,   0},
            {136, 150,   0},
            { 80, 175,  76},
            { 74, 195, 139},
            { 57, 220, 205},
            { 59, 235, 255},
            {  7, 193, 255},
            {  0, 152, 255},
            { 34,  87, 255},
            { 72,  85, 121},
            {158, 158, 158},
            {139, 125,  96}
    };

    int color_index = 0;
    bool detected_ball = false;
    setBallCloseRim(false);

    for (size_t i = 0; i < objects.size(); i++)
    {
        const Object& obj = objects[i];

        const unsigned char* color = colors[color_index % 19];
        color_index++;

//        if (obj.label == 2)
        if (obj.label == 3) // If the object is a rim
        {
            rim_x = obj.rect.x;
            rim_y = obj.rect.y;
            rim_width = obj.rect.width;
//            upper_rim_y = rim_y - obj.rect.height/2;
            upper_rim_y = rim_y;
            cv::line(rgb, cv::Point(rim_x, upper_rim_y), cv::Point(rim_x + rim_width, upper_rim_y), cc_red, 2);



            // Draw a bounding box around the rim
            cv::Scalar cc(color[0], color[1], color[2]);
            cv::rectangle(rgb, obj.rect, cc, 2);




            // Calculate the larger bounding box dimensions
            larger_box_width = obj.rect.width * 4;
            larger_box_height = obj.rect.height * 3;
            larger_box_x = obj.rect.x - (larger_box_width - obj.rect.width) / 2;
            larger_box_y = obj.rect.y - (larger_box_height - obj.rect.height) / 2;

            // Draw the larger bounding box
//            cv::rectangle(rgb, cv::Rect(larger_box_x, larger_box_y, larger_box_width, larger_box_height), cc, 2);



            // Draw text and label for the rim
//            char text[256];
//            sprintf(text, "%s %.1f%%", class_names[obj.label], obj.prob * 100);
//
//            int baseLine = 0;
//            cv::Size label_size = cv::getTextSize(text, cv::FONT_HERSHEY_SIMPLEX, 0.5, 1, &baseLine);
//
//            int x = obj.rect.x;
//            int y = obj.rect.y - label_size.height - baseLine;
//            if (y < 0)
//                y = 0;
//            if (x + label_size.width > rgb.cols)
//                x = rgb.cols - label_size.width;
//
//            cv::rectangle(rgb, cv::Rect(cv::Point(x, y), cv::Size(label_size.width, label_size.height + baseLine)), cc, -1);
//
//            cv::Scalar textcc = (color[0] + color[1] + color[2] >= 381) ? cv::Scalar(0, 0, 0) : cv::Scalar(255, 255, 255);
//            cv::putText(rgb, text, cv::Point(x, y + label_size.height), cv::FONT_HERSHEY_SIMPLEX, 0.5, textcc, 1);
        }

        else if (obj.label == 0) // If the object is a ball
        {
            detected_ball = true;
            ball_x = obj.rect.x + obj.rect.width / 2;
            ball_y = obj.rect.y + obj.rect.height / 2;
            setBallPos(ball_x,ball_y);
            __android_log_print(ANDROID_LOG_DEBUG, "yolo.cpp","ball_pos:(%d,%d)",ball_x,ball_y);
            // Draw a circle at the center of the ball
            cv::Point ball_center(ball_x, ball_y);
            int radius = std::min(obj.rect.width, obj.rect.height) / 2;
            cv::Scalar cc(color[0], color[1], color[2]);

            cv::circle(rgb, ball_center, radius, cc, -1);

            //Each time ball go in the larger box,only can score maximum one
            static bool flag = true;

            // Check if the ball is inside the larger box -> record and draw ball_trajectory
            if (ball_x >= larger_box_x && ball_x <= larger_box_x + larger_box_width &&
                   ball_y >= larger_box_y && ball_y <= larger_box_y + larger_box_height)
            {
                __android_log_print(ANDROID_LOG_DEBUG, "ball in larger_box", "in");
                setBallCloseRim(true);

                ball_trajectory.push_back(ball_center);


                // Draw ball trajectory
                if (ball_trajectory.size() >= 2 && ball_y > upper_rim_y){

                    key_ball_trajectory = find_key_point(ball_trajectory);

//                    for (int j = 0; j < ball_trajectory.size()-1 ; j++){
                    for (int j = 0; j < 1 ; j++){
                        cv::Point current_point = key_ball_trajectory[j];
                        cv::circle(rgb, current_point, radius, cc_blue, -1 );
                        __android_log_print(ANDROID_LOG_DEBUG, "key_ball_trajectory", "(%d,%d),(%d,%d)",key_ball_trajectory[0].x,key_ball_trajectory[0].y,key_ball_trajectory[1].x,key_ball_trajectory[1].y);
                        cv::line(rgb, key_ball_trajectory[j], key_ball_trajectory[j + 1], cc, 2);


                        //SCORE (check ball trajectory(line) and rim(line) whether cross )
                        bool do_Intersect = doIntersect(key_ball_trajectory[j], key_ball_trajectory[j + 1], cv::Point(rim_x, upper_rim_y), cv::Point(rim_x + rim_width, upper_rim_y));


                        //made
                        if (do_Intersect && flag)
                        {
                            // Intersection detected
                            __android_log_print(ANDROID_LOG_DEBUG, "this shot", "SCORE");
                            score++;

                            flag = false ;

                        }
                        //miss
                        if (!do_Intersect && flag_miss){
                            __android_log_print(ANDROID_LOG_DEBUG, "this shot", "MISS");
                            miss ++ ;
                            flag_miss = false;
                        }

                    }
                }

//                    {
//                        cv::line(rgb, ball_trajectory[i], ball_trajectory[i + 1], cc, 2);
//                    }


//                if (ball_y > upper_rim_y  && ball_trajectory.size() >= 2 && flag){
//                    __android_log_print(ANDROID_LOG_DEBUG, "draw key line", "in");
//                    cv::Point current_point = ball_trajectory.back();
//                    cv::Point previous_point = ball_trajectory[ball_trajectory.size() - 2];
//                    cv::line(rgb, previous_point, current_point, cc_red, 2);
//                    flag = false;
//                }

//                 If the ball wasn't inside the box in the previous frame, start tracking its trajectory
//                if (ball_trajectory.empty())
//                {
//                    __android_log_print(ANDROID_LOG_DEBUG, "ball_trajectory.empty()", "T");
//                    ball_trajectory.push_back(ball_center);
//                }
//                else
//                {
//                    __android_log_print(ANDROID_LOG_DEBUG, "ball_trajectory.empty()", "F");
//                    // Draw lines connecting consecutive positions of the ball
//                    for (size_t i = 0; i < ball_trajectory.size() - 1; i++)
//                    {
//                        cv::line(rgb, ball_trajectory[i], ball_trajectory[i + 1], cc, 2);
//                    }
//                    // Update the trajectory with the current position
//                    ball_trajectory.push_back(ball_center);
//                }
            }
            else
            {
//              If the ball leaves the box, clear the trajectory
                ball_trajectory.clear();
                key_ball_trajectory.clear();
                setBallCloseRim(false);
                flag = true;
                flag_miss = true;
            }

            // Draw text and label for the ball
//            char text[256];
//            sprintf(text, "%s %.1f%%", class_names[obj.label], obj.prob * 100);
//
//            int baseLine = 0;
//            cv::Size label_size = cv::getTextSize(text, cv::FONT_HERSHEY_SIMPLEX, 0.5, 1, &baseLine);
//
//            int x = obj.rect.x;
//            int y = obj.rect.y - label_size.height - baseLine;
//            if (y < 0)
//                y = 0;
//            if (x + label_size.width > rgb.cols)
//                x = rgb.cols - label_size.width;
//
//            cv::rectangle(rgb, cv::Rect(cv::Point(x, y), cv::Size(label_size.width, label_size.height + baseLine)), cc, -1);
//
//            cv::Scalar textcc = (color[0] + color[1] + color[2] >= 381) ? cv::Scalar(0, 0, 0) : cv::Scalar(255, 255, 255);
//            cv::putText(rgb, text, cv::Point(x, y + label_size.height), cv::FONT_HERSHEY_SIMPLEX, 0.5, textcc, 1);
        }

//        else if (obj.label == 2) // If the object is a person
//        {
//            cv::Scalar cc(color[0], color[1], color[2]);
//            cv::rectangle(rgb, obj.rect, cc, 2);
//
//            char text[256];
//            sprintf(text, "%s %.1f%%", class_names[obj.label], obj.prob * 100);
//
//            int baseLine = 0;
//            cv::Size label_size = cv::getTextSize(text, cv::FONT_HERSHEY_SIMPLEX, 0.5, 1, &baseLine);
//
//            int x = obj.rect.x;
//            int y = obj.rect.y - label_size.height - baseLine;
//            if (y < 0)
//                y = 0;
//            if (x + label_size.width > rgb.cols)
//                x = rgb.cols - label_size.width;
//            setPlayerPos (x, y);
//
//            __android_log_print(ANDROID_LOG_DEBUG, "setPlayerPos (x, y)", "(x: %d, y: %d)", x, y);
//
//            cv::rectangle(rgb, cv::Rect(cv::Point(x, y), cv::Size(label_size.width, label_size.height + baseLine)), cc, -1);
//
//            cv::Scalar textcc = (color[0] + color[1] + color[2] >= 381) ? cv::Scalar(0, 0, 0) : cv::Scalar(255, 255, 255);
//            cv::putText(rgb, text, cv::Point(x, y + label_size.height), cv::FONT_HERSHEY_SIMPLEX, 0.5, textcc, 1);
//        }

//        else if (obj.label == 1)
        else if (obj.label == 2) // If the object is a person
        {
            cv::Scalar cc(color[0], color[1], color[2]);
            cv::rectangle(rgb, obj.rect, cc, 2);

            char text[256];
            sprintf(text, "%s %.1f%%", class_names[obj.label], obj.prob * 100);

            int baseLine = 0;
            cv::Size label_size = cv::getTextSize(text, cv::FONT_HERSHEY_SIMPLEX, 0.5, 1, &baseLine);

            //player box left-top
            player_x = obj.rect.x ;
            player_y = obj.rect.y ;

            //check player hold the ball (distance ball to person < person box.width(height))
            if (calculateDistance(player_x,player_y,ball_x,ball_y) < obj.rect.width){

                // 嚴格用width,反之用height

                __android_log_print(ANDROID_LOG_DEBUG, "OB-shoot", "baller");
                baller = true ;
                isShooting = false;
            }


            if (baller){
                if (ball_y < player_y - obj.rect.height/2 ){
                    __android_log_print(ANDROID_LOG_DEBUG, "OB-shoot", "SHOOT");
                    baller = false ;
                    attempt++ ;
                    isShooting = true;

                    //box 的底邊中點 (player foot pos)

                    player_x = obj.rect.x + obj.rect.width / 2;
                    player_y = obj.rect.y + obj.rect.height;

                    setPlayerPos (player_x, player_y);

                    __android_log_print(ANDROID_LOG_DEBUG, "setPlayerPos (x, y)", "(x: %d, y: %d)", player_x, player_y);

                }
            }





//            cv::rectangle(rgb, cv::Rect(cv::Point(x - label_size.width / 2, y - label_size.height - baseLine),
//                                        cv::Size(label_size.width, label_size.height + baseLine)), cc, -1);

//            cv::Scalar textcc = (color[0] + color[1] + color[2] >= 381) ? cv::Scalar(0, 0, 0) : cv::Scalar(255, 255, 255);
//            cv::putText(rgb, text, cv::Point(x - label_size.width / 2, y), cv::FONT_HERSHEY_SIMPLEX, 0.5, textcc, 1);
        }



        // shoot, made don't care

    }

    if (!detected_ball)setBallPos(-1, -1);

    return 0;
}





Yolo g_yolo_1;

void Yolo::setBallPos(int x, int y) {
    g_yolo_1.ball_x = x;
    g_yolo_1.ball_y = y;

    __android_log_print(ANDROID_LOG_DEBUG, "Yolo::setBallPos", "in");
}


cv::Point Yolo::getBallPos() const{
    return cv::Point(g_yolo_1.ball_x, g_yolo_1.ball_y);
}

void Yolo::setBallCloseRim(bool b) {
    g_yolo_1.ball_close_rim = b;
}

bool Yolo::getBallCloseRim() {
    return g_yolo_1.ball_close_rim;
}

void Yolo:: setPlayerPos (int x, int y){
    g_yolo_1.player_x = x;
    g_yolo_1.player_y = y;
}

cv::Point Yolo::getPlayerPos() const {
    return cv::Point(g_yolo_1.player_x, g_yolo_1.player_y);
}



extern "C"
JNIEXPORT jint JNICALL
Java_com_tencent_yolov8ncnn_Yolov8Ncnn_getScore(JNIEnv *env, jclass clazz) {
    // TODO: implement getScore()
    return score;
}
extern "C"
JNIEXPORT jintArray JNICALL
Java_com_tencent_yolov8ncnn_Yolov8Ncnn_getCVdimension(JNIEnv *env, jclass clazz) {
    // TODO: implement getCVdimension()

    // Create a new jintArray with size 2
    jintArray result = env->NewIntArray(2);
    if (result == nullptr) {
        // Handle allocation failure
        return nullptr;
    }

    // Populate the array with values from cv_dim
    jint dim[2] = {cv_dim[0], cv_dim[1]};
    env->SetIntArrayRegion(result, 0, 2, dim);

    return result;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_tencent_yolov8ncnn_Yolov8Ncnn_getAttempt_1ob(JNIEnv *env, jclass clazz) {
    // TODO: implement getAttempt_ob()
    return  attempt;
}

static Yolo* g_yolo = 0;
extern "C"
JNIEXPORT jintArray JNICALL
Java_com_tencent_yolov8ncnn_Yolov8Ncnn_getPlayerPos_1ob(JNIEnv *env, jclass clazz) {
    // TODO: implement getPlayerPos_ob()

    player_pos = g_yolo->getPlayerPos();
    // Create a float array to hold the coordinates
    jintArray pointArray = env->NewIntArray(2);
    if (pointArray == nullptr) {
        // Handle allocation failure
        return nullptr;
    }

    // Convert cv::Point coordinates to float array
    int coordinates[2] = {player_pos.x, player_pos.y};

    // Fill the float array with the coordinates
    env->SetIntArrayRegion(pointArray, 0, 2, coordinates);

    return pointArray;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tencent_yolov8ncnn_Yolov8Ncnn_isShooting_1ob(JNIEnv *env, jclass clazz) {
    // TODO: implement isShooting_ob()
    return isShooting;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tencent_yolov8ncnn_Yolov8Ncnn_resetScore(JNIEnv *env, jobject thiz) {
    // TODO: implement resetScore()
    score = 0 ;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_tencent_yolov8ncnn_Yolov8Ncnn_resetAttempt_1ob(JNIEnv *env, jobject thiz) {
    // TODO: implement resetAttempt_ob()
    attempt = 0;
}