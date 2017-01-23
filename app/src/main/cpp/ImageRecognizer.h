//
// Created by my on 23.01.2017.
//

#ifndef ANDROID_FEATURE_DETECTION_IMAGERECOGNIZER_H
#define ANDROID_FEATURE_DETECTION_IMAGERECOGNIZER_H

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



struct TEMPLATE_IMAGE {
    cv::Mat descriptors;
    std::vector<cv::KeyPoint> keypoints;
    int rows = 0;
    int cols = 0;
};


class ImageRecognizer {
public:
    ImageRecognizer();
    ~ImageRecognizer();

    // matches a JPEG image against a template image
    int processImage(jbyte* imgArray, jsize len);
    // sets a template image
    void setTemplateImage(jbyte* imgArray, jsize len);

private:

    // creates the BRISK extractor and brute force matcher
    void initExtractorAndMatcher();
    // resizes the image but keeps the ratio
    void resizeImage(cv::Mat *pImage, double maxImageSize);
    // extract the search window if a previous recognition was successful
    bool extractSearchWindow(cv::Mat *pImage);

    // extracts the image key points and descriptors
    void extractImageDescriptors(jbyte* imgArray, jsize len, cv::Mat *imgDescriptors,
        std::vector<cv::KeyPoint> *keyPoints, double maxImageDim = -1);

    // extracts the image key points and descriptors and offers a gauss blur option
    void extractImageDescriptors(jbyte* imgArray, jsize len, cv::Mat *imgDescriptors,
        std::vector<cv::KeyPoint> *keyPoints, cv::Mat *pImage, double maxImageDim = -1, bool blurFilter = false);

    // extracts the corners of a found image match
    std::vector<cv::Point2f> calculateCorners(std::vector<cv::DMatch> *pGoodMatches,
        std::vector<cv::KeyPoint> *pKeyPointsScene);

    // private variables
    int detectedCornersBuffer = 0;
    std::vector<cv::Point2f> detectedCorners;
    cv::Ptr<cv::Feature2D> pFeatureExtractor;
    cv::Ptr<cv::DescriptorMatcher> pMatcher;

    cv::Size searchWindowOffset;
    cv::Rect searchWindow;
    bool windowModeActive;


    TEMPLATE_IMAGE templateData;

    // static const expressions

    static const int GAUSS_SIGMA = 3;
    static const int GAUSS_K_SIZE = (GAUSS_SIGMA * 3) | 1;

    static const int MIN_MATCHES = 11;
    static const double KNN_MATCH_RATIO = 0.75;
    static const double MAX_IMAGE_DIMENSION = 500.0;
    static const int MAX_FEATURE_POINTS_BUFFER_SIZE = 3;
};


#endif //ANDROID_FEATURE_DETECTION_IMAGERECOGNIZER_H
