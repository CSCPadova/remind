//
// Created by riccardo on 21/07/18.
//

#include <string>
#include "audiocommon.h"
#include "log.h"

static const int32_t audioFormatEnum[] = {
        AAUDIO_FORMAT_INVALID,
        AAUDIO_FORMAT_UNSPECIFIED,
        AAUDIO_FORMAT_PCM_I16,
        AAUDIO_FORMAT_PCM_FLOAT,
};

static const int32_t audioFormatCount = sizeof(audioFormatEnum)/
                                        sizeof(audioFormatEnum[0]);

static const char * audioFormatStr[] = {
        "AAUDIO_FORMAT_INVALID", // = -1,
        "AAUDIO_FORMAT_UNSPECIFIED", // = 0,
        "AAUDIO_FORMAT_PCM_I16",
        "AAUDIO_FORMAT_PCM_FLOAT",
};

void PrintAudioStreamInfo(const AAudioStream * stream) {
#define STREAM_CALL(c) AAudioStream_##c((AAudioStream*)stream)
    LOGD("StreamID: %p", stream);

    LOGD("BufferCapacity: %d", STREAM_CALL(getBufferCapacityInFrames));
    LOGD("BufferSize: %d", STREAM_CALL(getBufferSizeInFrames));
    LOGD("FramesPerBurst: %d", STREAM_CALL(getFramesPerBurst));
    LOGD("XRunCount: %d", STREAM_CALL(getXRunCount));
    LOGD("SampleRate: %d", STREAM_CALL(getSampleRate));
    LOGD("SamplesPerFrame: %d", STREAM_CALL(getChannelCount));
    LOGD("DeviceId: %d", STREAM_CALL(getDeviceId));
    LOGD("Format: %s",  FormatToString(STREAM_CALL(getFormat)));
    LOGD("SharingMode: %s", (STREAM_CALL(getSharingMode)) == AAUDIO_SHARING_MODE_EXCLUSIVE ?
                            "EXCLUSIVE" : "SHARED");

    aaudio_performance_mode_t perfMode = STREAM_CALL(getPerformanceMode);
    std::string perfModeDescription;
    switch (perfMode){
        case AAUDIO_PERFORMANCE_MODE_NONE:
            perfModeDescription = "NONE";
            break;
        case AAUDIO_PERFORMANCE_MODE_LOW_LATENCY:
            perfModeDescription = "LOW_LATENCY";
            break;
        case AAUDIO_PERFORMANCE_MODE_POWER_SAVING:
            perfModeDescription = "POWER_SAVING";
            break;
        default:
            perfModeDescription = "UNKNOWN";
            break;
    }
    LOGD("PerformanceMode: %s", perfModeDescription.c_str());

    aaudio_direction_t  dir = STREAM_CALL(getDirection);
    LOGD("Direction: %s", (dir == AAUDIO_DIRECTION_OUTPUT ? "OUTPUT" : "INPUT"));
    if (dir == AAUDIO_DIRECTION_OUTPUT) {
        LOGD("FramesReadByDevice: %d", (int32_t)STREAM_CALL(getFramesRead));
        LOGD("FramesWriteByApp: %d", (int32_t)STREAM_CALL(getFramesWritten));
    } else {
        LOGD("FramesReadByApp: %d", (int32_t)STREAM_CALL(getFramesRead));
        LOGD("FramesWriteByDevice: %d", (int32_t)STREAM_CALL(getFramesWritten));
    }
#undef STREAM_CALL
}

const char* FormatToString(aaudio_format_t format) {
    for (int32_t i = 0; i < audioFormatCount; ++i) {
        if (audioFormatEnum[i] == format)
            return audioFormatStr[i];
    }
    return "UNKNOW_AUDIO_FORMAT";
}

int64_t timestamp_to_nanoseconds(timespec ts){
    return (ts.tv_sec * (int64_t) NANOS_PER_SECOND) + ts.tv_nsec;
}

int64_t get_time_nanoseconds(clockid_t clockid){
    timespec ts;
    clock_gettime(clockid, &ts);
    return timestamp_to_nanoseconds(ts);
}