#include <android/asset_manager_jni.h>
#include <android/native_window_jni.h>
#include <android/native_window.h>

#include <android/log.h>

#include <jni.h>

#include <string>
#include <vector>

#include <platform.h>
#include <benchmark.h>
#include <ctime>

#include "Face_detection/face.h"

#include "Face_detection/ndkcamera.h"

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#if __ARM_NEON
#include <arm_neon.h>
#endif // __ARM_NEON

using namespace std::chrono;

std::vector<Object> faceobjects;

float distance(cv::Point2f p1, cv::Point2f p2) {
    float res = std::abs(sqrt(pow(pow(p2.x - p1.x, 2) + pow(p2.y - p1.y, 2), 1.0)));
    return res;
}

static void draw_fps(cv::Mat& rgb)
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

    cv::rectangle(rgb, cv::Rect(cv::Point(x, y), cv::Size(label_size.width, label_size.height + baseLine)),
                  cv::Scalar(255, 255, 255), -1);

    cv::putText(rgb, text, cv::Point(x, y + label_size.height),
                cv::FONT_HERSHEY_SIMPLEX, 0.5, cv::Scalar(0, 0, 0));
}

static Face *g_blazeface = 0;
static ncnn::Mutex lock;

int faceAlert = 0;
float rectAV = 0.0f;
time_t timeSecStart = 0;

//Class for DETECTOR
class MyNdkCamera : public NdkCameraWindow {
private:
    float threshold = 0.15f;

public:
    void on_image_render(cv::Mat &rgb) const override;
};

bool isdraw = true;

void MyNdkCamera::on_image_render(cv::Mat &rgb) const {
    {
        ncnn::MutexLockGuard g(lock);

        if (g_blazeface) {
//            high_resolution_clock::time_point t1 = high_resolution_clock::now();
            g_blazeface->detect(rgb, faceobjects, rectAV);
//            high_resolution_clock::time_point t2 = high_resolution_clock::now();

            if (faceobjects.size() > 0 && isdraw) {
//                duration<double, std::milli> time_span = t2-t1;
//                __android_log_print(ANDROID_LOG_DEBUG, "TimeFace","MiliSegundo: %f", time_span.count());
                g_blazeface->draw(rgb, faceobjects, true);
            }

//            draw_alert(rgb);
        } else {
//            draw_unsupported(rgb);
        }
    }
    draw_fps(rgb);
}

//Class for DETECTOR
class CameraCalibration : public NdkCameraWindow {
public:
    virtual void on_image_render(cv::Mat &rgb) const;
};

double ear = 0;
double mar = 0;
double area = 0;

void CameraCalibration::on_image_render(cv::Mat &rgb) const {
    {
        ncnn::MutexLockGuard g(lock);

        if (g_blazeface) {
            g_blazeface->detect(rgb, faceobjects, 0.1f);

            if (faceobjects.size() > 0 && isdraw) {
//                duration<double, std::milli> time_span = t2-t1;
//                __android_log_print(ANDROID_LOG_DEBUG, "TimeFace","MiliSegundo: %f", time_span.count());
                ear = (faceobjects[0].earleft + faceobjects[0].earright) / 2;
                area = faceobjects[0].rect.area();
                mar = faceobjects[0].mar;
                g_blazeface->draw(rgb, faceobjects, true);
            }
        } else {
//            draw_unsupported(rgb);
        }
    }
    draw_fps(rgb);
}

static MyNdkCamera *g_camera = 0;
static CameraCalibration *c_camera = 0;

extern "C" {
JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "JNI_OnLoad");

//        g_camera = new MyNdkCamera;

    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "JNI_OnUnload");

    {
        ncnn::MutexLockGuard g(lock);

        delete g_blazeface;
        g_blazeface = 0;
    }

    delete g_camera;
    g_camera = 0;

    delete c_camera;
    c_camera = 0;
}
}

float earleft;
float earright;

