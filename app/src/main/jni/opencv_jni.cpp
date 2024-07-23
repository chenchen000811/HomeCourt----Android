//
// Created by chenchen on 2024/5/6.
//
#include <jni.h>
#include <tuple>
#include <vector>
#include "opencv.h" // Include your Opencv class header file


Point2i convertToPoint2i(cv::Point2i pt) {
    return Point2i(pt.x, pt.y);
}


//extern "C" {

//JNIEXPORT jobject JNICALL Java_com_tencent_yolov8ncnn_OpenCvAPI_getMatrix(JNIEnv* env, jobject thiz, jint srcWidth, jint srcHeight, jbyteArray srcData, jobject tl, jobject tr, jobject bl, jobject br) {
//    // Convert jbyteArray to uchar* (unsigned char*)
//    jbyte* srcDataPtr = env->GetByteArrayElements(srcData, NULL);
//    jsize srcDataLength = env->GetArrayLength(srcData);
//
//    uchar* srcDataNative = new uchar[srcDataLength];
//    memcpy(srcDataNative, srcDataPtr, srcDataLength);
//    env->ReleaseByteArrayElements(srcData, srcDataPtr, JNI_ABORT);
//
//    // Convert Point2i objects to cv::Point2i
//    jclass point2iClass = env->GetObjectClass(tl);
//    jfieldID xFieldID = env->GetFieldID(point2iClass, "x", "I");
//    jfieldID yFieldID = env->GetFieldID(point2iClass, "y", "I");
//
//    jint tlX = env->GetIntField(tl, xFieldID);
//    jint tlY = env->GetIntField(tl, yFieldID);
//    cv::Point2i tlNative(tlX, tlY);
//    Point2i TL = convertToPoint2i(tlNative);
//
//    jint trX = env->GetIntField(tr, xFieldID);
//    jint trY = env->GetIntField(tr, yFieldID);
//    cv::Point2i trNative(trX, trY);
//    Point2i TR = convertToPoint2i(trNative);
//
//    jint blX = env->GetIntField(bl, xFieldID);
//    jint blY = env->GetIntField(bl, yFieldID);
//    cv::Point2i blNative(blX, blY);
//    Point2i BL = convertToPoint2i(blNative);
//
//    jint brX = env->GetIntField(br, xFieldID);
//    jint brY = env->GetIntField(br, yFieldID);
//    cv::Point2i brNative(brX, brY);
//    Point2i BR = convertToPoint2i(brNative);
//
//    // Call the getMatrix function
//    Opencv opencv;
//    auto result = opencv.getMatrix(srcWidth, srcHeight, srcDataNative, TL, TR, BL, BR);
//
//    // Convert the result to Java types
//    jint transformedWidth = std::get<0>(result);
//    jint transformedHeight = std::get<1>(result);
//    std::vector<uchar> transformedData = std::get<2>(result);
//
//    // Convert std::vector<uchar> to jbyteArray
////    jbyteArray transformedDataArray = env->NewByteArray(transformedData.size());
////    env->SetByteArrayRegion(transformedDataArray, 0, transformedData.size(), reinterpret_cast<jbyte*>(transformedData.data()));
//
//
//// Create a Java List object
//    jclass arrayListClass = env->FindClass("java/util/ArrayList");
//    jmethodID arrayListConstructor = env->GetMethodID(arrayListClass, "<init>", "()V");
//    jobject resultList = env->NewObject(arrayListClass, arrayListConstructor);
//
//// Add each element from transformedData to the Java list
//    jmethodID arrayListAddMethod = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");
//    for (uchar value : transformedData) {
//        jbyte byteValue = static_cast<jbyte>(value);
//        env->CallBooleanMethod(resultList, arrayListAddMethod, env->NewObject(env->FindClass("java/lang/Byte"), env->GetMethodID(env->FindClass("java/lang/Byte"), "<init>", "(B)V"), byteValue));
//    }
//
//// Now resultList is a Java List containing the bytes from transformedData
//
//
//
//
//    // Create a Java tuple to hold the transformed width, height, and data
//    jclass resultClass = env->FindClass("com/tencent/yolov8ncnn/Matrix");
////    jmethodID tupleConstructor = env->GetMethodID(tupleClass, "<init>", "(III[B)V");
//    jmethodID constructor = env->GetMethodID(resultClass, "<init>", "(IILjava/util/List;)V");
//    jobject resultTuple = env->NewObject(resultClass, constructor, transformedWidth, transformedHeight, resultList);
//
//    delete[] srcDataNative;
//    return resultTuple;
//}
//
//JNIEXPORT jobject JNICALL Java_com_tencent_yolov8ncnn_OpenCvAPI_calculateUpdatedPosition(JNIEnv* env, jobject thiz, jint originalX, jint originalY, jfloatArray transformationMatrix) {
//    // Convert jfloatArray to std::vector<float>
//    jfloat* transMatPtr = env->GetFloatArrayElements(transformationMatrix, NULL);
//    jsize transMatLength = env->GetArrayLength(transformationMatrix);
//
//    std::vector<float> transMatNative(transMatPtr, transMatPtr + transMatLength);
//    env->ReleaseFloatArrayElements(transformationMatrix, transMatPtr, JNI_ABORT);
//
//    // Call the calculateUpdatedPosition function
//    Opencv opencv;
//    auto result = opencv.calculateUpdatedPosition(originalX, originalY, transMatNative);
//
//    // Convert the result to Java types
////    jint updatedX = std::get<0>(result);
////    jint updatedY = std::get<1>(result);
////
////    // Create a Java pair to hold the updated X and Y coordinates
////    jclass pairClass = env->FindClass("android/util/Pair");
////    jmethodID pairConstructor = env->GetMethodID(pairClass, "<init>", "(Ljava/lang/Object;Ljava/lang/Object;)V");
////    jobject resultPair = env->NewObject(pairClass, pairConstructor, updatedX, updatedY);
//
//
//// Convert the result to Java types
//    jint updatedX = std::get<0>(result);
//    jint updatedY = std::get<1>(result);
//
//// Wrap the updated X and Y coordinates in Integer objects
//    jobject updatedXObject = env->NewObject(env->FindClass("java/lang/Integer"), env->GetMethodID(env->FindClass("java/lang/Integer"), "<init>", "(I)V"), updatedX);
//    jobject updatedYObject = env->NewObject(env->FindClass("java/lang/Integer"), env->GetMethodID(env->FindClass("java/lang/Integer"), "<init>", "(I)V"), updatedY);
//
//// Create a Java pair to hold the updated X and Y coordinates
//    jclass pairClass = env->FindClass("android/util/Pair");
//    jmethodID pairConstructor = env->GetMethodID(pairClass, "<init>", "(Ljava/lang/Object;Ljava/lang/Object;)V");
//    jobject resultPair = env->NewObject(pairClass, pairConstructor, updatedXObject, updatedYObject);
//
//    return resultPair;
//}
//
//
//
//
//
//}


