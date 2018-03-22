//
// Created by 杜圣哲 on 20180322.
//

#include <jni.h>
#include <string>
#include <opencv/cv.h>
#include <opencv2/opencv.hpp>

using namespace cv;

extern "C"
JNIEXPORT jstring JNICALL
Java_com_hitices_autopatrol_activity_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    Mat mat = imread("");
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}