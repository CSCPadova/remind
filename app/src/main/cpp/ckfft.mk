LOCAL_PATH := $(call my-dir)

#################################################

include $(CLEAR_VARS)

LOCAL_MODULE := ckfft

LOCAL_SRC_FILES := \
    ckfft/ckfft.cpp \
    ckfft/context.cpp \
    ckfft/debug.cpp \
    ckfft/fft.cpp \
    ckfft/fft_default.cpp \
    ckfft/fft_real.cpp \
    ckfft/fft_real_default.cpp 

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
LOCAL_SRC_FILES += \
    ckfft/fft_neon.cpp.neon \
    ckfft/fft_real_neon.cpp.neon
else
LOCAL_SRC_FILES += \
    ckfft/fft_neon.cpp \
    ckfft/fft_real_neon.cpp
endif
LOCAL_C_INCLUDES := $(LOCAL_PATH)/ckfft
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/ckfft
LOCAL_STATIC_LIBRARIES := cpufeatures

include $(BUILD_STATIC_LIBRARY)

$(call import-module,android/cpufeatures)