extern "C"
JNIEXPORT jobject JNICALL
Java_com_tencent_yolov8ncnn_OpenCvAPI_convertCircleCoordinates(JNIEnv *env, jobject thiz,
                                                               jint src_width, jint src_height,
                                                               jobject center, jfloat radius,
                                                               jobject tl, jobject tr, jobject bl,
                                                               jobject br) {
    // TODO: implement convertCircleCoordinates()
    // Retrieve fields of the Java objects
    jclass point2iClass = env->GetObjectClass(center);
    jfieldID xFieldID = env->GetFieldID(point2iClass, "x", "I");
    jfieldID yFieldID = env->GetFieldID(point2iClass, "y", "I");
    jint center_x = env->GetIntField(center, xFieldID);
    jint center_y = env->GetIntField(center, yFieldID);

    // Convert retrieved fields into C++ data types
    Point2i center_point(center_x, center_y);

    // Similar conversion for tl, tr, bl, br points
    jclass tlClass = env->GetObjectClass(tl);
    jfieldID tlXFieldID = env->GetFieldID(tlClass, "x", "I");
    jfieldID tlYFieldID = env->GetFieldID(tlClass, "y", "I");
    jint tl_x = env->GetIntField(tl, tlXFieldID);
    jint tl_y = env->GetIntField(tl, tlYFieldID);
// Convert retrieved fields into C++ data types for tl point
    Point2i tl_point(tl_x, tl_y);


    jclass trClass = env->GetObjectClass(tr);
    jfieldID trXFieldID = env->GetFieldID(trClass, "x", "I");
    jfieldID trYFieldID = env->GetFieldID(trClass, "y", "I");
    jint tr_x = env->GetIntField(tr, trXFieldID);
    jint tr_y = env->GetIntField(tr, trYFieldID);
    Point2i tr_point(tr_x, tr_y);

// Similar conversion for bl point
    jclass blClass = env->GetObjectClass(bl);
    jfieldID blXFieldID = env->GetFieldID(blClass, "x", "I");
    jfieldID blYFieldID = env->GetFieldID(blClass, "y", "I");
    jint bl_x = env->GetIntField(bl, blXFieldID);
    jint bl_y = env->GetIntField(bl, blYFieldID);
    Point2i bl_point(bl_x, bl_y);

// Similar conversion for br point
    jclass brClass = env->GetObjectClass(br);
    jfieldID brXFieldID = env->GetFieldID(brClass, "x", "I");
    jfieldID brYFieldID = env->GetFieldID(brClass, "y", "I");
    jint br_x = env->GetIntField(br, brXFieldID);
    jint br_y = env->GetIntField(br, brYFieldID);
    Point2i br_point(br_x, br_y);
    // Call the Opencv::convertCircleCoordinates method
    Opencv opencv;
    auto result = opencv.convertCircleCoordinates(src_width, src_height, center_point, radius, tl_point, tr_point, bl_point, br_point);


    // Extract values from the result tuple
    cv::Point2f point = std::get<0>(result);
    radius = std::get<1>(result);

// Convert the result to Java types
    jclass pointFClass = env->FindClass("android/graphics/PointF");
    jmethodID pointFConstructor = env->GetMethodID(pointFClass, "<init>", "(FF)V");
    jobject pointFObject = env->NewObject(pointFClass, pointFConstructor, point.x, point.y);

    jclass floatClass = env->FindClass("java/lang/Float");
    jmethodID floatConstructor = env->GetMethodID(floatClass, "<init>", "(F)V");
    jobject floatObject = env->NewObject(floatClass, floatConstructor, radius);



    // Create and return the Pair<PointF, Float>
    jclass pairClass = env->FindClass("android/util/Pair");
    jmethodID pairConstructor = env->GetMethodID(pairClass, "<init>", "(Ljava/lang/Object;Ljava/lang/Object;)V");
    jobject pairObject = env->NewObject(pairClass, pairConstructor, pointFObject, floatObject);

    return pairObject;



}