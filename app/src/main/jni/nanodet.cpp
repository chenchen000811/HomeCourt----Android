#include "nanodet.h"

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#include "cpu.h"
#include "yolo.h"


const int num_joints = 17;
int height = 0, width = 50, head_y =0;

//when player touch the ball again -> flag = true
static bool flag = true ;
static int attempt = 0;
static cv::Point player_pos ;
static bool isShooting = false ;

template<class ForwardIterator>
inline static size_t argmax(ForwardIterator first, ForwardIterator last) {
    return std::distance(first, std::max_element(first, last));
}

Yolo yolo;

NanoDet::NanoDet()
{
    blob_pool_allocator.set_size_compare_ratio(0.f);
    workspace_pool_allocator.set_size_compare_ratio(0.f);

}

int NanoDet::load(const char* modeltype, int _target_size, const float* _mean_vals, const float* _norm_vals, bool use_gpu)
{
    poseNet.clear();
    blob_pool_allocator.clear();
    workspace_pool_allocator.clear();

    ncnn::set_cpu_powersave(2);
    ncnn::set_omp_num_threads(ncnn::get_big_cpu_count());

    poseNet.opt = ncnn::Option();

#if NCNN_VULKAN
    poseNet.opt.use_vulkan_compute = use_gpu;
#endif

    poseNet.opt.num_threads = ncnn::get_big_cpu_count();
    poseNet.opt.blob_allocator = &blob_pool_allocator;
    poseNet.opt.workspace_allocator = &workspace_pool_allocator;

    char parampath[256];
    char modelpath[256];
    sprintf(parampath, "%s.param", modeltype);
    sprintf(modelpath, "%s.bin", modeltype);

    poseNet.load_param(parampath);
    poseNet.load_model(modelpath);

    target_size = _target_size;
    mean_vals[0] = _mean_vals[0];
    mean_vals[1] = _mean_vals[1];
    mean_vals[2] = _mean_vals[2];
    norm_vals[0] = _norm_vals[0];
    norm_vals[1] = _norm_vals[1];
    norm_vals[2] = _norm_vals[2];

    return 0;
}

int NanoDet::load(AAssetManager* mgr, const char* modeltype, int _target_size, const float* _mean_vals, const float* _norm_vals, bool use_gpu)
{
    poseNet.clear();
    blob_pool_allocator.clear();
    workspace_pool_allocator.clear();

    ncnn::set_cpu_powersave(2);
    ncnn::set_omp_num_threads(ncnn::get_big_cpu_count());

    poseNet.opt = ncnn::Option();

#if NCNN_VULKAN
    poseNet.opt.use_vulkan_compute = use_gpu;
#endif

    poseNet.opt.num_threads = ncnn::get_big_cpu_count();
    poseNet.opt.blob_allocator = &blob_pool_allocator;
    poseNet.opt.workspace_allocator = &workspace_pool_allocator;
    char parampath[256];
    char modelpath[256];
    sprintf(parampath, "%s.param", modeltype);
    sprintf(modelpath, "%s.bin", modeltype);

    poseNet.load_param(mgr,parampath);
    poseNet.load_model(mgr,modelpath);

    target_size = _target_size;
    mean_vals[0] = _mean_vals[0];
    mean_vals[1] = _mean_vals[1];
    mean_vals[2] = _mean_vals[2];
    norm_vals[0] = _norm_vals[0];
    norm_vals[1] = _norm_vals[1];
    norm_vals[2] = _norm_vals[2];

    if(target_size == 192)
    {
        feature_size = 48;
        kpt_scale = 0.02083333395421505;
    }
    else
    {
        feature_size = 64;
        kpt_scale = 0.015625;
    }
    for (int i = 0; i < feature_size; i++)
    {
        std::vector<float> x, y;
        for (int j = 0; j < feature_size; j++)
        {
            x.push_back(j);
            y.push_back(i);
        }
        dist_y.push_back(y);
        dist_x.push_back(x);
    }
    return 0;
}

int NanoDet::detect(const cv::Mat& rgb)
{
    //TODO:add person detection
    return 0;
}

