#ifndef NATIVE_PLAYER_H
#define NATIVE_PLAYER_H

#include <assert.h>
#include <jni.h>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include "Mixer.h"
#include "log.h"
#include "RateConverter.h"
#include "WaveReader.h"

#define PLAYBACK_STATE_INITIALIZED 0
#define PLAYBACK_STATE_STOPPED 1
#define PLAYBACK_STATE_PLAYING 2

/*
 * TODO
 * Niente di quello che c'è in questo file appartiene a un header;
 * da spostare tutto in native-player.cpp
 * (non lo ho fatto io perchè voglio spostare il meno possibile;
 * è già un miracolo che questa cosa funzioni).
 * -Marco
 */

extern "C"
{
	// engine interfaces
	SLObjectItf engineObject = NULL;
	SLEngineItf engineItf;

	SLObjectItf player_obj;
	SLPlayItf player;
	SLmilliHertz recorderSR;
	SLObjectItf output_mix_obj;
	SLPlaybackRateItf rateItf;

}
int playbackState;
int songSampleRate;
int loadState;

SLAndroidSimpleBufferQueueItf player_buf_q;
//WavReader *waveReaders[4];
WaveReader * waveReader = nullptr;
char paths[4][200];
short bufferLettura[4][2000];
short * bufferLetturaPtrs[4] = {nullptr};
int bufcount = 0;

double currentTime = 0; //tempo espresso in centesimi di secondo

jmethodID onTimeUpdateMethodID, songSpeedCallbackID, songLoadedCallbackID, playbackStateCallbackID;

jobject javaobj = 0;
JavaVM * jvm = nullptr;
JNIEnv *current_jni_env = nullptr;
Mixer * mixer = nullptr;
RateConverter * rateConverter = nullptr;
audio::InputStream inLeft;
audio::InputStream inRight;

SongType songType;
std::thread * fastThread = nullptr;
bool reverse;
void fastFunction();
void stop();
void play();
void seek(double timeCentisec);

void timeUpdate(SLPlayItf caller, void * context, SLuint32 event);
void playbackChange(int type, bool stop);
#endif
