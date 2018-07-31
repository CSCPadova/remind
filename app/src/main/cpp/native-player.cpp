#include <aaudio/AAudio.h>
#include "native-player.h"
#include "fftconvolver.h"
#include "audiocommon.h"

//short buffers[2][20000];		// <--- ugh

bool threadGo = false;

NativePlayer::NativePlayer() {
    currentSampleRate = -1;
    playbackState = PLAYBACK_STATE_INITIALIZED;
}

void NativePlayer::closeOutputStream() {

    if (playStream_ != nullptr) {
        aaudio_result_t result = AAudioStream_requestStop(playStream_);
        if (result != AAUDIO_OK) {
            LOGE("Error stopping output stream. %s", AAudio_convertResultToText(result));
        }

        result = AAudioStream_close(playStream_);
        if (result != AAUDIO_OK) {
            LOGE("Error closing output stream. %s", AAudio_convertResultToText(result));
        }
    }
}

void NativePlayer::setFFTFilters(SongEqualization inputEqu, SongEqualization outputEqu) {
    constexpr int size = audio::AudioBufferSize;
    std::vector<float> filter(size, 0.0f);

    //--- filtri passa basso temporanei per test ---
    int to = outputEqu == SongEqualization::CCIR ? size / 12 :
             outputEqu == SongEqualization::NAB ? size / 4 : size;

    for (int i = 0; i < to; i++) {
        filter[i] = 1.0f;
    }
    //----------------------------------------------

    fftconvolver[0].setFilter(filter.data(), size);
    fftconvolver[1].setFilter(filter.data(), size);
    fftconvolver[2].setFilter(filter.data(), size);
    fftconvolver[3].setFilter(filter.data(), size);
}

SongEqualization NativePlayer::convertJavaEqualization(JNIEnv *env, jstring javaEqu) {
    SongEqualization outEqu = SongEqualization::FLAT;

    const char *equName = env->GetStringUTFChars(javaEqu, nullptr);
    LOGD("Equalizzazione %s", equName);

    // Non la cosa più elegante, ma non si possono fare gli switch sui char* in C/C++
    if (strcmp(equName, "CCIR") == 0)
        outEqu = SongEqualization::CCIR;
    else if (strcmp(equName, "NAB") == 0)
        outEqu = SongEqualization::NAB;
    else if (strcmp(equName, "FLAT") == 0)
        outEqu = SongEqualization::FLAT;
    else {
        // wut?
        LOGE("setEqualization: passata equalizzazione non valida: %s", equName);
    }

    env->ReleaseStringUTFChars(javaEqu, equName);
    return outEqu;
}

SongEqualization NativePlayer::getSongEqu() {
    return songEqu;
}

void NativePlayer::playbackCallback() {
    audio::AudioBuffer leftBuffer, rightBuffer;

    if (waveReader->isEof() && !inLeft.hasData() && !inRight.hasData()) {
        //controllo stato bufferqueue
        LOGD("dati finiti");
        stop();
        return;
    }

    audio::Status leftStat = inLeft.pullData(leftBuffer);
    audio::Status rightStat = inRight.pullData(rightBuffer);

    if (leftStat == audio::Status::OK && rightStat == audio::Status::OK) {
        for (int i = 0; i < audio::AudioBufferSize; i++) {
            intermediateAudioBuffer.push_back((float) leftBuffer[i] * 32767);
            intermediateAudioBuffer.push_back((float) rightBuffer[i] * 32767);
        }
    }

    currentTime += 102400.0 / (double) mixer->getSamplingFrequency() / rateConverter->getRatio();
}

void NativePlayer::stop() {
    if (threadGo) {
        threadGo = false;
        if (fastThread->joinable())
            fastThread->join();
    }

    if (playbackState == PLAYBACK_STATE_PLAYING) {
        playbackState = PLAYBACK_STATE_STOPPED;
        aaudio_result_t result = AAudioStream_requestStop(playStream_);
        if (result != AAUDIO_OK) {
            LOGE("Error starting stream. %s", AAudio_convertResultToText(result));
        }
    }

    playbackChange(0, true);
}

