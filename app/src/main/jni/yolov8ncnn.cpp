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

#include <android/asset_manager_jni.h>
#include <android/native_window_jni.h>
#include <android/native_window.h>

#include <android/log.h>
#include <android/bitmap.h>

#include <jni.h>

#include <string>
#include <vector>

#include <platform.h>
#include <benchmark.h>

#include "yolo.h"

#include "ndkcamera.h"
#include "nanodet.h"

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>



#if __ARM_NEON
#include <arm_neon.h>
#endif // __ARM_NEON

static int draw_unsupported(cv::Mat& rgb)
{
    const char text[] = "unsupported";

    int baseLine = 0;
    cv::Size label_size = cv::getTextSize(text, cv::FONT_HERSHEY_SIMPLEX, 1.0, 1, &baseLine);

    int y = (rgb.rows - label_size.height) / 2;
    int x = (rgb.cols - label_size.width) / 2;

    cv::rectangle(rgb, cv::Rect(cv::Point(x, y), cv::Size(label_size.width, label_size.height + baseLine)),
                    cv::Scalar(255, 255, 255), -1);

    cv::putText(rgb, text, cv::Point(x, y + label_size.height),
                cv::FONT_HERSHEY_SIMPLEX, 1.0, cv::Scalar(0, 0, 0));

    return 0;
}

static int draw_fps(cv::Mat& rgb)
{
    // resolve moving average
    float avg_fps = 0.f;
    {
        static double t0 = 0.f;
        static float fps_history[10] = {0.f};

        double t1 = ncnn::get_current_time();
        if (t0 == 0.f)
        {
            t0 = t1;
            return 0;
        }

        float fps = 1000.f / (t1 - t0);
        t0 = t1;

        for (int i = 9; i >= 1; i--)
        {
            fps_history[i] = fps_history[i - 1];
        }
        fps_history[0] = fps;

        if (fps_history[9] == 0.f)
        {
            return 0;
        }

        for (int i = 0; i < 10; i++)
        {
            avg_fps += fps_history[i];
        }
        avg_fps /= 10.f;
    }

    char text[32];
    sprintf(text, "FPS=%.2f", avg_fps);

    int baseLine = 0;
    cv::Size label_size = cv::getTextSize(text, cv::FONT_HERSHEY_SIMPLEX, 0.5, 1, &baseLine);



    int y = 0;
    int x = rgb.cols - label_size.width;

// Create a rotation matrix
//    cv::Point2f center((x + label_size.width) / 2, y + label_size.height);
//    double angle = -90.0; // Rotate by -90 degrees
//    double scale = 1.0;
//    cv::Mat rotationMatrix = cv::getRotationMatrix2D(center, angle, scale);
//
//// Rotate the text
//    cv::Mat rotatedText;
//    cv::warpAffine(rgb, rotatedText, rotationMatrix, rgb.size());
//
//    // Draw the rectangle and text
//    cv::rectangle(rotatedText, cv::Rect(cv::Point(x, y), cv::Size(label_size.height, label_size.width + baseLine)), cv::Scalar(255, 255, 255), -1);
//    cv::putText(rotatedText, text, cv::Point(x, y + label_size.height),
//                cv::FONT_HERSHEY_SIMPLEX, 0.5, cv::Scalar(0, 0, 0));


    cv::rectangle(rgb, cv::Rect(cv::Point(x, y), cv::Size(label_size.width, label_size.height + baseLine)),
                    cv::Scalar(255, 255, 255), -1);

    cv::putText(rgb, text, cv::Point(x, y + label_size.height),
                cv::FONT_HERSHEY_SIMPLEX, 0.5, cv::Scalar(0, 0, 0));




    return 0;
}

static Yolo* g_yolo = 0;
static ncnn::Mutex lock;


static NanoDet* g_pose = 0;
static ncnn::Mutex lock2;

class MyNdkCamera : public NdkCameraWindow
{
public:
    virtual void on_image_render(cv::Mat& rgb) const;
};


//real-time detect result store inside (objects)#############################

