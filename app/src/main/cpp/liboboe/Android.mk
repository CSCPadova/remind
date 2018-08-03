LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := liboboegit

LOCAL_C_INCLUDES := $(LOCAL_PATH)

LOCAL_SRC_FILES:= aaudio/AAudioLoader.cpp\
        aaudio/AudioStreamAAudio.cpp\
        common/LatencyTuner.cpp\
        common/AudioStream.cpp\
        common/AudioStreamBuilder.cpp\
        common/Utilities.cpp\
        fifo/FifoBuffer.cpp\
        fifo/FifoController.cpp\
        fifo/FifoControllerBase.cpp\
        fifo/FifoControllerIndirect.cpp\
        opensles/AudioInputStreamOpenSLES.cpp\
        opensles/AudioOutputStreamOpenSLES.cpp\
        opensles/AudioStreamBuffered.cpp\
        opensles/AudioStreamOpenSLES.cpp\
        opensles/EngineOpenSLES.cpp\
        opensles/OpenSLESUtilities.cpp\
        opensles/OutputMixerOpenSLES.cpp

LOCAL_LDLIBS += -llog -lOpenSLES

include $(BUILD_STATIC_LIBRARY)

