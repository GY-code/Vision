//#include <jni.h>
//#include <string>
//
//extern "C" JNIEXPORT jstring JNICALL
//Java_com_example_finalopencvproject_MainActivity_stringFromJNI(
//        JNIEnv *env,
//        jobject /* this */) {
//    std::string hello = "Hello from C++";
//    return env->NewStringUTF(hello.c_str());
//}
#include <jni.h>
#include <opencv2/opencv.hpp>
#include <opencv2/core/base.hpp>
#import "opencv2/stitching.hpp"
#import "opencv2/imgcodecs.hpp"
#include <opencv2/video/tracking.hpp>
#include <opencv2/core/ocl.hpp>
#include "opencv2/core/version.hpp"


#define BORDER_GRAY_LEVEL 0

#include <android/log.h>
#include <android/bitmap.h>

#define SSTR( x ) static_cast< std::ostringstream & >(( std::ostringstream() << std::dec << x )).str()


#define LOG_TAG    "DDLog-jni"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG, __VA_ARGS__)
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG, __VA_ARGS__)
#define LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG, __VA_ARGS__)
#define LOGF(...)  __android_log_print(ANDROID_LOG_FATAL,LOG_TAG, __VA_ARGS__)
using namespace cv;
using namespace std;
char filepath1[100] = "/storage/emulated/0/panorama_stitched.jpg";
cv::Mat finalMat;


