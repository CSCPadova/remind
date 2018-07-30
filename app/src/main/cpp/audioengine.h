#ifndef AUDIOTEST_AUDIOENGINE_H
#define AUDIOTEST_AUDIOENGINE_H

#include <thread>
#include <stdint.h>
#include "audiocommon.h"
#include <vector>
#include <string>
#include <android/asset_manager.h>

#define BUFFER_SIZE_AUTOMATIC 0

class AudioEngine {

public:
    AudioEngine();

    ~AudioEngine();
    aaudio_data_callback_result_t dataCallback(AAudioStream *stream,
                                               void *audioData,
                                               int32_t numFrames);

    void errorCallback(AAudioStream *stream,
                       aaudio_result_t  __unused error);

    void setBufferSizeInBursts(int32_t numBursts);

    void stop();

    void play();

private:

    int32_t playbackDeviceId_ = AAUDIO_UNSPECIFIED;
    int32_t sampleRate_;
    int32_t mySampleRate_;
    int16_t sampleChannels_;
    aaudio_format_t sampleFormat_;

    int32_t framesPerBurst_;
    int32_t bufSizeInFrames_;
    int32_t playStreamUnderrunCount_;
    int32_t bufferSizeSelection_ = BUFFER_SIZE_AUTOMATIC;

    AAudioStream *playStream_;

    double currentOutputLatencyMillis_ = 0;


private:

    std::mutex restartingLock_;

    void createPlaybackStream();
    void closeOutputStream();

    AAudioStreamBuilder* createStreamBuilder();
    void setupPlaybackStreamParameters(AAudioStreamBuilder *builder);
    void restartStream();


    aaudio_result_t calculateCurrentOutputLatencyMillis(AAudioStream *stream, double *latencyMillis);


};

#endif //AUDIOTEST_AUDIOENGINE_H