void calculate() {
    float le1 = distance(faceobjects[0].left_eyes[11], faceobjects[0].left_eyes[3]);
    float le2 = distance(faceobjects[0].left_eyes[13], faceobjects[0].left_eyes[5]);
    float leS = distance(faceobjects[0].left_eyes[0], faceobjects[0].left_eyes[8]);
    earleft = (le1 + le2) / (leS * 2);

    float re1 = distance(faceobjects[0].right_eyes[11], faceobjects[0].right_eyes[3]);
    float re2 = distance(faceobjects[0].right_eyes[13], faceobjects[0].right_eyes[5]);
    float reS = distance(faceobjects[0].right_eyes[8], faceobjects[0].right_eyes[0]);
    earright = (re1 + re2) / (reS * 2);

    ear = (earright + earleft) / 2;

    float m1 = distance(faceobjects[0].skeleton[317], (cv::Point2f) faceobjects[0].skeleton[312]);
    float m2 = distance((cv::Point2f) faceobjects[0].skeleton[87],
                        (cv::Point2f) faceobjects[0].skeleton[82]);
    float ms = distance((cv::Point2f) faceobjects[0].skeleton[78],
                        (cv::Point2f) faceobjects[0].skeleton[415]);
    mar = (m1 + m2) / (ms * 2);
}

extern "C" {
//    SECTION DETAILS:
//    This section contains the camera function of MAIN CAMERA

// public native boolean loadModel(AssetManager mgr, int modelid, int cpugpu);
JNIEXPORT jboolean JNICALL
Java_com_vyw_tflite_algorithm_BlazeFaceNcnn_loadModel(JNIEnv *env, jobject thiz,
                                                      jobject assetManager, jint modelid,
                                                      jint cpugpu, jboolean draw) {
    isdraw = draw;
    if (modelid < 0 || modelid > 6 || cpugpu < 0 || cpugpu > 1) {
        return JNI_FALSE;
    }

    AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "loadModel %p", mgr);
    const char *modeltypes[] =
            {
                    "blazeface",
                    "blazeface",
                    "blazeface"
            };
    const int target_sizes[] =
            {
                    192,
                    320,
                    640
            };
    const char *modeltype = modeltypes[(int) modelid];
    int target_size = target_sizes[(int) modelid];
    bool use_gpu = (int) cpugpu == 1;

    char modelFinal[256];
    sprintf(modelFinal, "Models/%s", modeltype);
    __android_log_print(ANDROID_LOG_DEBUG, "ncnnModel", "loadModel %s", modelFinal);
    // reload
    {
        ncnn::MutexLockGuard g(lock);

        if (use_gpu && ncnn::get_gpu_count() == 0) {
            // no gpu
            delete g_blazeface;
            g_blazeface = 0;
        } else {
            if (!g_blazeface)
                g_blazeface = new Face;
            g_blazeface->load(mgr, modelFinal, target_size, use_gpu);
        }
    }

    return JNI_TRUE;
}

// public native boolean openCamera(int facing);
JNIEXPORT jboolean JNICALL
Java_com_vyw_tflite_algorithm_BlazeFaceNcnn_openCamera(JNIEnv *env, jobject thiz, jint facing) {
    if (facing < 0 || facing > 1)
        return JNI_FALSE;

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "openCamera %d", facing);

    g_camera->open((int) facing);

    return JNI_TRUE;
}

// public native boolean closeCamera();
JNIEXPORT jboolean JNICALL
Java_com_vyw_tflite_algorithm_BlazeFaceNcnn_closeCamera(JNIEnv *env, jobject thiz) {
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "closeCamera");

    g_camera->close();

    return JNI_FALSE;
}

// public native boolean setOutputWindow(Surface surface);
JNIEXPORT jboolean JNICALL
Java_com_vyw_tflite_algorithm_BlazeFaceNcnn_setOutputWindow(JNIEnv *env, jobject thiz,
                                                            jobject surface) {
    ANativeWindow *win = ANativeWindow_fromSurface(env, surface);

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "setOutputWindow %p", win);

    g_camera->set_window(win);

    return JNI_TRUE;
}
JNIEXPORT jfloatArray JNICALL
Java_com_vyw_tflite_algorithm_BlazeFaceNcnn_data(JNIEnv *env, jobject thiz) {
    jfloatArray result;

    int length = 3;
    result = env->NewFloatArray(length);
    if (result == NULL) {
        return NULL;
    }

    if (!faceobjects.empty()) {
        calculate();
        jfloat arrayData[length];
        arrayData[0] = ear;
        arrayData[1] = mar;
        arrayData[2] = 0.0;

        env->SetFloatArrayRegion(result, 0, length, arrayData);
        return result;
    } else {
        return result;
    }
}