void NativePlayer::play() {
    LOGD("Into PLAY");
    if (threadGo) {
        threadGo = false;
        if (fastThread->joinable())
            fastThread->join();
    }

    if (waveReader->isEof())
        return;

    playbackChange(0, false);

    if (playbackState != PLAYBACK_STATE_STOPPED)
        return;

    playbackState = PLAYBACK_STATE_PLAYING;

    aaudio_result_t result = AAudioStream_requestStart(playStream_);

    if (result != AAUDIO_OK) {
        LOGE("Error starting stream. %s", AAudio_convertResultToText(result));
    }
    // Store the underrun count so we can tune the latency in the dataCallback
    playStreamUnderrunCount_ = AAudioStream_getXRunCount(playStream_);
}

void NativePlayer::seek(double timeCentisec) {
    if (playbackState != PLAYBACK_STATE_STOPPED)
        return;

    /*
     * ++++++++ NOTA ++++++++++
     * Il problema è che i join sui filtri bloccano tutto finchè i loro thread
     * non vanno in timeout e terminano, bloccando la GUI nel frattempo.
     * Ovviamente la soluzione definitiva sarebbe rifare 'sto cesso di sistema
     * in modo che non sia tutto un patchwork di interconnessioni casuali,
     * ma ovviamente non ho nè tempo nè voglia di farlo; la soluzione bovina
     * è di creare un altro thread che esegua la reinizializzazione,
     * e usare detach() in modo che possa continuare anche quando seek() esce.
     *
     * A quanto pare però, capita che se si rientra in seek() prima che il thread
     * precedente abbia finito le sue operazioni, il programma crasha miseramente.
     * Ho quindi messo un mutex in modo da mantenere il comportamento precedente
     * ma prevenire che il codice venga chiamato mentre un'altra copia è in esecuzione;
     * in caso ciò succeda, la seconda esecuzione viene bloccata.
     */
    //static std::mutex threadJoinMtx;

    //threadJoinMtx.lock();    // Prova a fare un lock sul mutex; se si blocca, vuol dire che c'è ancora un thread che sta reinizializzando
    //threadJoinMtx.unlock();    // i filtri, quindi blocca finchè non si libera. Appena il lock() ha successo, lo sblocchiamo subito.

    //std::thread th([timeCentisec, mixer] {
    //    std::lock_guard<std::mutex> guard(threadJoinMtx);    // Impedisce di rientrare finchè non finiamo

    if (mixer != NULL)
        mixer->scheduleStop();

    if (waveReader != NULL)
        waveReader->scheduleStop();

    if (rateConverter != NULL)
        rateConverter->scheduleStop();

    fftconvolver[0].scheduleStop();
    fftconvolver[1].scheduleStop();
    fftconvolver[2].scheduleStop();
    fftconvolver[3].scheduleStop();

    // -- Join --
    LOGD("join");
    if (mixer != NULL)
        mixer->join();

    if (rateConverter != NULL)
        rateConverter->join();

    if (waveReader != NULL)
        waveReader->join();

    fftconvolver[0].join();
    fftconvolver[1].join();
    fftconvolver[2].join();
    fftconvolver[3].join();

    // -- Flush --
    LOGD("flush");
    if (mixer != NULL)
        mixer->flush();

    if (rateConverter != NULL)
        rateConverter->flush();

    if (waveReader != NULL) {
        waveReader->flush();
        waveReader->seek(timeCentisec);
    }

    fftconvolver[0].reset();
    fftconvolver[1].reset();
    fftconvolver[2].reset();
    fftconvolver[3].reset();

    // -- Run --
    if (mixer != NULL)
        mixer->run();

    if (rateConverter != NULL)
        rateConverter->run();

    if (waveReader != NULL) {
        LOGD("partito wave reader");
        waveReader->run();
    }

    fftconvolver[0].run();
    fftconvolver[1].run();
    fftconvolver[2].run();
    fftconvolver[3].run();

    LOGD("tutti filtri ok");
    //(*player_buf_q)->Clear(player_buf_q);
    if (!waveReader->isEof()) {
        playbackCallback(); //riempio i buffer
        playbackCallback();
    }
    //});
    //th.detach();

    playbackChange(0, true);
}

