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

#define PLAYBACK_STATE_INITIALIZED 0
#define PLAYBACK_STATE_STOPPED 1
#define PLAYBACK_STATE_PLAYING 2

#define BUFFER_SIZE_AUTOMATIC 0


static std::mutex threadJoinMtx;

class NativePlayer {

/*
 * TODO
 * Niente di quello che c'è in questo file appartiene a un header;
 * da spostare tutto in native-player.cpp
 * (non lo ho fatto io perchè voglio spostare il meno possibile;
 * è già un miracolo che questa cosa funzioni).
 * -Marco
 */

	int playbackState;
	int songSampleRate;
	int loadState;

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


    //AAUDIO
    AAudioStream *playStream_;

    aaudio_format_t sampleFormat_;

    int16_t sampleChannels_;
    int32_t playbackDeviceId_ = AAUDIO_UNSPECIFIED;
    int32_t bufferSizeSelection_ = BUFFER_SIZE_AUTOMATIC;
    int32_t currentFramesPerBurst;
    int32_t bufSizeInFrames_;

	int currentPlaybackDeviceId;
	int currentSampleFormat;
	int currentSampleChannels;
	int currentSampleRate;

    int32_t framesPerBurst_;
    int32_t playStreamUnderrunCount_;

    AAudioStreamBuilder* createStreamBuilder();

    std::mutex restartingLock_;

private:

    std::vector<float> intermediateAudioBuffer;
    //quanto riempire il buffer?
    int intermAudioBufferFillValue;
    bool fillInterAudioBuffer;
    //valore arbitrario
    int readThreadSleepTime;

    void setupPlaybackStreamParameters(AAudioStreamBuilder *builder,
                                       int playbackDeviceId_,
                                       int sampleFormat_,
                                       int sampleChannels_,
                                       int sampleRate_);


    void threadFunction(double timeCentisec);

	void closeOutputStream();

	void setupAudioEngineAndPlay(int playbackDeviceId_,
								 int sampleFormat_,
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
    SongEqualization convertJavaEqualization(JNIEnv* env, jstring javaEqu);
    SongEqualization getSongEqu();

    int getPlaybackState();
    int getCurrentTime();
    void unloadSong();
    void loadSong(JNIEnv* env, jclass clazz, jobjectArray pathsArray, jint songTypeNum, jint songSpeedNum, jstring songEquStr);
    void mixerSetChannelSatellitePosition(int channelNumber, int position);
    void mixerSetChannelEnabled(int channelNumber, int enabled);
    void mixerSetTrackChannel(int trackNumber, int channelNumber);
    jintArray mixerGetTrackMap(JNIEnv* env, jclass clazz);
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

    //AAUDIO
    //setupAudioEngineAndPlay chiamera' setupPlaybackStreamParameters


    aaudio_data_callback_result_t dataCallback(AAudioStream *stream,
                                               void *audioData,
                                               int32_t numFrames);

    void errorCallback(AAudioStream *stream, aaudio_result_t error);
    void restartStream();
};

#endif