void NanoDet::detect_pose(cv::Mat &rgb, std::vector<keypoint> &points)
{
    int w = rgb.cols;
    int h = rgb.rows;
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

    ncnn::Mat in = ncnn::Mat::from_pixels_resize(rgb.data, ncnn::Mat::PIXEL_RGB, rgb.cols, rgb.rows, w, h);
    int wpad = target_size - w;
    int hpad = target_size - h;
    ncnn::Mat in_pad;
    ncnn::copy_make_border(in, in_pad, hpad / 2, hpad - hpad / 2, wpad / 2, wpad - wpad / 2, ncnn::BORDER_CONSTANT, 0.f);

    ncnn::Extractor ex = poseNet.create_extractor();
    in_pad.substract_mean_normalize(mean_vals, norm_vals);

    ex.input("input", in_pad);

    ncnn::Mat regress, center, heatmap, offset;

    ex.extract("regress", regress);
    ex.extract("offset", offset);
    ex.extract("heatmap", heatmap);
    ex.extract("center", center);

    float* center_data = (float*)center.data;
    float* heatmap_data = (float*)heatmap.data;
    float* offset_data = (float*)offset.data;

    int top_index = 0;
    float top_score = 0;

    top_index = int(argmax(center_data, center_data+center.h));
    top_score = *std::max_element(center_data, center_data + center.h);

    int ct_y = (top_index / feature_size);
    int ct_x = top_index - ct_y * feature_size;

    std::vector<float> y_regress(num_joints), x_regress(num_joints);
    float* regress_data = (float*)regress.channel(ct_y).row(ct_x);
    for (size_t i = 0; i < num_joints; i++)
    {
        y_regress[i] = regress_data[i] + (float)ct_y;
        x_regress[i] = regress_data[i + num_joints] + (float)ct_x;
    }

    ncnn::Mat kpt_scores = ncnn::Mat(feature_size * feature_size, num_joints, sizeof(float));
    float* scores_data = (float*)kpt_scores.data;
    for (int i = 0; i < feature_size; i++)
    {
        for (int j = 0; j < feature_size; j++)
        {
            std::vector<float> score;
            for (int c = 0; c < num_joints; c++)
            {
                float y = (dist_y[i][j] - y_regress[c]) * (dist_y[i][j] - y_regress[c]);
                float x = (dist_x[i][j] - x_regress[c]) * (dist_x[i][j] - x_regress[c]);
                float dist_weight = std::sqrt(y + x) + 1.8;
                scores_data[c* feature_size * feature_size +i* feature_size +j] = heatmap_data[i * feature_size * num_joints + j * num_joints + c] / dist_weight;
            }
        }
    }
    std::vector<int> kpts_ys, kpts_xs;
    for (int i = 0; i < num_joints; i++)
    {
        top_index = 0;
        top_score = 0;
        top_index = int(argmax(scores_data + feature_size * feature_size *i, scores_data + feature_size * feature_size *(i+1)));
        top_score = *std::max_element(scores_data + feature_size * feature_size * i, scores_data + feature_size * feature_size * (i + 1));

        int top_y = (top_index / feature_size);
        int top_x = top_index - top_y * feature_size;
        kpts_ys.push_back(top_y);
        kpts_xs.push_back(top_x);
    }

    points.clear();
    for (int i = 0; i < num_joints; i++)
    {
        float kpt_offset_x = offset_data[kpts_ys[i] * feature_size * num_joints*2 + kpts_xs[i] * num_joints * 2 + i * 2];
        float kpt_offset_y = offset_data[kpts_ys[i] * feature_size * num_joints * 2 + kpts_xs[i] * num_joints * 2 + i * 2+1];

        float x = (kpts_xs[i] + kpt_offset_y) * kpt_scale * target_size;
        float y = (kpts_ys[i] + kpt_offset_x) * kpt_scale * target_size;

        keypoint kpt;
        kpt.x = (x - (wpad / 2)) / scale;
        kpt.y = (y - (hpad / 2)) / scale;
        kpt.score = heatmap_data[kpts_ys[i] * feature_size * num_joints + kpts_xs[i] * num_joints + i];
        points.push_back(kpt);

    }

}