void NativePlayer::speedChange() {
    LOGD("speedChange");
    JNIEnv *env;
    int check = (jvm)->GetEnv((void **) &env, JNI_VERSION_1_6);

    if (check == JNI_EDETACHED)
        jvm->AttachCurrentThread(&env, NULL);
    if (songSpeedCallbackID != 0)
        env->CallVoidMethod(javaobj, songSpeedCallbackID);
    if (check == JNI_EDETACHED)
        jvm->DetachCurrentThread();
}

void NativePlayer::playbackChange(int type, bool stop) {
    LOGD("playbackChange");
    JNIEnv *env;
    int check = (jvm)->GetEnv((void **) &env, JNI_VERSION_1_6);

    if (check == JNI_EDETACHED)
        jvm->AttachCurrentThread(&env, NULL);

    if (playbackStateCallbackID != 0)
        env->CallVoidMethod(javaobj, playbackStateCallbackID, type, stop);

    if (check == JNI_EDETACHED)
        jvm->DetachCurrentThread();
}

void NativePlayer::songLoaded() {
    LOGD("songLoaded");
    JNIEnv *env;
    int check = (jvm)->GetEnv((void **) &env, JNI_VERSION_1_6);

    if (check == JNI_EDETACHED)
        jvm->AttachCurrentThread(&env, NULL);

    if (songLoadedCallbackID != 0)
        env->CallVoidMethod(javaobj, songLoadedCallbackID);

    if (check == JNI_EDETACHED)
        jvm->DetachCurrentThread();
}

void NativePlayer::timeUpdate() {
    JNIEnv *env;
    int check = (jvm)->GetEnv((void **) &env, JNI_VERSION_1_6);

    if (check == JNI_EDETACHED)
        jvm->AttachCurrentThread(&env, NULL);

    if (onTimeUpdateMethodID != 0)
        env->CallVoidMethod(javaobj, onTimeUpdateMethodID, currentTime);

    if (check == JNI_EDETACHED)
        jvm->DetachCurrentThread();
}

void NativePlayer::fastFunction() {
    LOGD("thread partito");
    std::chrono::milliseconds dura(100);

    float speedRatio;
    double songDuration = waveReader->getSongDuration();
    SongSpeed originalSongSpeed = rateConverter->getOriginalSongSpeed();

    speedRatio = pow(2, originalSongSpeed - SONG_SPEED_30) * 0.25;
    threadGo = true;
    playbackChange(1, reverse);
    bool igo = true;

    while (threadGo && igo) {
        currentTime = currentTime + (reverse ? -10 : 10) / speedRatio;
        if (currentTime >= songDuration) {
            currentTime = songDuration;
            igo = false;
        } else if (currentTime <= 0) {
            currentTime = 0;
            igo = false;
        }

        timeUpdate();
        std::this_thread::sleep_for(dura);
    }

    seek(currentTime);
    threadGo = false;
    //fastThread->detach();
}

/**
 * Creates a stream builder which can be used to construct streams
 * @return a new stream builder object
 */
AAudioStreamBuilder *NativePlayer::createStreamBuilder() {

    AAudioStreamBuilder *builder = nullptr;
    aaudio_result_t result = AAudio_createStreamBuilder(&builder);
    if (result != AAUDIO_OK) {
        LOGE("Error creating stream builder: %s", AAudio_convertResultToText(result));
    }
    return builder;
}

/**
 * If there is an error with a stream this function will be called. A common example of an error
 * is when an audio device (such as headphones) is disconnected. In this case you should not
 * restart the stream within the callback, instead use a separate thread to perform the stream
 * recreation and restart.
 *
 * @param stream the stream with the error
 * @param userData the context in which the function is being called, in this case it will be the
 * PlayAudioEngine instance
 * @param error the error which occured, a human readable string can be obtained using
 * AAudio_convertResultToText(error);
 *
 * @see PlayAudioEngine#errorCallback
 */
void errorCallback(AAudioStream *stream,
                   void *userData,
                   aaudio_result_t error) {
    assert(userData);
    NativePlayer *audioEngine = reinterpret_cast<NativePlayer *>(userData);
    audioEngine->errorCallback(stream, error);
}

/**
 * @see errorCallback function at top of this file
 */
