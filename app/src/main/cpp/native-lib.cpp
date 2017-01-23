#include <jni.h>
#include "ImageRecognizer.h"


extern "C"
jlong
Java_sandbox_org_featuredetection_jni_NativeWrapper_initFeatureDetection(
        JNIEnv *env, jobject) {

    ImageRecognizer *pImageRecognizer = new ImageRecognizer();
    pImageRecognizer->initExtractorAndMatcher();

    return reinterpret_cast<jlong>(pImageRecognizer);
}


extern "C"
jintArray
Java_sandbox_org_featuredetection_jni_NativeWrapper_getFramePoints(
        JNIEnv *env, jobject, jlong handle) {

    // get the image recognizer instance
    ImageRecognizer *pImageRecognizer = reinterpret_cast<ImageRecognizer*>(handle);

    jintArray javaIntArray;
    javaIntArray = env->NewIntArray(8);

    jint result[8];
    // if successful -> set the frame points
    if(pImageRecognizer->getFramePoints(&result[0])) {
        env->SetIntArrayRegion(javaIntArray, 0, 8, result);
    }

    return javaIntArray;
}


extern "C"
void
Java_sandbox_org_featuredetection_jni_NativeWrapper_setTemplateImage(
        JNIEnv *env, jobject, jlong handle, jbyteArray imgByteArray) {

    // get the image array
    jbyte* imgArray = env->GetByteArrayElements(imgByteArray, NULL);
    jsize len = env->GetArrayLength(imgByteArray);

    // get the image recognizer instance
    ImageRecognizer *pImageRecognizer = reinterpret_cast<ImageRecognizer*>(handle);

    // set the template image
    pImageRecognizer->setTemplateImage(imgArray, len);

    // release byte array
    env->ReleaseByteArrayElements(imgByteArray, imgArray, JNI_ABORT);
}


extern "C"
jint
Java_sandbox_org_featuredetection_jni_NativeWrapper_processImage(
        JNIEnv *env, jobject, jlong handle, jbyteArray imgByteArray) {

    // get the image recognizer instance
    ImageRecognizer *pImageRecognizer = reinterpret_cast<ImageRecognizer*>(handle);

    // get the image array
    jbyte* imgArray = env->GetByteArrayElements(imgByteArray, NULL);
    jsize len = env->GetArrayLength(imgByteArray);

    // set the template image
    int goodMatchesCount = pImageRecognizer->processImage(imgArray, len);

    // release byte array
    env->ReleaseByteArrayElements(imgByteArray, imgArray, JNI_ABORT);

    return goodMatchesCount;
}


extern "C"
void
Java_sandbox_org_featuredetection_jni_NativeWrapper_destroyHandle(
        JNIEnv *env, jobject, jlong handle) {

    // get the image recognizer instance
    ImageRecognizer *pImageRecognizer = reinterpret_cast<ImageRecognizer*>(handle);
    delete pImageRecognizer;
}