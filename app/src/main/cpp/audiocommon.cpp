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

void PrintAudioStreamInfo(oboe::AudioStream * stream) {
    LOGD("StreamID: %p", stream);

    LOGD("BufferCapacity: %d", stream->getBufferSizeInFrames());
    LOGD("BufferSize: %d", stream->getBufferSizeInFrames());
    LOGD("FramesPerBurst: %d", stream->getFramesPerBurst());
    LOGD("XRunCount: %d", stream->getXRunCount().value());
    LOGD("SampleRate: %d", stream->getSampleRate());
    LOGD("SamplesPerFrame: %d", stream->getChannelCount());
    LOGD("DeviceId: %d", stream->getDeviceId());


    for (int32_t i = 0; i < audioFormatCount; ++i) {
        if (audioFormatEnum[i] == (int)stream->getFormat())
            LOGD("Format: %s", audioFormatStr[i]);
    }

    LOGD("SharingMode: %s", stream->getSharingMode() == oboe::SharingMode::Exclusive ?
                            "EXCLUSIVE" : "SHARED");

    oboe::PerformanceMode perfMode = stream->getPerformanceMode();
    std::string perfModeDescription;
    switch (perfMode){
        case oboe::PerformanceMode::None :
            perfModeDescription = "NONE";
            break;
        case oboe::PerformanceMode::LowLatency:
            perfModeDescription = "LOW_LATENCY";
            break;
        case oboe::PerformanceMode::PowerSaving:
            perfModeDescription = "POWER_SAVING";
            break;
        default:
            perfModeDescription = "UNKNOWN";
            break;
    }
    LOGD("PerformanceMode: %s", perfModeDescription.c_str());

    oboe::Direction  dir = stream->getDirection();
    LOGD("Direction: %s", (dir == oboe::Direction::Output ? "OUTPUT" : "INPUT"));
    if (dir == oboe::Direction::Output) {
        LOGD("FramesReadByDevice: %d",stream->getFramesRead());
        LOGD("FramesWriteByApp: %d", stream->getFramesWritten());
    } else {
        LOGD("FramesReadByApp: %d", stream->getFramesRead());
        LOGD("FramesWriteByDevice: %d", stream->getFramesWritten());
    }
}

int64_t timestamp_to_nanoseconds(timespec ts){
    return (ts.tv_sec * (int64_t) NANOS_PER_SECOND) + ts.tv_nsec;
}

int64_t get_time_nanoseconds(clockid_t clockid){
    timespec ts;
    clock_gettime(clockid, &ts);
    return timestamp_to_nanoseconds(ts);
}