void MyNdkCamera::on_image_render(cv::Mat& rgb) const
{

    //object detect
    {
        ncnn::MutexLockGuard g(lock);

        if (g_yolo)
        {
            std::vector<Object> objects;
            g_yolo->detect(rgb, objects);

            for (const auto& obj : objects)
            {
                // Access the properties of each object
                cv::Rect_<float> rect = obj.rect;
                int label = obj.label;
                float prob = obj.prob;

                __android_log_print(ANDROID_LOG_DEBUG, "Detect result", "label: %d,prob: %f",label,prob);
            }


            g_yolo->draw(rgb, objects);
        }
        else
        {
            draw_unsupported(rgb);
        }
    }


    // Pose detection
//    {
//        ncnn::MutexLockGuard g(lock2);
//
//        if (g_pose) {
//
//            // Draw detected poses on the RGB image
//            g_pose->draw(rgb);
//        } else {
//            draw_unsupported(rgb);
//        }
//    }

    draw_fps(rgb);
}


static MyNdkCamera* g_camera = 0;

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "JNI_OnLoad");

    g_camera = new MyNdkCamera;

    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM* vm, void* reserved)
{
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "JNI_OnUnload");

    {
        ncnn::MutexLockGuard g(lock);

        delete g_yolo;
        g_yolo = 0;
    }
//    {
//        ncnn::MutexLockGuard g(lock2);
//        delete g_pose;
//        g_pose = 0;
//    }

    delete g_camera;
    g_camera = 0;
}

// public native boolean loadModel(AssetManager mgr, int modelid, int cpugpu);
JNIEXPORT jboolean JNICALL Java_com_tencent_yolov8ncnn_Yolov8Ncnn_loadModel(JNIEnv* env, jobject thiz, jobject assetManager, jint modelid, jint cpugpu)
{
    if (modelid < 0 || modelid > 6 || cpugpu < 0 || cpugpu > 1)
    {
        return JNI_FALSE;
    }

    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "loadModel %p", mgr);

    const char* modeltypes[] =
    {
            //default
//            "yolov8n"

//v1
            "model_ncnn"
//        "n",
//        "s",


//new
//        "new_model",
    };

    const int target_sizes[] =
    {
        640,
//        320,
    };

    const float mean_vals[][3] =
    {
        {103.53f, 116.28f, 123.675f},
//        {103.53f, 116.28f, 123.675f},
    };

    const float norm_vals[][3] =
    {
        { 1 / 255.f, 1 / 255.f, 1 / 255.f },
//        { 1 / 255.f, 1 / 255.f, 1 / 255.f },
    };

    const char* modeltype = modeltypes[(int)modelid];
    int target_size = target_sizes[(int)modelid];
    bool use_gpu = (int)cpugpu == 1;

    // reload
    {
        ncnn::MutexLockGuard g(lock);

        if (use_gpu && ncnn::get_gpu_count() == 0)
        {
            // no gpu
            delete g_yolo;
            g_yolo = 0;
        }
        else
        {
            if (!g_yolo)
                g_yolo = new Yolo;
            g_yolo->load(mgr, modeltype, target_size, mean_vals[(int)modelid], norm_vals[(int)modelid], use_gpu);
        }
    }
    __android_log_print(ANDROID_LOG_DEBUG, "g_yolo", "g_yolo :%d", g_yolo);

    return JNI_TRUE;
}

// public native boolean openCamera(int facing);
JNIEXPORT jboolean JNICALL Java_com_tencent_yolov8ncnn_Yolov8Ncnn_openCamera(JNIEnv* env, jobject thiz, jint facing)
{
    if (facing < 0 || facing > 1)
        return JNI_FALSE;

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "openCamera %d", facing);

    g_camera->open((int)facing);

    return JNI_TRUE;
}

// public native boolean closeCamera();
JNIEXPORT jboolean JNICALL Java_com_tencent_yolov8ncnn_Yolov8Ncnn_closeCamera(JNIEnv* env, jobject thiz)
{
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "closeCamera");

    g_camera->close();

    return JNI_TRUE;
}

