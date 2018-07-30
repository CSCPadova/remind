//
// Created by riccardo on 21/07/18.
//

#ifndef AUDIOTEST_AUDIO_COMMON_H
#define AUDIOTEST_AUDIO_COMMON_H

#include <aaudio/AAudio.h>

// Time constants
#define NANOS_PER_SECOND 1000000000L
#define NANOS_PER_MILLISECOND 1000000L

constexpr int kStereoChannelCount = 2;

void PrintAudioStreamInfo(const AAudioStream * stream);

const char * FormatToString(aaudio_format_t format);

int64_t get_time_nanoseconds(clockid_t clockid);

#endif //AUDIOTEST_AUDIO_COMMON_H