JNIEXPORT void JNICALL
Java_com_vyw_tflite_algorithm_BlazeFaceNcnn_initiateCamera(JNIEnv *env, jobject thiz) {
    g_camera = new MyNdkCamera();
    __android_log_print(ANDROID_LOG_DEBUG, "reactangleArea", "AVG rect = %f", rectAV);
}
}

extern "C" {
// SECTION DETAILS:
// This section contains the camera function of CALIBRATE CAMERA

// public native boolean loadModel(AssetManager mgr, int modelid, int cpugpu);
JNIEXPORT jboolean JNICALL
Java_com_vyw_tflite_algorithm_BlazeFaceNcnnCalibrate_loadModel(JNIEnv *env, jobject thiz,
                                                               jobject assetManager, jint modelid,
                                                               jint cpugpu) {
    if (modelid < 0 || modelid > 6 || cpugpu < 0 || cpugpu > 1) {
        return JNI_FALSE;
    }

    AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "loadModel %p", mgr);
    const char *modeltypes[] =
            {
                    "blazeface",
                    "blazeface",
                    "blazeface"
            };
    const int target_sizes[] =
            {
                    192,
                    320,
                    640
            };
    const char *modeltype = modeltypes[(int) modelid];
    int target_size = target_sizes[(int) modelid];
    bool use_gpu = (int) cpugpu == 1;

    char modelFinal[256];
    sprintf(modelFinal, "Models/%s", modeltype);
    __android_log_print(ANDROID_LOG_DEBUG, "ncnnModel", "loadModel %s", modelFinal);
    // reload
    {
        ncnn::MutexLockGuard g(lock);

        if (use_gpu && ncnn::get_gpu_count() == 0) {
            // no gpu
            delete g_blazeface;
            g_blazeface = 0;
        } else {
            if (!g_blazeface)
                g_blazeface = new Face;
            g_blazeface->load(mgr, modelFinal, target_size, use_gpu);
        }
    }

    return JNI_TRUE;
}

// public native boolean openCamera(int facing);
JNIEXPORT jboolean JNICALL
Java_com_vyw_tflite_algorithm_BlazeFaceNcnnCalibrate_openCamera(JNIEnv *env, jobject thiz,
                                                                jint facing) {
    if (facing < 0 || facing > 1)
        return JNI_FALSE;

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "openCamera %d", facing);

    c_camera->open((int) facing);

    return JNI_TRUE;
}

// public native boolean closeCamera();
JNIEXPORT jboolean JNICALL
Java_com_vyw_tflite_algorithm_BlazeFaceNcnnCalibrate_closeCamera(JNIEnv *env, jobject thiz) {
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "closeCamera");

    c_camera->close();

    return JNI_FALSE;
}

// public native boolean setOutputWindow(Surface surface);
JNIEXPORT jboolean JNICALL
Java_com_vyw_tflite_algorithm_BlazeFaceNcnnCalibrate_setOutputWindow(JNIEnv *env, jobject thiz,
                                                                     jobject surface) {
    ANativeWindow *win = ANativeWindow_fromSurface(env, surface);

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "setOutputWindow %p", win);

    c_camera->set_window(win);

    return JNI_TRUE;
}

JNIEXPORT jdoubleArray JNICALL
Java_com_vyw_tflite_algorithm_BlazeFaceNcnnCalibrate_calibrateFeature(JNIEnv *env, jobject thiz) {
    jdoubleArray result;

    int length = 4;
    result = env->NewDoubleArray(length);
    if (result == NULL) {
        return NULL;
    }

    if (!faceobjects.empty()) {
        calculate();
        jdouble arrayData[length];
        arrayData[0] = ear;
        arrayData[1] = mar;
        arrayData[2] = 0.0;
        arrayData[3] = area;

        env->SetDoubleArrayRegion(result, 0, length, arrayData);
        return result;
    } else {
        jdouble arrayData[length];
        arrayData[0] = 0;
        arrayData[1] = 0;
        arrayData[2] = 0;
        arrayData[3] = 0;
        env->SetDoubleArrayRegion(result, 0, length, arrayData);
        return result;
    }
}

JNIEXPORT void JNICALL
Java_com_vyw_tflite_algorithm_BlazeFaceNcnnCalibrate_initiateCamera(JNIEnv *env, jobject thiz) {
    c_camera = new CameraCalibration;
}
}