// public native boolean setOutputWindow(Surface surface);
JNIEXPORT jboolean JNICALL Java_com_tencent_yolov8ncnn_Yolov8Ncnn_setOutputWindow(JNIEnv* env, jobject thiz, jobject surface)
{
    ANativeWindow* win = ANativeWindow_fromSurface(env, surface);

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "setOutputWindow %p", win);

    g_camera->set_window(win);

    return JNI_TRUE;
}

}


//static Yolo* g_yolo_2 = 0;
static jclass objCls = NULL;
static jmethodID constructortorId;
static jfieldID xId;
static jfieldID yId;
static jfieldID wId;
static jfieldID hId;
static jfieldID labelId;
static jfieldID probId;
//先定义返回的数据

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_tencent_yolov8ncnn_YoloAPI_Init(JNIEnv *env, jobject thiz, jobject mgr, jint cpugpu) {
    // TODO: implement Init()
    jclass localObjCls = env->FindClass("com/tencent/yolov8ncnn/YoloAPI$Obj");

    objCls = reinterpret_cast<jclass>(env->NewGlobalRef(localObjCls));

    constructortorId = env->GetMethodID(objCls,"<init>", "()V");
    xId = env->GetFieldID(objCls, "x", "F");
    yId = env->GetFieldID(objCls, "y", "F");
    wId = env->GetFieldID(objCls, "w", "F");
    hId = env->GetFieldID(objCls, "h", "F");
    labelId = env->GetFieldID(objCls, "label", "I");
    probId = env->GetFieldID(objCls, "prob", "F");

    return JNI_TRUE;
}



extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_tencent_yolov8ncnn_YoloAPI_Detect(JNIEnv *env, jclass thiz, jobject bitmap,jboolean use_gpu) {

    // TODO: implement Detect()
    __android_log_print(ANDROID_LOG_DEBUG, "yolov8ncnn_YoloAPI", "YoloAPI_Detect in ");
    double start_time = ncnn::get_current_time();
    AndroidBitmapInfo info;
    AndroidBitmap_getInfo(env, bitmap, &info);
    const int width = info.width;
    const int height = info.height;
    __android_log_print(ANDROID_LOG_DEBUG, "yolov8ncnn", "Bitmap dimension: (%d,%d)", width,height);
//    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888)
//    {
//        __android_log_print(ANDROID_LOG_ERROR, "yolov8ncnn", "Bitmap format is not RGBA_8888");
//        __android_log_print(ANDROID_LOG_ERROR, "yolov8ncnn", "original:Bitmap format: %d", info.format);
//        info.format = ANDROID_BITMAP_FORMAT_RGBA_8888;


    void *pixels;
    AndroidBitmap_lockPixels(env, bitmap, &pixels);

    if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
        __android_log_print(ANDROID_LOG_DEBUG, "yolov8ncnn", "FORMAT:RGBA_8888");
        // 灰度处理
        for (int i = 0; i < info.width * info.height; i++) {
            uint32_t *pixel_p = static_cast<uint32_t *>(pixels) + i;
            uint32_t pixel = *pixel_p;
            int a = (pixel >> 24) & 0xff; //8
            int r = (pixel >> 16) & 0xff; //8
            int g = (pixel >> 8) & 0xff;  //8
            int b = (pixel) & 0xff;       //8
            int gray = (int) (0.213f * r + 0.715f * g + 0.072f * b); // 32位的公式
//            int gray = (r * 38 + g * 75 + b * 15) >> 7;
            *pixel_p = (a << 24) | (gray << 16) | (gray << 8) | gray;
        }

    }


    AndroidBitmap_unlockPixels(env, bitmap);
    __android_log_print(ANDROID_LOG_DEBUG, "yolov8ncnn", "after:%d",info.format);

    std::vector<Object> objects;
    if (g_yolo)
    {
        __android_log_print(ANDROID_LOG_DEBUG, "yolov8ncnn", "in g_yolo");
        try{
            //这里调用了yolo.cpp的新函数,将在下面步骤中定义
            // 問題在這!!!!!!!!!!!!!!!!!!!!!!!!!!

            float prob_threshold = 0.1f, nms_threshold = 0.5f;
            g_yolo->detectPicture(env, bitmap, width, height, objects);
//            g_yolo->detectPicture(env, bitmap, width, height, objects, prob_threshold, nms_threshold);
//            int result = g_yolo->detect(env, bitmap, width, height, objects);
//didn't go down and crush



        }catch (const std::exception& e) {
            __android_log_print(ANDROID_LOG_ERROR, "yolov8ncnn", "ERROR");
//            e.what() ;
        }

    }
    else
    {
        __android_log_print(ANDROID_LOG_DEBUG, "yolov8ncnn", "g_yolo is NULL!");
//        __android_log_print(ANDROID_LOG_DEBUG, "yolov8ncnn", "g_yolo:%d",g_yolo);
    }