void NativePlayer::errorCallback(AAudioStream *stream,
                                 aaudio_result_t error) {

    assert(stream == playStream_);
    LOGD("errorCallback result: %s", AAudio_convertResultToText(error));

    aaudio_stream_state_t streamState = AAudioStream_getState(playStream_);
    if (streamState == AAUDIO_STREAM_STATE_DISCONNECTED) {

        // Handle stream restart on a separate thread
        std::function<void(void)> restartStream = std::bind(&NativePlayer::restartStream, this);
        std::thread streamRestartThread(restartStream);
        streamRestartThread.detach();
    }
}

void NativePlayer::restartStream() {

    LOGD("Restarting stream");

    if (restartingLock_.try_lock()) {
        closeOutputStream();
        //TODO sistemare
        //setupAudioEngine();
        restartingLock_.unlock();
    } else {
        LOGD("Restart stream operation already in progress - ignoring this request");
    }
}

/**
 * Every time the playback stream requires data this method will be called.
 *
 * @param stream the audio stream which is requesting data, this is the playStream_ object
 * @param userData the context in which the function is being called, in this case it will be the
 * PlayAudioEngine instance
 * @param audioData an empty buffer into which we can write our audio data
 * @param numFrames the number of audio frames which are required
 * @return Either AAUDIO_CALLBACK_RESULT_CONTINUE if the stream should continue requesting data
 * or AAUDIO_CALLBACK_RESULT_STOP if the stream should stop.
 *
 * @see PlayAudioEngine#dataCallback
 */
aaudio_data_callback_result_t dataCallback(AAudioStream *stream, void *userData,
                                           void *audioData, int32_t numFrames) {
    assert(userData &&
           audioData);//if both zero a message is written to the standard error device and abort is called, terminating the program execution
    NativePlayer *audioEngine = reinterpret_cast<NativePlayer *>(userData);//when you call reinterpret_cast the CPU does not invoke any calculations. It just treats a set of bits in the memory like if it had another type
    return audioEngine->dataCallback(stream, audioData, numFrames);
}

/**
 * Sets the stream parameters which are specific to playback, including device id and the
 * dataCallback function, which must be set for low latency playback.
 * @param builder The playback stream builder
 */
