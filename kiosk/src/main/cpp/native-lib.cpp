#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "Llama2Genie"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ⚠️ 이 파일에는 JNI 함수 없음. 전부 LlmClient.cpp 에만 존재.
