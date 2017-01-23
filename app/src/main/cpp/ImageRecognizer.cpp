
#include "ImageRecognizer.h"

using namespace cv;
using namespace std;


ImageRecognizer::ImageRecognizer()
{
    initExtractorAndMatcher();
}


ImageRecognizer::~ImageRecognizer()
{
}


void ImageRecognizer::setTemplateImage(jbyte* imgArray, jsize len) {
    Mat templateImage;

    // get the image descriptors and key points
    extractImageDescriptors(imgArray, len, &templateData.descriptors,
                            &templateData.keypoints, &templateImage, MAX_IMAGE_DIMENSION, true);

    // get the image frame and store it
    templateData.rows = templateImage.rows;
    templateData.cols = templateImage.cols;

    // release the template image
    templateImage.release();
}


int ImageRecognizer::processImage(jbyte* imgArray, jsize len) {
    // start performance clock
    clock_t tStart = clock();

    // extract features
    cv::Mat imgDescriptors;
    vector<KeyPoint> keyPoints;

    clock_t tExtractionStart = clock();
    extractImageDescriptors(imgArray, len, &imgDescriptors, &keyPoints, MAX_IMAGE_DIMENSION);
    clock_t tExtractionStop = clock();

    // matcher performance counter start
    clock_t tMatcherStart = clock();

    // calculate matches
    vector< vector <cv::DMatch> > matches;
    for(int i = 0; i < 1; ++i) {
        matches.clear();
        pMatcher->knnMatch(imgDescriptors, templateData.descriptors, matches, 2);
    }

    // matcher performance counter stop
    clock_t tMatcherStop = clock();

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
        detectedCornersBuffer = 0;
        detectedCorners = calculateCorners(&good_matches, &keyPoints);

    } else if(detectedCornersBuffer < MAX_FEATURE_POINTS_BUFFER_SIZE) {
        ++detectedCornersBuffer;
    } else {
        detectedCornersBuffer = 0;
        detectedCorners.clear();
    }

    // release the descriptor
    imgDescriptors.release();

    // stop performance clock
    clock_t tStop = clock();
    stringstream ss;
    ss << "Time Overall: " << (((double)(tStop - tStart) / CLOCKS_PER_SEC)) << endl;
    ss << "Time Extraction: " << (((double)(tExtractionStop - tExtractionStart) / CLOCKS_PER_SEC)) << endl;
    __android_log_write(ANDROID_LOG_ERROR, "FEATURE", ss.str().c_str());

    return good_matches.size();
}


void ImageRecognizer::extractImageDescriptors(jbyte* imgArray, jsize len,
        Mat *imgDescriptors, vector<KeyPoint> *keyPoints, double maxImageDim) {

    Mat image;

    extractImageDescriptors(imgArray, len, imgDescriptors, keyPoints, &image, maxImageDim);

    // release image
    image.release();
}


void ImageRecognizer::extractImageDescriptors(jbyte* imgArray, jsize len, Mat *imgDescriptors,
        vector<KeyPoint> *keyPoints, Mat *pImage, double maxImageDim, bool blurFilter) {

    // create a mat object from the jpg byte array
    vector<char> imgVector(imgArray, imgArray + len);
    *pImage = imdecode(imgVector, IMREAD_COLOR);

    // convert to gray image
    cvtColor(*pImage, *pImage, CV_BGR2GRAY);

    // resize the image but keep the ratio
    if(maxImageDim > 0) {
        resizeImage(pImage, maxImageDim);
    }

    // extract the search window
    windowModeActive = extractSearchWindow(pImage);

    if (windowModeActive) {
        // get the window
        *pImage = Mat(*pImage, searchWindow);
    }

    // blur the image
    if(blurFilter) {
        GaussianBlur(*pImage, *pImage, Size(GAUSS_K_SIZE, GAUSS_K_SIZE), GAUSS_SIGMA, GAUSS_SIGMA);
    }

    //-- Step 2: Detect the key points
    pFeatureExtractor->detect(*pImage, keyPoints[0]);
    //extractGoodKeyPoints(keyPoints);

    pFeatureExtractor->compute(*pImage, keyPoints[0], *imgDescriptors);
}