void NativePlayer::setupPlaybackStreamParameters(AAudioStreamBuilder *builder,
                                                 int playbackDeviceId_,
                                                 int sampleFormat_,
                                                 int sampleChannels_,
                                                 int sampleRate_) {
    AAudioStreamBuilder_setDeviceId(builder, playbackDeviceId_);
    AAudioStreamBuilder_setFormat(builder, sampleFormat_);
    AAudioStreamBuilder_setChannelCount(builder, sampleChannels_);
    AAudioStreamBuilder_setSampleRate(builder, sampleRate_);

    // We request EXCLUSIVE mode since this will give us the lowest possible latency.
    // If EXCLUSIVE mode isn't available the builder will fall back to SHARED mode.
    AAudioStreamBuilder_setSharingMode(builder, AAUDIO_SHARING_MODE_EXCLUSIVE);
    AAudioStreamBuilder_setPerformanceMode(builder, AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
    AAudioStreamBuilder_setDirection(builder, AAUDIO_DIRECTION_OUTPUT);
    AAudioStreamBuilder_setDataCallback(builder, ::dataCallback, this);
    AAudioStreamBuilder_setErrorCallback(builder, ::errorCallback, this);
}

/**
 * @see dataCallback function at top of this file
 */
aaudio_data_callback_result_t NativePlayer::dataCallback(AAudioStream *stream,
                                                         void *audioData,
                                                         int32_t numFrames) {
    assert(stream == playStream_);

    //controlla che il buffer non sia mai stato vuoto
    int32_t underrunCount = AAudioStream_getXRunCount(playStream_);
    aaudio_result_t bufferSize = AAudioStream_getBufferSizeInFrames(playStream_);
    bool hasUnderrunCountIncreased = false;
    bool shouldChangeBufferSize = false;

    if (underrunCount > playStreamUnderrunCount_) {
        playStreamUnderrunCount_ = underrunCount;
        hasUnderrunCountIncreased = true;
    }

    if (hasUnderrunCountIncreased && bufferSizeSelection_ == BUFFER_SIZE_AUTOMATIC) {

        /**
         * This is a buffer size tuning algorithm. If the number of underruns (i.e. instances where
         * we were unable to supply sufficient data to the stream) has increased since the last callback
         * we will try to increase the buffer size by the burst size, which will give us more protection
         * against underruns in future, at the cost of additional latency.
         */
        bufferSize += framesPerBurst_; // Increase buffer size by one burst
        shouldChangeBufferSize = true;
    } else if (bufferSizeSelection_ > 0 && (bufferSizeSelection_ * framesPerBurst_) != bufferSize) {

        // If the buffer size selection has changed then update it here
        bufferSize = bufferSizeSelection_ * framesPerBurst_;
        shouldChangeBufferSize = true;
    }

    if (shouldChangeBufferSize) {
        LOGD("Setting buffer size to %d", bufferSize);
        bufferSize = AAudioStream_setBufferSizeInFrames(stream, bufferSize);
        if (bufferSize > 0) {
            bufSizeInFrames_ = bufferSize;
        } else {
            LOGE("Error setting buffer size: %s", AAudio_convertResultToText(bufferSize));
        }
    }

    int32_t samplesPerFrame = sampleChannels_;

    //zeroing the audio buffer before filling it mixing the sounds
    memset(static_cast<uint8_t *>(audioData), 0,
           sizeof(float) * samplesPerFrame * numFrames);

    float *temp = static_cast<float *>(audioData);

    int i;
    for (i = 0; !intermediateAudioBuffer.empty() && i < numFrames; i++) {
        temp[i] = intermediateAudioBuffer.at(i);
    }

    if (!intermediateAudioBuffer.empty()) {
        intermediateAudioBuffer.erase(intermediateAudioBuffer.begin(),
                                      intermediateAudioBuffer.begin() + i);
    }



    //calculateCurrentOutputLatencyMillis(stream, &currentOutputLatencyMillis_);

    return AAUDIO_CALLBACK_RESULT_CONTINUE;
}

/**
 * Creates an audio stream for playback. The audio device used will depend on playbackDeviceId_.
 */
void NativePlayer::setupAudioEngine(int playbackDeviceId_,
                                    int sampleFormat_,
                                    int sampleChannels_,
                                    int sampleRate_) {
    currentTime = 0;

    AAudioStreamBuilder *builder = createStreamBuilder();

    if (builder != nullptr) {

        setupPlaybackStreamParameters(builder, playbackDeviceId_, sampleFormat_, sampleChannels_,
                                      sampleRate_);

        aaudio_result_t result = AAudioStreamBuilder_openStream(builder, &playStream_);

        if (result == AAUDIO_OK && playStream_ != nullptr) {

            // check that we got PCM_FLOAT format
            if (sampleFormat_ != AAudioStream_getFormat(playStream_)) {
                LOGD("Sample format is not PCM_FLOAT");
            }

            currentSampleRate = AAudioStream_getSampleRate(playStream_);
            currentFramesPerBurst = AAudioStream_getFramesPerBurst(playStream_);

            // Set the buffer size to the burst size - this will give us the minimum possible latency
            AAudioStream_setBufferSizeInFrames(playStream_, currentFramesPerBurst);
            bufSizeInFrames_ = currentFramesPerBurst;

            PrintAudioStreamInfo(playStream_);

            LOGD("creato audio engine");

            // Start the stream - the dataCallback function will start being called
            //result = AAudioStream_requestStart(playStream_);
            //if (result != AAUDIO_OK) {
            //    LOGE("Error starting stream. %s", AAudio_convertResultToText(result));
            //}

            // Store the underrun count so we can tune the latency in the dataCallback
            //playStreamUnderrunCount_ = AAudioStream_getXRunCount(playStream_);

        } else {
            LOGE("Failed to create stream. Error: %s", AAudio_convertResultToText(result));
        }

        AAudioStreamBuilder_delete(builder);

    } else {
        LOGE("Unable to obtain an AAudioStreamBuilder object");
    }
}

int NativePlayer::getPlaybackState() {
    return playbackState;
}

int NativePlayer::getCurrnetTime() {
    return currentTime;
}

void NativePlayer::unloadSong() {
    if (playbackState != PLAYBACK_STATE_STOPPED)
        return;

    //TODO da fare
    //releaseAudioEngine();

    if (mixer != NULL)mixer->scheduleStop();

    rateConverter->scheduleStop();
    if (waveReader != NULL)
        waveReader->scheduleStop();

    fftconvolver[0].scheduleStop();
    fftconvolver[1].scheduleStop();
    fftconvolver[2].scheduleStop();
    fftconvolver[3].scheduleStop();

    LOGD("scheduleStop tutto");
    if (mixer != NULL)
        mixer->join();

    rateConverter->join();
    waveReader->join();

    fftconvolver[0].join();
    fftconvolver[1].join();
    fftconvolver[2].join();
    fftconvolver[3].join();

    LOGD("join tutto");

    delete mixer;
    mixer = NULL;

    delete rateConverter;
    delete waveReader;
    waveReader = NULL;
    rateConverter = NULL;

    LOGD("distrutto tutto");

    playbackState = PLAYBACK_STATE_INITIALIZED;
}

void NativePlayer::loadSong(JNIEnv *env, jclass clazz, jobjectArray pathsArray, jint songTypeNum,
                            jint songSpeedNum, jstring songEquStr) {
    if (playbackState != PLAYBACK_STATE_INITIALIZED)
        return;

    int ntracce = 0;

    // --- Imposta velocità, tipo Song e equalizzazioni ---
    switch (songSpeedNum) {
        default:
        case SONG_SPEED_3_75:
            songSpeed = SONG_SPEED_3_75;
            break;
        case SONG_SPEED_7_5:
            songSpeed = SONG_SPEED_7_5;
            break;
        case SONG_SPEED_15:
            songSpeed = SONG_SPEED_15;
            break;
        case SONG_SPEED_30:
            songSpeed = SONG_SPEED_30;
            break;
    }

    switch (songTypeNum) {
        case SONG_TYPE_1M:
            songType = SONG_TYPE_1M;
            ntracce = 1;
            break;
        case SONG_TYPE_1S:
            songType = SONG_TYPE_1S;
            ntracce = 2;
            break;
        case SONG_TYPE_2M:
            songType = SONG_TYPE_2M;
            ntracce = 2;
            break;
        case SONG_TYPE_4M:
            songType = SONG_TYPE_4M;
            ntracce = 4;
            break;
    }

    songEqu = convertJavaEqualization(env, songEquStr);
    setFFTFilters(songEqu, desiredEqu);

    // --- Crea WaveReader ---
    jstring string[4];
    char const *paths[4] = {nullptr};
    for (int i = 0; i < ntracce; i++) {
        string[i] = (jstring) env->GetObjectArrayElement(pathsArray, i);
        paths[i] = env->GetStringUTFChars(string[i], nullptr);
    }

    waveReader = new WaveReader(paths, ntracce);

    for (int i = 1; i < ntracce; i++)
        env->ReleaseStringUTFChars(string[i], paths[i]);

    if (!waveReader->isValid()) {
        LOGE("native-player loadSong: Errore nel creare WaveReader");
        return;
    }

    // --- Collega i filtri ---
    //numChannels = waveReader->getChannelCount();
    songSampleRate = waveReader->getSamplerate();
    int bitPerSample = waveReader->getBitPerSample();
    mixer = new Mixer(songType, songSampleRate);

    rateConverter = new RateConverter(songSpeed, ntracce);

    // --- Connessione dei filtri ---
    audio::connect(waveReader->outStreams[0], fftconvolver[0].input);
    audio::connect(fftconvolver[0].output, rateConverter->inStreams[0]);
    audio::connect(rateConverter->outStreams[0], mixer->inputs[0]);

    if (songType == SONG_TYPE_1S) {
        audio::connect(waveReader->outStreams[1], fftconvolver[1].input);
        audio::connect(fftconvolver[1].output, rateConverter->inStreams[1]);
        audio::connect(rateConverter->outStreams[1], mixer->inputs[1]);
    }

    for (int i = 1; i < ntracce; i++) {
        audio::connect(waveReader->outStreams[i], fftconvolver[i].input);
        audio::connect(fftconvolver[i].output, rateConverter->inStreams[i]);
        audio::connect(rateConverter->outStreams[i], mixer->inputs[i]);
    }

    audio::connect(mixer->outputLeft, inLeft);
    audio::connect(mixer->outputRight, inRight);
    // ------------------------------

    //void NativePlayer::setupPlaybackStreamParameters(AAudioStreamBuilder *builder,
    //                                                 int playbackDeviceId_,
    //                                                int sampleFormat_,
    //                                                int sampleChannels_,
    //                                                int sampleRate_)
    setupAudioEngine(0, AAUDIO_FORMAT_PCM_I16, 2, songSampleRate);


    playbackState = PLAYBACK_STATE_STOPPED;
    seek(0);
    songLoaded();
    speedChange();
    playbackChange(0, true);
}

void NativePlayer::mixerSetChannelSatellitePosition(int channelNumber, int position) {
    if (mixer != NULL)
        mixer->setChannelSatellitePosition(channelNumber, position);
}

void NativePlayer::mixerSetChannelEnabled(int channelNumber, int enabled) {
    if (mixer != NULL)
        mixer->setChannelEnabled(channelNumber, enabled);
}

void NativePlayer::mixerSetTrackChannel(int trackNumber, int channelNumber) {
    if (mixer != NULL)
        mixer->setTrackChannel(trackNumber, channelNumber);
}

jintArray NativePlayer::mixerGetTrackMap(JNIEnv *env, jclass clazz) {
    jintArray result = env->NewIntArray(4);
    if (result == NULL)
        return NULL;

    // fill a temp structure to use to populate the java int array
    jint map[4];
    if (mixer != NULL)
        mixer->getTrackMap(map);

    env->SetIntArrayRegion(result, 0, 4, map);
    return result;
}

int NativePlayer::mixerGetChannelSatellitePosition(int channel) {
    if (mixer != NULL)
        return mixer->getChannelSatellitePosition(channel);
    return -1;
}

int NativePlayer::mixerGetChannelEnabled(int channel) {
    if (mixer != NULL)
        return mixer->getChannelEnabled(channel);
    return -1;
}

void NativePlayer::setSpeed(int speed) {
    if (playbackState == PLAYBACK_STATE_INITIALIZED)
        return;

    switch (speed) {
        case SONG_SPEED_3_75:
            songSpeed = SONG_SPEED_3_75;
            break;
        case SONG_SPEED_7_5:
            songSpeed = SONG_SPEED_7_5;
            break;
        case SONG_SPEED_15:
            songSpeed = SONG_SPEED_15;
            break;
        case SONG_SPEED_30:
            songSpeed = SONG_SPEED_30;
            break;
        default:
            return;
    }

    rateConverter->setSpeed(songSpeed);

    speedChange();
}

void NativePlayer::fastForward() {
    if (playbackState == PLAYBACK_STATE_INITIALIZED)
        return;
    reverse = false;

    if (threadGo) {
        playbackChange(1, reverse);
        return;
    }
    LOGD("fastforward");
    stop();
    LOGD("stopped");
    delete fastThread;
    fastThread = new std::thread(&NativePlayer::fastFunction, this);
}

void NativePlayer::fastReverse() {
    if (playbackState == PLAYBACK_STATE_INITIALIZED)
        return;
    reverse = true;

    if (threadGo) {
        playbackChange(1, reverse);
        return;
    }

    LOGD("fastreverse");
    stop();

    delete fastThread;
    fastThread = new std::thread(&NativePlayer::fastFunction, this);
}

float NativePlayer::getRatio() {
    if (rateConverter == NULL)
        return 1.0f;

    if (threadGo)
        //sono in fast
        return pow(0.5, rateConverter->getOriginalSongSpeed() - (SONG_SPEED_FAST + 1));

    return 1.0f / rateConverter->getRatio();
}

void NativePlayer::setNewGlobalRef(jobject jObject) {
    javaobj = jObject;

}

void NativePlayer::setOnTimeUpdate(jmethodID mID) {
    onTimeUpdateMethodID = mID;

}

void NativePlayer::setSongSpeedCallback(jmethodID mID) {
    songSpeedCallbackID = mID;

}

void NativePlayer::setSongLoadedCallback(jmethodID mID) {
    songLoadedCallbackID = mID;

}

void NativePlayer::setPlaybackStateCallback(jmethodID mID) {
    playbackStateCallbackID = mID;
}
