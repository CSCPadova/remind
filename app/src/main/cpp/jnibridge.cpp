//
// Created by riccardo on 31/07/18.
//

#include <jni.h>
#include "native-player.h"

static NativePlayer *engine = nullptr;

//without this you will get "java.lang.UnsatisfiedLinkError: No implementation found for..."
extern "C"
{

JavaVM *jvm = nullptr;

void
Java_unipd_dei_megnetophone_NativePlayer_convertJavaEqualization(JNIEnv *env, jstring javaEqu) {
    if (engine == nullptr) {
        LOGE("Engine is null, you must call createEngine before calling this method");
        return;
    }

    engine->convertJavaEqualization(env, javaEqu);
}

JNIEXPORT void JNICALL Java_unipd_dei_magnetophone_MusicService_init(JNIEnv *env, jobject obj) {
    //slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);

    //(*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    //(*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineItf);

    engine = new NativePlayer();

    env->GetJavaVM(&jvm);

    jclass cls = env->GetObjectClass(obj);
    //javaobj = (jobject) env->NewGlobalRef(obj);
    engine->setNewGlobalRef((jobject) env->NewGlobalRef(obj));

    //onTimeUpdateMethodID = env->GetMethodID(cls, "onTimeUpdate", "(D)V");
    engine->setOnTimeUpdate(env->GetMethodID(cls, "onTimeUpdate", "(D)V"));

    //songSpeedCallbackID = env->GetMethodID(cls, "songSpeedCallback", "()V");
    engine->setSongSpeedCallback(env->GetMethodID(cls, "songSpeedCallback", "()V"));

    //songLoadedCallbackID = env->GetMethodID(cls, "songLoadedCallback", "()V");
    engine->setSongLoadedCallback(env->GetMethodID(cls, "songLoadedCallback", "()V"));

    //playbackStateCallbackID = env->GetMethodID(cls, "playbackStateCallback", "(IZ)V");
    engine->setPlaybackStateCallback(env->GetMethodID(cls, "playbackStateCallback", "(IZ)V"));

    if (engine == nullptr) {
        LOGE("Engine is null, you must call createEngine before calling this method");
        return;
    }
    //TODO da fare
    //playbackState = PLAYBACK_STATE_INITIALIZED;
    //engine->init();
}

// create the engine
void setupAudioEngine(int sampleRate, int bitPerSample) {
    //TODO da fare
    //engine->createPlaybackStream();

    ////create the Engine object
    //(*engineItf)->CreateOutputMix(engineItf, &output_mix_obj, 0, NULL, NULL);
    //(*output_mix_obj)->Realize(output_mix_obj, SL_BOOLEAN_FALSE);
    //// configure audio source
    //SLDataLocator_AndroidSimpleBufferQueue loc_bq = { SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2 };
//
    //SLuint32 slSampleRate;
    //switch (sampleRate)
    //{
    //    case 96000:
    //        slSampleRate = SL_SAMPLINGRATE_96; //!!! non supportata
    //        break;
    //    case 48000:
    //        slSampleRate = SL_SAMPLINGRATE_48;
    //        break;
    //    case 22050:
    //        slSampleRate = SL_SAMPLINGRATE_22_05;
    //        break;
    //    case 24000:
    //        slSampleRate = SL_SAMPLINGRATE_24;
    //        break;
    //    case 32000:
    //        slSampleRate = SL_SAMPLINGRATE_32;
    //        break;
    //    case 16000:
    //        slSampleRate = SL_SAMPLINGRATE_16;
    //        break;
    //    case 8000:
    //        slSampleRate = SL_SAMPLINGRATE_16;
    //        break;
    //    default:
    //        slSampleRate = SL_SAMPLINGRATE_44_1;
    //        break;
    //}
//
    //SLuint32 slPcmFormat;
    //switch (bitPerSample)
    //{
    //    case SF_FORMAT_PCM_S8:
    //        slPcmFormat = SL_PCMSAMPLEFORMAT_FIXED_8;
    //        break;
    //    case SF_FORMAT_PCM_24:
    //        slPcmFormat = SL_PCMSAMPLEFORMAT_FIXED_24;
    //        break;
    //    case SF_FORMAT_PCM_32:
    //        slPcmFormat = SL_PCMSAMPLEFORMAT_FIXED_32;
    //        break;
    //    default:
    //        slPcmFormat = SL_PCMSAMPLEFORMAT_FIXED_16;
    //        break;
    //}
//
    //SLDataFormat_PCM format_pcm = { SL_DATAFORMAT_PCM, 2, slSampleRate, slPcmFormat, slPcmFormat, 0, SL_BYTEORDER_LITTLEENDIAN };
    //SLDataSource audioSrc = { &loc_bq, &format_pcm };
    //// configure audio sink
    //SLDataLocator_OutputMix loc_outmix = { SL_DATALOCATOR_OUTPUTMIX, output_mix_obj };
    //SLDataSink audioSnk = { &loc_outmix, NULL };
    ////create the object
    //const SLInterfaceID ids[] = { SL_IID_BUFFERQUEUE };
    //const SLboolean req[] = { SL_BOOLEAN_TRUE };
    //(*engineItf)->CreateAudioPlayer(engineItf, &player_obj, &audioSrc, &audioSnk, 1, ids, req);
    //(*player_obj)->Realize(player_obj, SL_BOOLEAN_FALSE);
    //(*player_obj)->GetInterface(player_obj, SL_IID_PLAY, &player);
//
    //(*player_obj)->GetInterface(player_obj, SL_IID_BUFFERQUEUE, &player_buf_q);
//
    //(*player)->SetPositionUpdatePeriod(player, 100);
    ////assert(SL_RESULT_SUCCESS == result);
//
    //(*player)->RegisterCallback(player, timeUpdate, NULL);
    ////assert(SL_RESULT_SUCCESS == result);
//
    //// register callback on the buffer queue
    //(*player_buf_q)->RegisterCallback(player_buf_q, playbackCallback, NULL);
//
    //currentTime = 0;
//
    //LOGD("creato audio engine");
}

void releaseAudioEngine() {
    if (engine == nullptr) {
        LOGE("Engine is null, you must call createEngine before calling this method");
        return;
    }
    //TODO
    delete engine;
}

JNIEXPORT void JNICALL
Java_unipd_dei_magnetophone_MusicService_terminate(JNIEnv *env, jclass clazz) {
    //TODO da fare
    if (engine == nullptr) {
        LOGE("Engine is null, you must call createEngine before calling this method");
        return;
    }
    if (engine->getPlaybackState() != PLAYBACK_STATE_INITIALIZED)
        return;

    engine->closeOutputStream();
    delete engine;
    engine = nullptr;
    //(*engineObject)->Destroy(engineObject);
    LOGD("terminate");
}

JNIEXPORT jint JNICALL
Java_unipd_dei_magnetophone_MusicService_getPlaybackState(JNIEnv *env, jclass clazz) {
    if (engine == nullptr) {
        LOGE("Engine is null, you must call createEngine before calling this method");
        return -1;
    }
    return engine->getPlaybackState();
}

JNIEXPORT jint JNICALL Java_unipd_dei_magnetophone_MusicService_getTime(JNIEnv *env, jclass clazz) {
    if (engine == nullptr) {
        LOGE("Engine is null, you must call createEngine before calling this method");
        return -1;
    }
    return engine->getCurrnetTime();
}

JNIEXPORT void JNICALL Java_unipd_dei_magnetophone_MusicService_play(JNIEnv *env, jclass clazz) {
    if (engine == nullptr) {
        LOGE("Engine is null, you must call createEngine before calling this method");
        return;
    }
    engine->play();
}

JNIEXPORT void JNICALL Java_unipd_dei_magnetophone_MusicService_stop(JNIEnv *env, jclass clazz) {
    if (engine == nullptr) {
        LOGE("Engine is null, you must call createEngine before calling this method");
        return;
    }
    engine->stop();
}

JNIEXPORT void JNICALL
Java_unipd_dei_magnetophone_MusicService_unloadSong(JNIEnv *env, jclass clazz) {
    if (engine == nullptr) {
        LOGE("Engine is null, you must call createEngine before calling this method");
        return;
    }
    engine->unloadSong();
}

JNIEXPORT void JNICALL Java_unipd_dei_magnetophone_MusicService_loadSong(JNIEnv *env, jclass clazz,
                                                                         jobjectArray pathsArray,
                                                                         jint songTypeNum,
                                                                         jint songSpeedNum,
                                                                         jstring songEquStr) {
    if (engine == nullptr) {
        LOGE("Engine is null, you must call createEngine before calling this method");
        return;
    }

    engine->loadSong(env, clazz, pathsArray, songTypeNum, songSpeedNum, songEquStr);
}

JNIEXPORT void JNICALL
Java_unipd_dei_magnetophone_MusicService_setSatellitePosition(JNIEnv *env, jclass clazz,
                                                              jint channelNumber, jint position) {
    if (engine == nullptr) {
        LOGE("Engine is null, you must call createEngine before calling this method");
        return;
    }
    engine->mixerSetChannelSatellitePosition(channelNumber, position);
}

JNIEXPORT void JNICALL
Java_unipd_dei_magnetophone_MusicService_setChannelEnabled(JNIEnv *env, jclass clazz,
                                                           jint channelNumber, jint enabled) {
    if (engine == nullptr) {
        LOGE("Engine is null, you must call createEngine before calling this method");
        return;
    }

    engine->mixerSetChannelEnabled(channelNumber, enabled);
}

JNIEXPORT void JNICALL
Java_unipd_dei_magnetophone_MusicService_setTrackChannel(JNIEnv *env, jclass clazz,
                                                         jint trackNumber, jint channelNumber) {
    if (engine == nullptr) {
        LOGE("Engine is null, you must call createEngine before calling this method");
        return;
    }
    engine->mixerSetTrackChannel(trackNumber, channelNumber);
}

JNIEXPORT jintArray JNICALL
Java_unipd_dei_magnetophone_MusicService_getTrackMap(JNIEnv *env, jclass clazz) {
    if (engine == nullptr) {
        LOGE("Engine is null, you must call createEngine before calling this method");
        return env->NewIntArray(4);
    }
    return engine->mixerGetTrackMap(env, clazz);

}

JNIEXPORT jint JNICALL
Java_unipd_dei_magnetophone_MusicService_getChannelSatellitePosition(JNIEnv *env, jclass clazz,
                                                                     jint channel) {
    if (engine == nullptr) {
        LOGE("Engine is null, you must call createEngine before calling this method");
        return -1;
    }
    return engine->mixerGetChannelSatellitePosition(channel);
}

JNIEXPORT jint JNICALL
Java_unipd_dei_magnetophone_MusicService_getChannelEnabled(JNIEnv *env, jclass clazz,
                                                           jint channel) {

    if (engine == nullptr) {
        LOGE("Engine is null, you must call createEngine before calling this method");
        return -1;
    }
    return engine->mixerGetChannelEnabled(channel);
}

JNIEXPORT void JNICALL
Java_unipd_dei_magnetophone_MusicService_setSpeed(JNIEnv *env, jclass clazz, jint speed) {
    if (engine == nullptr) {
        LOGE("Engine is null, you must call createEngine before calling this method");
        return;
    }
    return engine->setSpeed(speed);
}

//TODO da sistemare
// Imposta l'equalizzazione di riproduzione
JNIEXPORT void JNICALL
Java_unipd_dei_magnetophone_MusicService_setEqualization(JNIEnv *env, jclass clazz, jstring equal) {
    if (engine == nullptr) {
        LOGE("Engine is null, you must call createEngine before calling this method");
        return;
    }

    SongEqualization desiredEqu = engine->convertJavaEqualization(env, equal);
    engine->setFFTFilters(engine->getSongEqu(), desiredEqu);
}

JNIEXPORT void JNICALL
Java_unipd_dei_magnetophone_MusicService_fastForward(JNIEnv *env, jclass clazz) {
    if (engine == nullptr) {
        LOGE("Engine is null, you must call createEngine before calling this method");
        return;
    }
    engine->fastForward();
}

JNIEXPORT void JNICALL
Java_unipd_dei_magnetophone_MusicService_fastReverse(JNIEnv *env, jclass clazz) {
    if (engine == nullptr) {
        LOGE("Engine is null, you must call createEngine before calling this method");
        return;
    }
    engine->fastReverse();
}

JNIEXPORT jfloat JNICALL
Java_unipd_dei_magnetophone_MusicService_getRatio(JNIEnv *env, jclass clazz) {
    if (engine == nullptr) {
        LOGE("Engine is null, you must call createEngine before calling this method");
        return -1.0;
    }
    return engine->getRatio();
}
}