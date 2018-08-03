#ifndef NATIVE_PLAYER_H
#define NATIVE_PLAYER_H

#include <assert.h>
#include <jni.h>
#include "Mixer.h"
#include "log.h"
#include "RateConverter.h"
#include "WaveReader.h"
#include "fftconvolver.h"
#include <aaudio/AAudio.h>
#include <oboe/AudioStream.h>

#define PLAYBACK_STATE_INITIALIZED 0
#define PLAYBACK_STATE_STOPPED 1
#define PLAYBACK_STATE_PLAYING 2

#define BUFFER_SIZE_AUTOMATIC 0


static std::mutex threadJoinMtx;

static std::mutex threadReadLock;

class NativePlayer : oboe::AudioStreamCallback {

    bool threadGo;

    int playbackState;
    int songSampleRate;
    int loadState;

    bool songReady;

    double time;

    double currentTime = 0; //tempo espresso in centesimi di secondo

    SongSpeed songSpeed;
    SongEqualization songEqu, desiredEqu;
    FFTConvolver fftconvolver[4];

    WaveReader *waveReader = nullptr;

    Mixer *mixer = nullptr;
    RateConverter *rateConverter = nullptr;
    audio::InputStream inLeft;
    audio::InputStream inRight;

    SongType songType;
    std::thread *fastThread = nullptr;

    std::thread *getAudioDataThread = nullptr;

    bool reverse;

    void fastFunction();

    void seek(double timeCentisec);

    void timeUpdate();

    void playbackChange(int type, bool stop);

    void songLoaded();

    void playbackCallback();

    //roba jni
    JavaVM *jvm = nullptr;
    jobject javaobj = 0;
    jmethodID onTimeUpdateMethodID, songSpeedCallbackID, songLoadedCallbackID, playbackStateCallbackID;

    oboe::AudioStream *stream;


    int32_t playbackDeviceId_;
    int32_t bufferSizeSelection_ = BUFFER_SIZE_AUTOMATIC;
    int32_t currentFramesPerBurst;
    int32_t bufSizeInFrames_;

    int currentPlaybackDeviceId;
    oboe::AudioFormat currentSampleFormat;
    int currentSampleChannels;
    int currentSampleRate;

    int32_t framesPerBurst_;

private:


    int32_t playStreamUnderrunCount_;
    std::vector<float> intermediateAudioBuffer;
    //quanto riempire il buffer?
    int intermAudioBufferFillValue;
    //valore arbitrario
    int readThreadSleepTime;

    void closeOutputStream();

    void setupAudioEngineAndPlay(int playbackDeviceId_,
                                 oboe::AudioFormat sampleFormat_,
                                 int sampleChannels_,
                                 int sampleRate_);

    void threadReadData();

public:
    NativePlayer();

    ~NativePlayer();

    void stop();

    void play();

    void setFFTFilters(SongEqualization inputEqu, SongEqualization outputEqu);

    //SongEqualization convertJavaEqualization(const char *equName);
    SongEqualization convertJavaEqualization(JNIEnv *env, jstring javaEqu);

    SongEqualization getSongEqu();

    int getPlaybackState();

    int getCurrentTime();

    void unloadSong();

    void loadSong(JNIEnv *env, jclass clazz, jobjectArray pathsArray, jint songTypeNum,
                  jint songSpeedNum, jstring songEquStr);

    void mixerSetChannelSatellitePosition(int channelNumber, int position);

    void mixerSetChannelEnabled(int channelNumber, int enabled);

    void mixerSetTrackChannel(int trackNumber, int channelNumber);

    jintArray mixerGetTrackMap(JNIEnv *env, jclass clazz);

    int mixerGetChannelSatellitePosition(int channel);

    int mixerGetChannelEnabled(int channel);

    void setSpeed(int speed);

    void speedChange();

    void fastForward();

    void fastReverse();

    float getRatio();

    //cose varie per interfacciarsi con jni
    void setJavaVMObj(JNIEnv *env);

    //javaobj = (jobject) env->NewGlobalRef(obj);
    void setNewGlobalRef(jobject jObject);

    //onTimeUpdateMethodID = env->GetMethodID(cls, "onTimeUpdate", "(D)V");
    void setOnTimeUpdate(jmethodID mID);

    //songSpeedCallbackID = env->GetMethodID(cls, "songSpeedCallback", "()V");
    void setSongSpeedCallback(jmethodID mID);

    //songLoadedCallbackID = env->GetMethodID(cls, "songLoadedCallback", "()V");
    void setSongLoadedCallback(jmethodID mID);

    //playbackStateCallbackID = env->GetMethodID(cls, "playbackStateCallback", "(IZ)V");
    void setPlaybackStateCallback(jmethodID mID);

    oboe::DataCallbackResult
    onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames);

    void errorCallback(oboe::AudioStream *streamCall, oboe::Result error);

    void restartStream();
};

#endif