//在detectPicture方法中将结果保存在了 objects中，还需继续对他进换
    jobjectArray jObjArray = env->NewObjectArray(objects.size(),objCls,NULL);

    __android_log_print(ANDROID_LOG_DEBUG, "yolov8ncnn", "%d objects detected!", objects.size());

    for (size_t i=0; i<objects.size(); i++){
//        jobject jObj = env->NewObject(objCls, constructortorId,thiz);
        jobject jObj = env->NewObject(objCls, constructortorId);
        env->SetFloatField(jObj, xId,objects[i].rect.x);
        env->SetFloatField(jObj, yId,objects[i].rect.y);
        env->SetFloatField(jObj, wId,objects[i].rect.width);
        env->SetFloatField(jObj, hId,objects[i].rect.height);
        env->SetIntField(jObj, labelId,objects[i].label);
        env->SetFloatField(jObj, probId,objects[i].prob);

        env->SetObjectArrayElement(jObjArray, i, jObj);
    }

    double elasped = ncnn::get_current_time() - start_time;
    __android_log_print(ANDROID_LOG_DEBUG, "yolov8ncnn", "the entire detection takes %.2fms", elasped);


    return jObjArray;
}




//extern "C"
//JNIEXPORT jboolean JNICALL
//Java_com_tencent_yolov8ncnn_Yolov8Ncnn_loadModel2(JNIEnv* env, jobject thiz, jobject assetManager, jint modelid, jint cpugpu) {
//    // TODO: implement loadModel2
//    if (modelid < 0 || modelid > 6 || cpugpu < 0 || cpugpu > 1)
//    {
//        return JNI_FALSE;
//    }
//
//    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);
//
//    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "loadModel %p", mgr);
//
//    const char* modeltypes[] =
//            {
//                    "lightning",
//                    "thunder",
//            };
//
//    const int target_sizes[] =
//            {
//                    192,
//                    256,
//            };
//
//    const float mean_vals[][3] =
//            {
//                    {127.5f, 127.5f,  127.5f},
//                    {127.5f, 127.5f,  127.5f},
//            };
//
//    const float norm_vals[][3] =
//            {
//                    {1/ 127.5f, 1 / 127.5f, 1 / 127.5f},
//                    {1/ 127.5f, 1 / 127.5f, 1 / 127.5f},
//            };
//
//    const char* modeltype = modeltypes[(int)modelid];
//    int target_size = target_sizes[(int)modelid];
//    bool use_gpu = (int)cpugpu == 1;
//
//    // reload
//    {
//        ncnn::MutexLockGuard g(lock2);
//
//        if (use_gpu && ncnn::get_gpu_count() == 0)
//        {
//            // no gpu
//            delete g_pose;
//            g_pose = 0;
//        }
//        else
//        {
//            if (!g_pose)
//                g_pose = new NanoDet;
//            g_pose->load(mgr, modeltype, target_size, mean_vals[(int)modelid], norm_vals[(int)modelid], use_gpu);
//        }
//    }
//
//    return JNI_TRUE;
//}

