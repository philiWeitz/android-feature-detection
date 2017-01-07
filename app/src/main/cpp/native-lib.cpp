#include <jni.h>
#include <string>
#include <vector>
#include <sstream>
#include <android/log.h>

#include "opencv2/core.hpp"
#include "opencv2/imgproc.hpp"
#include "opencv2/imgcodecs.hpp"
#include "opencv2/features2d.hpp"
#include "opencv2/calib3d.hpp"

using namespace cv;
using namespace std;


const static double MAX_IMAGE_DIMENSION = 500.0;

const double KNN_MATCH_RATIO = 0.75;
const int MIN_MATCHES = 11;

vector<KeyPoint> templateKeyPoints;
Mat templateDescriptors;

Ptr<Feature2D> pFeatureExtractor;
Ptr<DescriptorMatcher> pMatcher;

int templateRows = 0;
int templateCols = 0;

vector<Point2f> featurePoints;



void resizeImage(Mat *pImg, double maxImageSize);

void extractImageDescriptors(JNIEnv *env, jbyteArray *pImgByteArray,
                             Mat *imgDescriptors, vector<KeyPoint> *keyPoints, double maxImageDim = -1);
void extractImageDescriptors(JNIEnv *env, jbyteArray *pImgByteArray,
                             Mat *imgDescriptors, vector<KeyPoint> *keyPoints, Mat *pImage, double maxImageDim = -1);

vector<Point2f> calculateCorners(vector<DMatch> *pGoodMatches, vector<KeyPoint> *pKeyPointsScene,
                                 vector<KeyPoint> *pKeyPointsTemplate);



extern "C"
void
Java_sandbox_org_featuredetection_jni_NativeWrapper_initFeatureDetection(
        JNIEnv *env,
        jobject thiz) {

    //-- Step 1: Initialize all components (BRISK and Flann)
    int Threshl = 20;
    int Octaves = 3;
    float PatternScales = 1.5f;

    pFeatureExtractor = BRISK::create(Threshl, Octaves, PatternScales);
    new FlannBasedMatcher(new flann::LshIndexParams(10, 10, 2));

    pMatcher = DescriptorMatcher::create("BruteForce-Hamming");
}



extern "C"
jintArray
Java_sandbox_org_featuredetection_jni_NativeWrapper_getFramePoints(
        JNIEnv *env, jobject thiz) {

    jintArray javaIntArray;
    javaIntArray = env->NewIntArray(8);

    if(featurePoints.size() >= 4) {
        jint result[8];
        result[0] = featurePoints[0].x;
        result[1] = featurePoints[0].y;
        result[2] = featurePoints[1].x;
        result[3] = featurePoints[1].y;
        result[4] = featurePoints[2].x;
        result[5] = featurePoints[2].y;
        result[6] = featurePoints[3].x;
        result[7] = featurePoints[3].y;

        env->SetIntArrayRegion(javaIntArray, 0, 8, result);
    }

    return javaIntArray;
}


extern "C"
void
Java_sandbox_org_featuredetection_jni_NativeWrapper_setTemplateImage(
        JNIEnv *env, jobject thiz, jbyteArray imgByteArray) {

    Mat templateImage;

    extractImageDescriptors(env, &imgByteArray, &templateDescriptors,
                            &templateKeyPoints, &templateImage, MAX_IMAGE_DIMENSION);

    templateRows = templateImage.rows;
    templateCols = templateImage.cols;

    templateImage.release();
}


