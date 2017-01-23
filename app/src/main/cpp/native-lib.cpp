#include <jni.h>
#include <string>
#include <vector>
#include <sstream>
#include <algorithm>
#include <android/log.h>

#include "opencv2/core.hpp"
#include "opencv2/imgproc.hpp"
#include "opencv2/imgcodecs.hpp"
#include "opencv2/features2d.hpp"
#include "opencv2/calib3d.hpp"

#include "ImageRecognizer.h"



ImageRecognizer imageRecognizer;


extern "C"
void
Java_sandbox_org_featuredetection_jni_NativeWrapper_initFeatureDetection(
        JNIEnv *env,
        jobject thiz) {

    imageRecognizer.initExtractorAndMatcher();
}


extern "C"
jintArray
Java_sandbox_org_featuredetection_jni_NativeWrapper_getFramePoints(
        JNIEnv *env, jobject thiz) {

    jintArray javaIntArray;
    javaIntArray = env->NewIntArray(8);

    jint result[8];
    // if successful -> set the frame points
    if(imageRecognizer.getFramePoints(&result[0])) {
        env->SetIntArrayRegion(javaIntArray, 0, 8, result);
    }

    return javaIntArray;
}


extern "C"
void
Java_sandbox_org_featuredetection_jni_NativeWrapper_setTemplateImage(
        JNIEnv *env, jobject thiz, jbyteArray imgByteArray) {

    // get the image array
    jbyte* imgArray = env->GetByteArrayElements(imgByteArray, NULL);
    jsize len = env->GetArrayLength(imgByteArray);

    // set the template image
    imageRecognizer.setTemplateImage(imgArray, len);

    // release byte array
    env->ReleaseByteArrayElements(imgByteArray, imgArray, JNI_ABORT);
}


extern "C"
jint
Java_sandbox_org_featuredetection_jni_NativeWrapper_processImage(
        JNIEnv *env, jobject thiz, jbyteArray imgByteArray) {

    // get the image array
    jbyte* imgArray = env->GetByteArrayElements(imgByteArray, NULL);
    jsize len = env->GetArrayLength(imgByteArray);

    // set the template image
    int goodMatchesCount = imageRecognizer.processImage(imgArray, len);

    // release byte array
    env->ReleaseByteArrayElements(imgByteArray, imgArray, JNI_ABORT);

    return goodMatchesCount;
}