void ImageRecognizer::initExtractorAndMatcher()
{
    const int Threshl = 20;
    const int Octaves = 3;
    const float PatternScales = 1.5f;

    // init extractor
    pFeatureExtractor = BRISK::create(Threshl, Octaves, PatternScales);

    // init matcher
    new FlannBasedMatcher(new flann::LshIndexParams(10, 10, 2));
    pMatcher = DescriptorMatcher::create("BruteForce-Hamming");
}


void ImageRecognizer::resizeImage(Mat *pImage, double maxImageSize) {
    // resize the image
    int maxSize = max(pImage->size().width, pImage->size().height);
    if (maxSize > maxImageSize) {

        double factor = maxImageSize / maxSize;
        Size newSize(
                (int)pImage->size().width * factor,
                (int)pImage->size().height * factor);

        resize(*pImage, *pImage, newSize);
    }
}


bool ImageRecognizer::extractSearchWindow(Mat *pImage) {
    float xMax = 0, yMax = 0;
    float xMin = 10000, yMin = 10000;

    bool result = false;

    searchWindowOffset = Size(0, 0);
    searchWindow = Rect(0, 0, pImage->cols, pImage->rows);

    if (detectedCorners.size() >= 4) {
        for (int i = 0; i < detectedCorners.size(); ++i) {
            xMin = min(xMin, detectedCorners[i].x);
            xMax = max(xMax, detectedCorners[i].x);
            yMin = min(yMin, detectedCorners[i].y);
            yMax = max(yMax, detectedCorners[i].y);
        }

        xMin = max((float)0, (float) (xMin * 0.8));
        yMin = max((float)0, (float) (yMin * 0.8));
        xMax = min(pImage->cols - xMin,(float) ((xMax - xMin) * 1.2));
        yMax = min(pImage->rows - yMin,(float) ((yMax - yMin) * 1.2));

        searchWindow = Rect(xMin,yMin,xMax,yMax);

        if (searchWindow.width > 50 && searchWindow.height > 50) {
            searchWindowOffset = Size(xMin, yMin);
            result = true;
        }
    }

    return result;
}


vector<Point2f> ImageRecognizer::calculateCorners(vector<DMatch> *pGoodMatches, vector<KeyPoint> *pKeyPointsScene) {

    std::vector<Point2f> framePoints;
    std::vector<Point2f> templatePoints;

    for (int i = 0; i < pGoodMatches->size(); i++)
    {
        //Get the key points from the good matches
        framePoints.push_back(pKeyPointsScene->at(pGoodMatches->at(i).queryIdx).pt);
        templatePoints.push_back(templateData.keypoints.at(pGoodMatches->at(i).trainIdx).pt);
    }

    Mat H = findHomography(templatePoints, framePoints, CV_RANSAC);

    // get the template corners
    std::vector<Point2f> obj_corners(4);
    obj_corners[0] = cvPoint(0, 0);
    obj_corners[1] = cvPoint(templateData.cols, 0);
    obj_corners[2] = cvPoint(templateData.cols, templateData.rows);
    obj_corners[3] = cvPoint(0, templateData.rows);
    std::vector<Point2f> scene_corners(4);

    // calculate the perspective change to get the corners of the input image
    perspectiveTransform(obj_corners, scene_corners, H);

    // if windwo mode is active -> add the offset
    if (windowModeActive) {
        for (int i = 0; i < scene_corners.size(); ++i) {
            scene_corners[i].x += searchWindowOffset.width;
            scene_corners[i].y += searchWindowOffset.height;
        }
    }

    return scene_corners;
}