//extern "C"
//JNIEXPORT jintArray
//Java_t20220049_sw_1vision_utils_Pano_stitchImages(JNIEnv *env, jclass type,
//                                                  jobjectArray paths) {
//    try {
//        jstring jstr;
//        jsize len = env->GetArrayLength(paths);
//        std::vector<cv::Mat> mats;
//        for (int i = 0; i < len; i++) {
//            jstr = (jstring) env->GetObjectArrayElement(paths, i);
//            const char *path = (char *) env->GetStringUTFChars(jstr, 0);
//            LOGI("path %s", path);
//            cv::Mat mat = cv::imread(path);
////        cvtColor(mat, mat, CV_RGBA2RGB);
//            mats.push_back(mat);
//        }
//        LOGI("开始拼接......");
//        cv::Stitcher stitcher = cv::Stitcher::createDefault(false);
////        cv::Stitcher stitcher = cv::Stitcher::create();
//        LOGI("1");
//        //stitcher.setRegistrationResol(0.6);
//        // stitcher.setWaveCorrection(false);
//        /*=match_conf默认是0.65，我选0.8，选太大了就没特征点啦,0.8都失败了*/
//        detail::BestOf2NearestMatcher *matcher = new detail::BestOf2NearestMatcher(false, 0.5f);
//        stitcher.setFeaturesMatcher(matcher);
//        stitcher.setBundleAdjuster(new detail::BundleAdjusterRay());
//        stitcher.setSeamFinder(new detail::NoSeamFinder);
//        stitcher.setExposureCompensator(new detail::NoExposureCompensator());//曝光补偿
//        stitcher.setBlender(new detail::FeatherBlender());
//        Stitcher::Status state = stitcher.stitch(mats, finalMat);
//        LOGI("2");
//        //此时finalMat是bgr类型
//        LOGI("拼接结果: %d", state);
////        finalMat = clipping(finalMat);
//        jintArray jint_arr = env->NewIntArray(3);
//        jint *elems = env->GetIntArrayElements(jint_arr, NULL);
//        elems[0] = state;//状态码
//        elems[1] = finalMat.cols;//宽
//        elems[2] = finalMat.rows;//高
//        if (state == cv::Stitcher::OK) {
//            LOGI("拼接成功: OK");
//        } else {
//            LOGI("拼接失败:fail code %d", state);
//        }
//        //同步
//        env->ReleaseIntArrayElements(jint_arr, elems, 0);
////    bool isSave  = cv::imwrite(filepath1, finalMat);
////    LOGI("是否存储成功:%d",isSave);
//        return jint_arr;
//    } catch (Exception &e) {
//        jclass je = env->FindClass("java/lang/Exception");
//        env->ThrowNew(je, e.what());
//    }
//    return nullptr;
//}
//
//extern "C"
//JNIEXPORT void JNICALL
//Java_t20220049_sw_1vision_utils_Pano_getMat(JNIEnv *env, jclass type, jlong mat) {
//    try {
//        LOGI("开始获取mat...");
//        Mat *res = (Mat *) mat;
//        res->create(finalMat.rows, finalMat.cols, finalMat.type());
//        memcpy(res->data, finalMat.data, finalMat.rows * finalMat.step);
//        LOGI("获取成功");
//    } catch (Exception &e) {
//        jclass je = env->FindClass("java/lang/Exception");
//        env->ThrowNew(je, e.what());
//    }
//}
//
////将mat转化成bitmap
//void MatToBitmap(JNIEnv *env, Mat &mat, jobject &bitmap, jboolean needPremultiplyAlpha) {
//    AndroidBitmapInfo info;
//    void *pixels = 0;
//    Mat &src = mat;
//    try {
//        LOGD("nMatToBitmap");
//        CV_Assert(AndroidBitmap_getInfo(env, bitmap, &info) >= 0);
//        LOGD("nMatToBitmap1");
//        CV_Assert(info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 ||
//                  info.format == ANDROID_BITMAP_FORMAT_RGB_565);
//        LOGD("nMatToBitmap2 :%d  : %d  :%d", src.dims, src.rows, src.cols);
//        CV_Assert(src.dims == 2 && info.height == (uint32_t) src.rows &&
//                  info.width == (uint32_t) src.cols);
//        LOGD("nMatToBitmap3");
//        CV_Assert(src.type() == CV_8UC1 || src.type() == CV_8UC3 || src.type() == CV_8UC4);
//        LOGD("nMatToBitmap4");
//        CV_Assert(AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0);
//        LOGD("nMatToBitmap5");
//        CV_Assert(pixels);
//        LOGD("nMatToBitmap6");
//        if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
//            Mat tmp(info.height, info.width, CV_8UC4, pixels);
////            Mat tmp(info.height, info.width, CV_8UC3, pixels);
//            if (src.type() == CV_8UC1) {
//                LOGD("nMatToBitmap: CV_8UC1 -> RGBA_8888");
//                cvtColor(src, tmp, COLOR_GRAY2RGBA);
//            } else if (src.type() == CV_8UC3) {
//                LOGD("nMatToBitmap: CV_8UC3 -> RGBA_8888");
////                cvtColor(src, tmp, COLOR_RGB2RGBA);
////                cvtColor(src, tmp, COLOR_RGB2RGBA);
//                cvtColor(src, tmp, COLOR_BGR2RGBA);
////                src.copyTo(tmp);
//            } else if (src.type() == CV_8UC4) {
//                LOGD("nMatToBitmap: CV_8UC4 -> RGBA_8888");
//                if (needPremultiplyAlpha)
//                    cvtColor(src, tmp, COLOR_RGBA2mRGBA);
//                else
//                    src.copyTo(tmp);
//            }
//        } else {
//            // info.format == ANDROID_BITMAP_FORMAT_RGB_565
//            Mat tmp(info.height, info.width, CV_8UC2, pixels);
//            if (src.type() == CV_8UC1) {
//                LOGD("nMatToBitmap: CV_8UC1 -> RGB_565");
//                cvtColor(src, tmp, COLOR_GRAY2BGR565);
//            } else if (src.type() == CV_8UC3) {
//                LOGD("nMatToBitmap: CV_8UC3 -> RGB_565");
////                src.copyTo(tmp);
//                cvtColor(src, tmp, COLOR_RGB2BGR565);
//            } else if (src.type() == CV_8UC4) {
//                LOGD("nMatToBitmap: CV_8UC4 -> RGB_565");
//                cvtColor(src, tmp, COLOR_RGBA2BGR565);
//            }
//        }
//        AndroidBitmap_unlockPixels(env, bitmap);
//        return;
//    } catch (const cv::Exception &e) {
//        AndroidBitmap_unlockPixels(env, bitmap);
//        LOGE("nMatToBitmap catched cv::Exception: %s", e.what());
//        jclass je = env->FindClass("org/opencv/core/CvException");
//        if (!je) je = env->FindClass("java/lang/Exception");
//        env->ThrowNew(je, e.what());
//        return;
//    } catch (...) {
//        AndroidBitmap_unlockPixels(env, bitmap);
//        LOGE("nMatToBitmap catched unknown exception (...)");
//        jclass je = env->FindClass("java/lang/Exception");
//        env->ThrowNew(je, "Unknown exception in JNI code {nMatToBitmap}");
//        return;
//    }
//}
//
//extern "C"
//JNIEXPORT jint JNICALL
//Java_t20220049_sw_1vision_utils_Pano_getBitmap(JNIEnv *env, jclass type, jobject bitmap) {
//    try {
//        if (finalMat.dims != 2) {
//            return -1;
//        }
//        MatToBitmap(env, finalMat, bitmap, false);
//    } catch (Exception &e) {
//        jclass je = env->FindClass("java/lang/Exception");
//        env->ThrowNew(je, e.what());
//    }
//    return 0;
//}
//
//
//extern "C"
//JNIEXPORT jint JNICALL
//Java_t20220049_sw_1vision_ui_CVTestActivity_intTest(JNIEnv *env, jclass clazz, jint num) {
//    return num*num;
//}
//extern "C"
//JNIEXPORT long JNICALL
//Java_t20220049_sw_1vision_ui_CVTestActivity_cameraTest(JNIEnv *env, jclass clazz) {
//    VideoCapture capture(0);
//    Mat* frame = new Mat();
//    capture >> *frame;
////    imshow("hhh", *frame);
//
//    return long(frame);
//}
//