extern "C"
jint
Java_sandbox_org_featuredetection_jni_NativeWrapper_processImage(
        JNIEnv *env, jobject thiz, jbyteArray imgByteArray) {

    // start performance clock
    clock_t tStart = clock();

    // extract features
    cv::Mat imgDescriptors;
    vector<KeyPoint> keyPoints;
    extractImageDescriptors(env, &imgByteArray, &imgDescriptors, &keyPoints, MAX_IMAGE_DIMENSION);

    // calculate matches
    vector< vector <cv::DMatch> > matches;
    pMatcher->knnMatch(imgDescriptors, templateDescriptors, matches, 2);

    // extract the good matches
    std::vector< DMatch > good_matches;
    for (int i = 0; i < matches.size(); ++i) {
        if (matches[i].size() >= 2) {
            if (matches[i][0].distance < KNN_MATCH_RATIO * matches[i][1].distance) {
                good_matches.push_back(matches[i][0]);
            }
        }
    }

    if(good_matches.size() >= MIN_MATCHES) {
        featurePoints = calculateCorners(&good_matches, &keyPoints, &templateKeyPoints);
    } else {
        featurePoints.clear();
    }

    // release the descriptor
    imgDescriptors.release();

    // stop performance clock
    clock_t tStop = clock();
    stringstream ss;
    ss << "Time: " << (((double)(tStop - tStart) / CLOCKS_PER_SEC)) << endl;
    __android_log_write(ANDROID_LOG_ERROR, "FEATURE", ss.str().c_str());

    return good_matches.size();
}


void extractImageDescriptors(JNIEnv *env, jbyteArray *pImgByteArray,
                             Mat *imgDescriptors, vector<KeyPoint> *keyPoints, double maxImageDim) {

    Mat image;

    extractImageDescriptors(env, pImgByteArray, imgDescriptors, keyPoints, &image, maxImageDim);
    // release image
    image.release();
}


void extractImageDescriptors(JNIEnv *env, jbyteArray *pImgByteArray,
                             Mat *imgDescriptors, vector<KeyPoint> *keyPoints, Mat *pImage, double maxImageDim) {

    // get the image array
    jbyte* imgArray = env->GetByteArrayElements(*pImgByteArray, NULL);
    jsize len = env->GetArrayLength(*pImgByteArray);

    // create a mat object from the jpg byte array
    vector<char> imgVector(imgArray, imgArray + len);
    *pImage = imdecode(imgVector, IMREAD_COLOR);

    // resize the image but keep the ratio
    if(maxImageDim > 0) {
        resizeImage(pImage, maxImageDim);
    }

    // convert to gray image
    cvtColor(*pImage, *pImage, CV_BGR2GRAY);

    //-- Step 2: Detect the key points
    pFeatureExtractor->detect(*pImage, keyPoints[0]);
    pFeatureExtractor->compute(*pImage, keyPoints[0], *imgDescriptors);

    // release byte array
    env->ReleaseByteArrayElements(*pImgByteArray, imgArray, JNI_ABORT);
}


void resizeImage(Mat *pImg, double maxImageSize) {
    // resize the image
    int maxSize = max(pImg->size().width, pImg->size().height);
    if (maxSize > maxImageSize) {

        double factor = maxImageSize / maxSize;
        Size newSize(
                (int)pImg->size().width * factor,
                (int)pImg->size().height * factor);

        resize(*pImg, *pImg, newSize);
    }
}


vector<Point2f> calculateCorners(vector<DMatch> *pGoodMatches, vector<KeyPoint> *pKeyPointsScene,
                                      vector<KeyPoint> *pKeyPointsTemplate) {

    std::vector<Point2f> framePoints;
    std::vector<Point2f> templatePoints;

    for (int i = 0; i < pGoodMatches->size(); i++)
    {
        //-- Get the keypoints from the good matches
        framePoints.push_back(pKeyPointsScene->at(pGoodMatches->at(i).queryIdx).pt);
        templatePoints.push_back(pKeyPointsTemplate->at(pGoodMatches->at(i).trainIdx).pt);
    }

    Mat H = findHomography(templatePoints, framePoints, CV_RANSAC);

    // get the template corners
    std::vector<Point2f> obj_corners(4);
    obj_corners[0] = cvPoint(0, 0); obj_corners[1] = cvPoint(templateCols, 0);
    obj_corners[2] = cvPoint(templateCols, templateRows); obj_corners[3] = cvPoint(0, templateRows);
    std::vector<Point2f> scene_corners(4);

    // calculate the perspective change to get the corners of the input image
    perspectiveTransform(obj_corners, scene_corners, H);

    return scene_corners;
}


