#include <jni.h>
#include <string>
#include <vector>

#include "opencv2/core.hpp"
#include "opencv2/imgproc.hpp"
#include "opencv2/imgcodecs.hpp"
#include "opencv2/features2d.hpp"

using namespace cv;
using namespace std;


const static double MAX_IMAGE_DIMENSION = 640.0;

Ptr<BRISK> BRISKD;
FlannBasedMatcher *pMatcher;

vector<KeyPoint> templateKeyPoints;
Mat templateDescriptors;



void extractImageDescriptors(JNIEnv *env, jbyteArray *pImgByteArray, Mat *imgDescriptors, double maxImageDim = -1);


extern "C"
void
Java_sandbox_org_featuredetection_jni_NativeWrapper_initFeatureDetection(
        JNIEnv *env,
        jobject thiz) {

    //-- Step 1: Initialize all components (BRISK and Flann)
    int Threshl = 30;
    int Octaves = 3;
    float PatternScales = 1.0f;

    BRISKD = BRISK::create(Threshl, Octaves, PatternScales);
    pMatcher = new FlannBasedMatcher(new flann::LshIndexParams(20, 10, 2));
}


extern "C"
void
Java_sandbox_org_featuredetection_jni_NativeWrapper_setTemplateImage(
        JNIEnv *env, jobject thiz, jbyteArray imgByteArray) {

    extractImageDescriptors(env, &imgByteArray, &templateDescriptors);
}


extern "C"
jint
Java_sandbox_org_featuredetection_jni_NativeWrapper_processImage(
        JNIEnv *env, jobject thiz, jbyteArray imgByteArray) {

    // extract features
    cv::Mat imgDescriptors;
    extractImageDescriptors(env, &imgByteArray, &imgDescriptors);

    // calculate matches
    vector< vector <cv::DMatch> > matches;
    pMatcher->knnMatch(imgDescriptors, templateDescriptors, matches, 2);

    // extract the good matches
    std::vector< DMatch > good_matches;
    for (int i = 0; i < matches.size(); ++i) {
        if (matches[i].size() >= 2) {
            if (matches[i][0].distance < 0.7 * matches[i][1].distance) {
                good_matches.push_back(matches[i][0]);
            }
        }
    }

    // release the descriptor
    imgDescriptors.release();

    // return the amount of good matches
    return good_matches.size();
}


void extractImageDescriptors(JNIEnv *env, jbyteArray *pImgByteArray, Mat *imgDescriptors, double maxImageDim) {

    // get the image array
    jbyte* imgArray = env->GetByteArrayElements(*pImgByteArray, NULL);
    jsize len = env->GetArrayLength(*pImgByteArray);

    // create a mat object from the jpg byte array
    vector<char> imgVector(imgArray, imgArray + len);
    Mat image = imdecode(imgVector, IMREAD_COLOR);

    // resize the image but keep the ratio
    if(maxImageDim > 0) {

        int maxSize = max(image.size().width, image.size().height);
        if (maxSize > MAX_IMAGE_DIMENSION) {

            double factor = MAX_IMAGE_DIMENSION / maxSize;
            Size newSize(
                    (int) image.size().width * factor,
                    (int) image.size().height * factor);

            resize(image, image, newSize);
        }
    }

    // convert to gray image
    cvtColor(image, image, CV_BGR2GRAY);

    //-- Step 2: Detect the key points
    std::vector<cv::KeyPoint> imgKeypoints;

    BRISKD->detect(image, imgKeypoints);
    BRISKD->compute(image, imgKeypoints, *imgDescriptors);

    // release image
    image.release();
    // release byte array
    env->ReleaseByteArrayElements(*pImgByteArray, imgArray, JNI_ABORT);
}