extern "C"
JNIEXPORT void JNICALL
Java_t20220049_sw_1vision_ui_CVActivity_findFeature(JNIEnv *env, jobject thiz, jlong addr_gray,
                                                    jlong addr_rgb) {
    Mat* mGray = (Mat*)addr_gray;
    Mat* mRGB = (Mat*)addr_rgb;

    vector<Point2f> corners;
    goodFeaturesToTrack(*mGray, corners, 20, 0.01, 10, Mat(), 3, false, 0.04);



    for (auto & corner : corners) {
        circle(*mRGB, corner, 10, Scalar(0, 255, 0), 2);
    }
}
extern "C"
JNIEXPORT jlong JNICALL
Java_t20220049_sw_1vision_ui_CVActivity_testLong(JNIEnv *env, jobject thiz) {
    return 114514;
}

bool flag = false;
bool trackFinish = false;
bool started = false;
Mat* trackFrame = nullptr;

void myTracker() {

    Ptr<Tracker> tracker;

    tracker = TrackerMIL::create();
    Rect2i bbox(287, 23, 86, 320);

    rectangle(*trackFrame, bbox, Scalar( 255, 0, 0 ), 2, 1 );
    tracker->init(*trackFrame, bbox);
    trackFrame = nullptr;
    flag = false;

    while (!trackFinish) {
        if (!flag || !trackFrame) continue;
        double timer = (double)getTickCount();
        bool ok = tracker->update(*trackFrame, bbox);

        float fps = getTickFrequency() / ((double)getTickCount() - timer);

        if (ok) {
            rectangle(*trackFrame, bbox, Scalar( 255, 0, 0 ), 2, 1 );
        } else {
            putText(*trackFrame, "Tracking failure detected", Point(100,80), FONT_HERSHEY_SIMPLEX, 0.75, Scalar(0,0,255),2);
        }
        putText(*trackFrame, "MIL Tracker", Point(100,20), FONT_HERSHEY_SIMPLEX, 0.75, Scalar(50,170,50),2);
//    putText(*mRGB, "FPS : " + SSTR(int(fps)), Point(100,50), FONT_HERSHEY_SIMPLEX, 0.75, Scalar(50,170,50), 2);
        trackFrame = nullptr;
        flag = false;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_t20220049_sw_1vision_ui_CVActivity_tracking(JNIEnv *env, jobject thiz, jlong addr_gray,
                                                 jlong addr_rgb) {

    Mat* mGray = (Mat*)addr_gray;
    Mat* mRGB = (Mat*)addr_rgb;
    trackFrame = mRGB;
    flag = true;
    if (!started) {
        myTracker();
    }
}