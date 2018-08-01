
LOCAL_PATH := $(call my-dir)

include $(LOCAL_PATH)/libsamplerate.mk
include $(LOCAL_PATH)/libsndfile/Android.mk


include $(CLEAR_VARS)
LOCAL_PATH := $(LOCAL_PATH)/..

LOCAL_MODULE    := native-player
LOCAL_SRC_FILES := native-player.cpp
LOCAL_SRC_FILES += Mixer.cpp
LOCAL_SRC_FILES += Reverber.cpp
LOCAL_SRC_FILES += QuadraphonicFilter.cpp
LOCAL_SRC_FILES += audiostreams.cpp
LOCAL_SRC_FILES += filterbase.cpp
LOCAL_SRC_FILES += DelayBuffer.cpp
LOCAL_SRC_FILES += RateConverter.cpp
LOCAL_SRC_FILES += WaveReader.cpp
LOCAL_SRC_FILES += fftconvolver.cpp
LOCAL_SRC_FILES += audiocommon.cpp
LOCAL_SRC_FILES += jnibridge.cpp

# for native audio
#LOCAL_LDLIBS    := -lOpenSLES
LOCAL_LDLIBS    := -laaudio
# for logging
LOCAL_LDLIBS    += -llog
# for native asset manager
LOCAL_LDLIBS    += -landroid

LOCAL_STATIC_LIBRARIES  := libsamplerate
LOCAL_STATIC_LIBRARIES  += libsndfile
LOCAL_STATIC_LIBRARIES  += ckfft
include $(BUILD_SHARED_LIBRARY)

#include $(LOCAL_PATH)/ckfft.mk

#LOCAL_PATH := /media/laura/eclipse/workspace/git/remind_2/remind/Magnetophone/jni/
#LOCAL_PATH :=$(LOCAL_PATH)/..

include $(CLEAR_VARS)

# OpenCV
#OPENCV_CAMERA_MODULES:=on
#OPENCV_INSTALL_MODULES:=on
#OPENCV_LIB_TYPE:=STATIC

#include /media/laura/android-studio/OpenCV-android-sdk/sdk/native/jni/OpenCV.mk

#LOCAL_MODULE    := videoCompute
#LOCAL_SRC_FILES := videoCompute.cpp analyzer.cpp

#LOCAL_LDLIBS := -llog -ljnigraphics -lz -landroid -ldl
#LOCAL_SHARED_LIBRARIES := libavformat libavcodec libswscale libavutil

#include $(BUILD_SHARED_LIBRARY)
include $(LOCAL_PATH)/ckfft.mk
#$(call import-module,ffmpeg-2.0.1/android/arm)
