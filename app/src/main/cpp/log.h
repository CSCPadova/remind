#ifndef LOG_H
#define LOG_H
#include <android/log.h>

#define LOG_TAG "Magneto NDK"
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#endif
