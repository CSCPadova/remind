#include <jni.h>
#include "native-player.h"

static NativePlayer *engine = nullptr;

extern "C"
{

void
Java_unipd_dei_megnetophone_NativePlayer_convertJavaEqualization(JNIEnv *env, jstring javaEqu) {
    if (engine == nullptr) {
        LOGE("Engine is null, you must call createEngine before calling this method");
        return;
    }

    engine->convertJavaEqualization(env, javaEqu);
}

JNIEXPORT void JNICALL Java_unipd_dei_magnetophone_MusicService_init(JNIEnv *env, jobject obj) {
    if (engine == nullptr) {
        engine = new NativePlayer();
        engine->setJavaVMObj(env);
        JavaVM *javaVM = nullptr;
        env->GetJavaVM(&javaVM);

        jclass cls = env->GetObjectClass(obj);

        engine->setNewGlobalRef((jobject) env->NewGlobalRef(obj));

        engine->setOnTimeUpdate(env->GetMethodID(cls, "onTimeUpdate", "(D)V"));
        engine->setSongSpeedCallback(env->GetMethodID(cls, "songSpeedCallback", "()V"));
        engine->setSongLoadedCallback(env->GetMethodID(cls, "songLoadedCallback", "()V"));
        engine->setPlaybackStateCallback(env->GetMethodID(cls, "playbackStateCallback", "(IZ)V"));
    }
}

JNIEXPORT void JNICALL
Java_unipd_dei_magnetophone_MusicService_terminate(JNIEnv *env, jclass clazz) {
    if (engine == nullptr) {
        LOGE("Engine is null, you must call createEngine before calling this method");
        return;
    }
    if (engine->getPlaybackState() != PLAYBACK_STATE_INITIALIZED)
        return;

    engine->stop();
    delete engine;
    engine = nullptr;
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
    return engine->getCurrentTime();
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
                                                                         jstring songEquStr,
                                                                         jstring equPath) {
    if (engine == nullptr) {
        LOGE("Engine is null, you must call createEngine before calling this method");
        return;
    }

    engine->loadSong(env, clazz, pathsArray, songTypeNum, songSpeedNum, songEquStr, equPath);
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
    engine->setFFTFilters(env, engine->getSongEqu(), desiredEqu);
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

JNIEXPORT void JNICALL
Java_unipd_dei_magnetophone_MusicService_setTrackVolume(JNIEnv *env, jclass clazz, jint track,
                                                        jfloat volumeL, jfloat volumeR) {
    if (engine == nullptr)
        LOGE("Engine is null, you must call createEngine before calling this method");

    return engine->mixerSetTrackVolume(track, volumeL, volumeR);
}

JNIEXPORT jfloat JNICALL
Java_unipd_dei_magnetophone_MusicService_getTrackVolumeL(JNIEnv *env, jclass clazz, jint track) {
    if (engine == nullptr) {
        LOGE("Engine is null, you must call createEngine before calling this method");
        return -1.0;
    }
    return engine->mixerGetTrackVolumeL(track);
}

JNIEXPORT jfloat JNICALL
Java_unipd_dei_magnetophone_MusicService_getTrackVolumeR(JNIEnv *env, jclass clazz, jint track) {
    if (engine == nullptr) {
        LOGE("Engine is null, you must call createEngine before calling this method");
        return -1.0;
    }
    return engine->mixerGetTrackVolumeR(track);
}
}