LOCAL_PATH := $(call my-dir)

OBOE_PATH :=$(LOCAL_PATH)
LIB_PATH :=$(LOCAL_PATH)
include $(OBOE_PATH)/liboboe/Android.mk

include $(LIB_PATH)/libsamplerate.mk
include $(LIB_PATH)/libsndfile/Android.mk

include $(CLEAR_VARS)
LOCAL_PATH := $(LOCAL_PATH)/..

LOCAL_MODULE    := native-player

LOCAL_SRC_FILES := native-player.cpp
LOCAL_SRC_FILES += Mixer.cpp
LOCAL_SRC_FILES += audiostreams.cpp
LOCAL_SRC_FILES += filterbase.cpp
LOCAL_SRC_FILES += RateConverter.cpp
LOCAL_SRC_FILES += WaveReader.cpp
LOCAL_SRC_FILES += fftconvolver.cpp
LOCAL_SRC_FILES += audiocommon.cpp
LOCAL_SRC_FILES += jnibridge.cpp

#Classi tolte definitivamente dalla compilazione ma i file sono ancora presenti nel progetto
#LOCAL_SRC_FILES += Reverber.cpp
#LOCAL_SRC_FILES += QuadraphonicFilter.cpp
#LOCAL_SRC_FILES += DelayBuffer.cpp

# for native audio
LOCAL_LDLIBS    := -lOpenSLES
# for logging
LOCAL_LDLIBS    += -llog
# for native asset manager
LOCAL_LDLIBS    += -landroid

LOCAL_STATIC_LIBRARIES  := libsamplerate
LOCAL_STATIC_LIBRARIES  += libsndfile
LOCAL_STATIC_LIBRARIES  += ckfft
LOCAL_STATIC_LIBRARIES  += liboboegit

include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)

include $(LOCAL_PATH)/ckfft.mk