//draw circle on hand
void process_hand_coordinates(const std::vector<keypoint>& points, cv::Mat& image)
{
    // Check if the points vector contains enough keypoints
    if (points.size() >= 11 ) { // At least 11 keypoints are needed for left and right wrists
        if(points[9].score > 0.3){
            // Extract left wrist coordinates
            const keypoint& left_wrist = points[9]; // Index 9 corresponds to the left wrist
            // Draw a circle on top of the left wrist
            cv::Point left_wrist_point(left_wrist.x, left_wrist.y);
            cv::circle(image, left_wrist_point, 10, cv::Scalar(0, 255, 0), -1); // Draw a filled green circle

        }
        if(points[10].score > 0.3){
            // Extract right wrist coordinates
            const keypoint& right_wrist = points[10]; // Index 10 corresponds to the right wrist
            // Draw a circle on top of the right wrist
            cv::Point right_wrist_point(right_wrist.x, right_wrist.y);
            cv::circle(image, right_wrist_point, 10, cv::Scalar(0, 255, 0), -1); // Draw a filled green circle
        }

    } else {
        // Handle the case where there are not enough keypoints detected
        __android_log_print(ANDROID_LOG_DEBUG, "Hand Detection", "Not enough keypoints detected for wrist extraction");
    }
}

int shoulder_to_wrist (const std::vector<keypoint>& points){
    // Check if the points vector contains enough keypoints
    if (points.size() >= 10 ) { // At least 11 keypoints are needed for left and right wrists
        if(points[5].score > 0.3 && points[9].score > 0.3){
            // Extract left wrist coordinates
            const keypoint& left_shoulder = points[5];
            cv::Point left_shoulder_point(left_shoulder.x, left_shoulder.y);

            const keypoint& left_wrist = points[9];
            cv::Point left_wrist_point(left_wrist.x, left_wrist.y);

            int distance = cv::norm(left_shoulder_point - left_wrist_point);
            return distance;

        }
        return -1;

    } else {
        // Handle the case where there are not enough keypoints detected
        __android_log_print(ANDROID_LOG_DEBUG, "Hand Detection", "Not enough keypoints detected for wrist extraction");
        return -1;
    }
}

cv::Point get_hand_pos(const std::vector<keypoint>& points){
    if (points.size() >= 11 ) { // At least 11 keypoints are needed for left and right wrists
        if (points[9].score > 0.3) {
            // Extract left wrist coordinates
            const keypoint &left_wrist = points[9]; // Index 9 corresponds to the left wrist
            // Draw a circle on top of the left wrist
            cv::Point left_wrist_point(left_wrist.x, left_wrist.y);
            return left_wrist_point;
        }
        if (points[10].score > 0.3) {
            // Extract right wrist coordinates
            const keypoint &right_wrist = points[10]; // Index 10 corresponds to the right wrist
            // Draw a circle on top of the right wrist
            cv::Point right_wrist_point(right_wrist.x, right_wrist.y);
            return right_wrist_point;
        }
        return cv::Point(-1,-1);
    }
    else
    {
        // Handle the case where there are not enough keypoints detected
        __android_log_print(ANDROID_LOG_DEBUG, "hand Detection", "Not enough keypoints detected for hand extraction");
        return  cv::Point(-1,-1);
    }
}

//draw a line above head
void draw_line_above_head(const std::vector<keypoint>& points, cv::Mat& image)
{
    // Check if the points vector contains enough keypoints
    if (points.size() >= 6 ) { // At least 11 keypoints are needed for left shoulder

        if (points[5].score > 0.3 && points[0].score > 0.3) {
            // Extract left shoulder coordinates
            const keypoint &nose = points[0];
            const keypoint &left_shoulder = points[5];
            // Draw a circle on top of the left wrist

            height = left_shoulder.y - nose.y;
            head_y = nose.y - height;
            cv::line(image, cv::Point(nose.x - width, head_y), cv::Point(nose.x + width, head_y),
                     cv::Scalar(0, 255, 0), 2);


        }
    }
    else
    {
        // Handle the case where there are not enough keypoints detected
        __android_log_print(ANDROID_LOG_DEBUG, "Shoulder Detection", "Not enough keypoints detected for shoulder extraction");
    }
}


// hand higher than head
bool is_shooting(const std::vector<keypoint>& points){

    if (points.size() >= 10 ){
        //left wrist
        if(points[9].score > 0.3){
            const keypoint& left_wrist = points[9];
            if (left_wrist.y < head_y){

                __android_log_print(ANDROID_LOG_DEBUG, "is_shooting()","wrist higher than head");
                return true;
            }
        }
        //right wrist
        else if (points[10].score > 0.3){
            const keypoint& right_wrist = points[10];
//            if (right_wrist.y < head_y) return true;
            if (right_wrist.y < head_y) {

                __android_log_print(ANDROID_LOG_DEBUG, "is_shooting()","wrist higher than head");
                return true;
            }
        }
        else{
            return false;
        }
    }
    return false;



}

int NanoDet::draw(cv::Mat& rgb)
{
    std::vector<keypoint> points;
    detect_pose(rgb,points);

    int skele_index[][2] = { {0,1},{0,2},{1,3},{2,4},{0,5},{0,6},{5,6},{5,7},{7,9},{6,8},{8,10},{11,12},
                             {5,11},{11,13},{13,15},{6,12},{12,14},{14,16} };
    int color_index[][3] = { {255, 0, 0},
                             {0, 0, 255},
                             {255, 0, 0},
                             {0, 0, 255},
                             {255, 0, 0},
                             {0, 0, 255},
                             {0, 255, 0},
                             {255, 0, 0},
                             {255, 0, 0},
                             {0, 0, 255},
                             {0, 0, 255},
                             {0, 255, 0},
                             {255, 0, 0},
                             {255, 0, 0},
                             {255, 0, 0},
                             {0, 0, 255},
                             {0, 0, 255},
                             {0, 0, 255}, };

    for (int i = 0; i < 18; i++)
    {
        if(points[skele_index[i][0]].score > 0.3 && points[skele_index[i][1]].score > 0.3)
            cv::line(rgb, cv::Point(points[skele_index[i][0]].x,points[skele_index[i][0]].y),
                     cv::Point(points[skele_index[i][1]].x,points[skele_index[i][1]].y), cv::Scalar(color_index[i][0], color_index[i][1], color_index[i][2]), 2);
    }
    for (int i = 0; i < num_joints; i++)
    {
        if (points[i].score > 0.3)
            cv::circle(rgb, cv::Point(points[i].x,points[i].y), 3, cv::Scalar(100, 255, 150), -1);
    }
    process_hand_coordinates(points,rgb);

    draw_line_above_head(points,rgb);

    // Shooting ( ball come into larger box -> player touch the ball -> wrist higher than head)
     if( flag && is_shooting(points) ){
        __android_log_print(ANDROID_LOG_DEBUG, "Shoot","SHOOTING!!!");
        attempt ++;
        flag = false;
        isShooting = true;

    }
     else isShooting = false;



    static bool closeRim = false ;
    if (g_yolo_1.getBallCloseRim()){

        closeRim = true;
    }
    __android_log_print(ANDROID_LOG_DEBUG, "closeRim","%d",closeRim);

    //when player touch the ball again -> flag = true
    //get ball and shooter pos from yolo.cpp
    cv::Point ball_pos = g_yolo_1.getBallPos();
    cv::Point hand_pos = get_hand_pos(points);

//    __android_log_print(ANDROID_LOG_DEBUG, "Shoot","ball pos:(%d,%d)",ball_pos.x,ball_pos.y);

    int arm_length = shoulder_to_wrist(points);
//    __android_log_print(ANDROID_LOG_DEBUG, "Shoot","arm:%d",arm_length);

//    __android_log_print(ANDROID_LOG_DEBUG, "Shoot","ball_pos:(%d,%d)",ball_pos.x,ball_pos.y);
//    __android_log_print(ANDROID_LOG_DEBUG, "Shoot","hand_pos:(%d,%d)",hand_pos.x,hand_pos.y);



    if (ball_pos.x != -1 && ball_pos.y != -1 && hand_pos.x !=-1 && hand_pos.y !=-1){
        int ball_baller_dis = cv::norm(ball_pos - hand_pos);
//        __android_log_print(ANDROID_LOG_DEBUG, "Shoot","ball to hand distance:%d",ball_baller_dis);

        //player touch the ball again and ball go into rim's larger box

        if (ball_baller_dis < arm_length  && closeRim ){
            flag = true;
            closeRim = false ;
            __android_log_print(ANDROID_LOG_DEBUG, "closeRim","set closeRim to false");

        }


    }



    return 0;
}


extern "C"
JNIEXPORT jint JNICALL
Java_com_tencent_yolov8ncnn_Yolov8Ncnn_getAttempt(JNIEnv *env, jclass clazz) {
    return attempt;
}


static Yolo* g_yolo = 0;
extern "C"
JNIEXPORT jintArray JNICALL
Java_com_tencent_yolov8ncnn_Yolov8Ncnn_getPlayerPos(JNIEnv *env, jclass clazz) {

//    cv::Point player_pos = g_yolo->getPlayerPos();
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
Java_com_tencent_yolov8ncnn_Yolov8Ncnn_isShooting(JNIEnv *env, jclass thiz) {
    // TODO: implement isShooting()
    return isShooting;
}