#include "native-player.h"
#include "fftconvolver.h"
#include "audiocommon.h"
#include <math.h>
#include <liboboe/Oboe.h>

NativePlayer::NativePlayer() {
    time = 0;
    playbackDeviceId_ = 0;
    intermAudioBufferFillValue = 2000;
    threadGo = false;

    currentSampleRate = -1;
    currentPlaybackDeviceId = -1;
    currentSampleFormat = oboe::AudioFormat::Unspecified;
    currentSampleChannels = -1;
    playbackState = PLAYBACK_STATE_INITIALIZED;
}

NativePlayer::~NativePlayer() {
}

void NativePlayer::closeOutputStream() {

    if (stream != nullptr) {

        stop();

        oboe::Result result = stream->close();
        if (result != oboe::Result::OK) {
            LOGE("Error closing output stream. %s", oboe::convertToText(result));
        }
        delete stream;
    }
}

void
NativePlayer::setFFTFilters(SongEqualization newEqu) {
    //filtro base neutro
    constexpr int size = audio::AudioBufferSize;
    std::vector<float> filter(size, 1.0f);
    for (int i = 0; i < size; i++) {
        filter[i] = 1.0f;
    }
    EQCurrent = newEqu;

    char eqName[2000];
    strcpy(eqName, "");
    //strcpy(eqName, eqPath);

    //cerca le risposte impulsive per le varie combinazioni
    //se non trova il file viene dato per scontato che la risposta impulsiva sia neutra.
    //prima genera il nome del file che serve
    switch (songSpeedOriginal) {
        case SONG_SPEED_3_75:
            strcat(eqName, "3.75_");
            break;
        case SONG_SPEED_7_5:
            strcat(eqName, "7.5_");
            break;
        case SONG_SPEED_15:
            strcat(eqName, "15_");
            break;
        case SONG_SPEED_30:
            strcat(eqName, "30_");
            break;
        default:
            //non dovrebbe mai andare qui
            return;
    }

    switch (EQOriginal) {
        case SongEqualization::CCIR:
            strcat(eqName, "CCIR_");
            break;
        case SongEqualization::NAB:
            strcat(eqName, "NAB_");
            break;
        case SongEqualization::FLAT:
            strcat(eqName, "FLAT_");
            break;
    }

    switch (EQCurrent) {
        case SongEqualization::CCIR:
            strcat(eqName, "CCIR_");
            break;
        case SongEqualization::NAB:
            strcat(eqName, "NAB_");
            break;
        case SongEqualization::FLAT:
            strcat(eqName, "FLAT_");
            break;
    }

    switch (songSpeed) {
        case SONG_SPEED_3_75:
            strcat(eqName, "3.75");
            break;
        case SONG_SPEED_7_5:
            strcat(eqName, "7.5");
            break;
        case SONG_SPEED_15:
            strcat(eqName, "15");
            break;
        case SONG_SPEED_30:
            strcat(eqName, "30");
            break;
        default:
            //non dovrebbe mai andare qui
            return;
    }

    char pathEQToLoad[2000];
    strcpy(pathEQToLoad, eqPath);
    strcat(pathEQToLoad, "/");
    strcat(pathEQToLoad, eqName);
    strcat(pathEQToLoad, ".wav");
    std::string strPathEQ(eqName);
    //controlla se e' gia' stata usata
    for (int k = 0; k < loadedEQs.size(); k++) {
        auto tupla = loadedEQs.at(k);
        if (strPathEQ.compare(std::get<0>(tupla)) == 0) {
            filter.clear();
            filter = std::get<1>(tupla);
            LOGD("NativePlayer_setFFTFilters: caricamento filtro dalla cache");
            fftconvolver[0].setFilter(filter.data(), size);
            fftconvolver[1].setFilter(filter.data(), size);
            fftconvolver[2].setFilter(filter.data(), size);
            fftconvolver[3].setFilter(filter.data(), size);
            return;
        }
    }

    int fd = open(pathEQToLoad, O_RDONLY);
    //se non trova il file allora carica la risposta predefinita
    if (fd == -1) {
        LOGD("NativePlayer_setFFTFilters: file %s non trovato: caricamento filtro di default",
             pathEQToLoad);
        fftconvolver[0].setFilter(filter.data(), size);
        fftconvolver[1].setFilter(filter.data(), size);
        fftconvolver[2].setFilter(filter.data(), size);
        fftconvolver[3].setFilter(filter.data(), size);

        //se arriva a questo punto allora il filtro non era gia' in memoria
        //quindi lo tiene da parte
        std::vector<float> savedFilter = filter;
        auto second = std::make_tuple(strPathEQ, savedFilter);
        loadedEQs.push_back(second);

        return;
    }

    LOGD("NativePlayer_setFFTFilters: file %s trovato: caricamento filtro in corso", pathEQToLoad);
    //altrimenti chiude il file e continua
    close(fd);

    //legge il file .wav contenente la risposta impulsiva
    char const *paths[1] = {nullptr};
    paths[0] = pathEQToLoad;
    WaveReader *wavImpRes = new WaveReader(paths, 1);

    audio::InputStream streamImpResp;

    audio::connect(wavImpRes->outStreams[0], streamImpResp);

    audio::AudioBuffer filterBuffer;

    filter.clear();
    if (wavImpRes != NULL) {
        LOGD("NativePlayer_setFFTFilters: partita lettura wave reader impulse response");
        wavImpRes->run();
    }

    while (!streamImpResp.hasData()) {
        LOGD("NativePlayer_setFFTFilters Waiting for data");
        streamImpResp.waitIfEmpty();
    }
    //while (!wavImpRes->isEof() && streamImpResp.hasData()) {
    while (!wavImpRes->isEof()) {

        streamImpResp.waitIfEmpty();

        audio::Status impResStatus = streamImpResp.pullData(filterBuffer);

        if (impResStatus == audio::Status::OK) {
            for (int i = 0; i < audio::AudioBufferSize; i++) {
                filter.push_back(filterBuffer[i]);
            }
        } else {
            LOGE("NativePlayer_setFFTFilters: wave reader status NOT OK");
        }
    }
    if (wavImpRes != NULL) {
        wavImpRes->scheduleStop();
        wavImpRes->join();
    }

    if (filter.size() == 0)
        LOGE("NativePlayer_setFFTFilters: errore, il filtro e' vuoto");

    //filter contiene ora tutti i campioni

    //controlla che i sampling rate combacino
    if (wavImpRes->getSamplerate() != songSampleRate) {

        LOGD("NativePlayer_setFFTFilters: need resampling");
        //fase di setup
        double ratio = (double) (songSampleRate) / (double) (wavImpRes->getSamplerate());

        float *arrayFilterIn;
        arrayFilterIn = new float[filter.size()];

        for (int i = 0; i < filter.size(); i++) {
            arrayFilterIn[i] = filter.at(i);
        }

        float *arrayFilterOut;
        arrayFilterOut = new float[(int(filter.size() * ratio) + 1)];

        //inizializzazione della conversione
        SRC_DATA samplerate;

        /* samplerate params config */
        samplerate.data_in = arrayFilterIn;
        samplerate.input_frames = (long) filter.size();
        samplerate.data_out = arrayFilterOut;
        samplerate.end_of_input = 0;
        samplerate.src_ratio = ratio;
        samplerate.output_frames = (long) ((size_t) (ratio + 1)) * filter.size();

        size_t nread = 0;

        int error;
        SRC_STATE *state;
        if ((state = src_new(SRC_SINC_BEST_QUALITY, 1, &error)) == NULL) {
            LOGE("NativePlayer_setFFTFilters resample failed");
            exit(EXIT_FAILURE);
        }

        //conversione
        if ((error = src_process(state, &samplerate)))
            LOGE("NativePlayer_setFFTFilters: src_process failed : %s", src_strerror(error));

        //pulisce e riempie il filtro con la risposta impulsiva ricampionata
        filter.clear();
        for (int i = 0; i < samplerate.output_frames; i++) {
            filter[i] = arrayFilterOut[i];
        }
        LOGD("NativePlayer_setFFTFilters: resample avvenuto");

        src_delete(state);
        delete arrayFilterIn;
        delete arrayFilterOut;
    }
    LOGD("NativePlayer_setFFTFilters: caricamento filtro");

    fftconvolver[0].setFilter(filter.data(), size);
    fftconvolver[1].setFilter(filter.data(), size);
    fftconvolver[2].setFilter(filter.data(), size);
    fftconvolver[3].setFilter(filter.data(), size);

    //se arriva a questo punto allora il filtro non era gia' in memoria
    //quindi lo tiene da parte
    std::vector<float> savedFilter = filter;
    auto second = std::make_tuple(strPathEQ, savedFilter);
    loadedEQs.push_back(second);

    delete wavImpRes;
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
    return EQOriginal;
}

SongEqualization NativePlayer::getCurrentEqu() {
    return EQCurrent;
}

void NativePlayer::playbackCallback() {
    audio::AudioBuffer leftBuffer, rightBuffer;

    //if (waveReader->isEof() && !inLeft.hasData() && !inRight.hasData()) {
    if (waveReader->isEof()) {
        //controllo stato bufferqueue
        LOGD("dati finiti");
        stop();
        return;
    }

    inLeft.waitIfEmpty();
    inRight.waitIfEmpty();

    audio::Status leftStat = inLeft.pullData(leftBuffer);
    audio::Status rightStat = inRight.pullData(rightBuffer);

    if (leftStat == audio::Status::OK && rightStat == audio::Status::OK) {
        for (int i = 0; i < audio::AudioBufferSize; i++) {
            intermediateAudioBuffer.push_back(leftBuffer[i] * 32767);
            intermediateAudioBuffer.push_back(rightBuffer[i] * 32767);
        }
    }
    currentTime += 102400.0 / (double) mixer->getSamplingFrequency() / rateConverter->getRatio();
    timeUpdate();
}

void NativePlayer::stop() {
    if (songReady) {
        if (threadGo) {
            threadGo = false;
            if (fastThread->joinable()) {
                fastThread->join();
                delete fastThread;
            }
        }

        if (playbackState == PLAYBACK_STATE_PLAYING) {
            //cambia playbackState, unlock thread e solo dopo faccio un join
            playbackState = PLAYBACK_STATE_STOPPED;

            threadReadLock.unlock();
            //if (getAudioDataThread->joinable()) {
            //    getAudioDataThread->join();
            //    delete getAudioDataThread;
            //}

            oboe::Result result = stream->stop();
            if (result != oboe::Result::OK) {
                LOGE("Error stopping output stream. %s", oboe::convertToText(result));
            }
        }
        playbackChange(0, true);
    }
}

void NativePlayer::play() {
    if (songReady) {
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

        //if(getAudioDataThread!= nullptr) {
        //    if (getAudioDataThread->joinable())
        //        getAudioDataThread->join();
        //    delete getAudioDataThread;
        //}
        //getAudioDataThread = new std::thread(&NativePlayer::threadReadData, this);

        playbackState = PLAYBACK_STATE_PLAYING;

        oboe::Result result = stream->start();
        if (result != oboe::Result::OK) {
            LOGE("Error stopping output stream. %s", oboe::convertToText(result));
        }
    }
}

void NativePlayer::seek(double timeCentisec) {
    if (playbackState != PLAYBACK_STATE_STOPPED)
        return;

    stop();
    intermediateAudioBuffer.clear();

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

    threadJoinMtx.lock();    // Prova a fare un lock sul mutex; se si blocca, vuol dire che c'è ancora un thread che sta reinizializzando
    threadJoinMtx.unlock();    // i filtri, quindi blocca finchè non si libera. Appena il lock() ha successo, lo sblocchiamo subito.

    std::thread th([timeCentisec, this] {
        std::lock_guard<std::mutex> guard(
                threadJoinMtx);    // Impedisce di rientrare finchè non finiamo

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
    });
    th.detach();

    playbackChange(0, true);
}

void NativePlayer::fastFunction() {
    LOGD("thread partito");
    std::chrono::milliseconds dura(100);

    intermediateAudioBuffer.clear();

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

    threadGo = false;
    seek(currentTime);
}

//void NativePlayer::threadReadData() {
//    int prevLen = -1;
//    while (playbackState == PLAYBACK_STATE_PLAYING) {
//        prevLen = -1;
//        threadReadLock.lock();
//        while (intermediateAudioBuffer.size() < intermAudioBufferFillValue &&
//               prevLen != intermediateAudioBuffer.size() && playbackState == PLAYBACK_STATE_PLAYING) {
//            playbackCallback();
//            prevLen = intermediateAudioBuffer.size();
//        }
//    }
//}

oboe::DataCallbackResult NativePlayer::onAudioReady(
        oboe::AudioStream *oboeStream,
        void *audioData,
        int32_t numFrames) {

    if (callback_cpu_ids_.size() > 0 && !is_thread_affinity_set_) setThreadAffinity();

    //controlla che il buffer non sia mai stato vuoto
    int32_t underrunCount = (oboeStream->getXRunCount()).value();
    aaudio_result_t bufferSize = oboeStream->getBufferSizeInFrames();
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
        //LOGD("UNDERRUN DETECTED");
    } else if (bufferSizeSelection_ > 0 &&
               (bufferSizeSelection_ * framesPerBurst_) != bufferSize) {

        // If the buffer size selection has changed then update it here
        bufferSize = bufferSizeSelection_ * framesPerBurst_;
        shouldChangeBufferSize = true;
        LOGD("CHANGE BUFFER SIZE");
    }

    if (shouldChangeBufferSize) {
        LOGD("Setting buffer size to %d", bufferSize);
        oboeStream->setBufferSizeInFrames(bufferSize);
        if (bufferSize > 0) {
            bufSizeInFrames_ = bufferSize;
        } else {
            LOGE("Error setting buffer size: %d", bufferSize);
        }
    }
    int16_t *temp = static_cast<int16_t *>(audioData);

    if (playbackState == PLAYBACK_STATE_STOPPED) {
        //zeroing the audio buffer for silence
        memset(temp, 0, sizeof(int16_t) * currentSampleChannels * numFrames);
        return oboe::DataCallbackResult::Continue;
    }

    if(intermediateAudioBuffer.empty() ||
       intermediateAudioBuffer.size()< numFrames * currentSampleChannels)
    {
        memset(temp, 0, sizeof(int16_t) * currentSampleChannels * numFrames);
        playbackCallback();
        return oboe::DataCallbackResult::Continue;
    }

    //float max = 0;

    if (!intermediateAudioBuffer.empty() &&
        intermediateAudioBuffer.size() >= numFrames * currentSampleChannels) {

        int i;
        for (i = 0; i < numFrames * 2; i++) {
            temp[i] = intermediateAudioBuffer.at(i) * FINAL_VOLUME;
            //if (max < abs(temp[i]))
            //    max = temp[i];
        }
        intermediateAudioBuffer.erase(intermediateAudioBuffer.begin(),
                                      intermediateAudioBuffer.begin() + i);
    }

    //TODO decidere se tenere un numero magico
    intermAudioBufferFillValue = numFrames * 2 * 2*4;
    if (intermediateAudioBuffer.size() < intermAudioBufferFillValue)
        playbackCallback();

    return oboe::DataCallbackResult::Continue;
}

/**
 * Creates an audio stream for playback. The audio device used will depend on playbackDeviceId_.
 */
void NativePlayer::setupAudioEngineAndPlay(int playbackDeviceId_,
                                           oboe::AudioFormat sampleFormat_,
                                           int sampleChannels_,
                                           int sampleRate_) {
    oboe::AudioStreamBuilder builder;

    builder.setDeviceId(playbackDeviceId_);
    builder.setDirection(oboe::Direction::Output);
    builder.setSharingMode(oboe::SharingMode::Exclusive);
    builder.setSampleRate(sampleRate_);
    builder.setChannelCount(sampleChannels_);
    builder.setFormat(sampleFormat_);
    builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
    builder.setCallback(this);

    oboe::Result result = builder.openStream(&stream);

    if (result == oboe::Result::OK) {
        if (stream->getFormat() != sampleFormat_) {
            LOGD("Sample format is not PCM_FLOAT");
        }
        currentSampleRate = stream->getSampleRate();
        currentFramesPerBurst = stream->getFramesPerBurst();
        stream->setBufferSizeInFrames(currentFramesPerBurst);
        bufSizeInFrames_ = currentFramesPerBurst;

        PrintAudioStreamInfo(stream);

        LOGD("creato stream audio");

        playStreamUnderrunCount_ = (stream->getXRunCount()).value();
    } else {
        LOGE("Failed to create stream. Error: %s", oboe::convertToText(result));
    }

}

int NativePlayer::getPlaybackState() {
    return playbackState;
}

int NativePlayer::getCurrentTime() {
    return currentTime;
}

void NativePlayer::unloadSong() {
    if (playbackState != PLAYBACK_STATE_STOPPED)
        return;

    closeOutputStream();

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
    songReady = false;
    playbackChange(0, false);
}

//TODO verificare che songSpeedNum sia il valore originale della canzone e non quello selezionato dlla manopola
void
NativePlayer::loadSong(JNIEnv *env, jclass clazz, jobjectArray pathsArray,
                       jint songTypeNum, jint songSpeedNum, jstring songEquStr,
                       jstring equPathJStr) {
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
    songSpeedOriginal = songSpeed;

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
    mixer = new Mixer(songType, songSampleRate);

    rateConverter = new RateConverter(songSpeed, ntracce);

    EQOriginal = convertJavaEqualization(env, songEquStr);

    EQCurrent = EQOriginal;

    //loadEquImpResp(env, songSampleRate, songSpeed, songEqu, equPath);
    char const *pathsEQ[1] = {nullptr};
    paths[0] = env->GetStringUTFChars(equPathJStr, nullptr);
    memset(eqPath, 0, sizeof eqPath);
    strcat(eqPath, paths[0]);
    setFFTFilters(EQCurrent);
    env->ReleaseStringUTFChars(equPathJStr, paths[0]);

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

    //setup parametri motore audio
    currentPlaybackDeviceId = 0;
    currentSampleFormat = oboe::AudioFormat::I16;//oboe::AudioFormat::Float;//;
    currentSampleChannels = 2;
    currentSampleRate = songSampleRate;


    setupAudioEngineAndPlay(currentPlaybackDeviceId, currentSampleFormat, currentSampleChannels,
                            currentSampleRate);

    playbackState = PLAYBACK_STATE_STOPPED;
    currentTime = 0;
    seek(0);
    songLoaded();
    speedChange();
    songReady = true;
    playbackChange(0, true);
}

void NativePlayer::mixerSetTrackVolume(int trackNumber, float volumeL, float volumeR) {
    if (mixer != NULL)
        mixer->setTrackVolume(trackNumber, volumeL, volumeR);
}

float NativePlayer::mixerGetTrackVolumeL(int trackNumber) {
    if (mixer != NULL)
        return mixer->getTrackVolumeL(trackNumber);
    return -1;
}

float NativePlayer::mixerGetTrackVolumeR(int trackNumber) {
    if (mixer != NULL)
        return mixer->getTrackVolumeR(trackNumber);
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

    setFFTFilters(EQCurrent);
}

void NativePlayer::fastForward() {
    if (playbackState == PLAYBACK_STATE_INITIALIZED)
        return;

    LOGD("fastforward");
    stop();
    LOGD("stoppeclsd");

    reverse = false;

    if (threadGo) {
        playbackChange(1, reverse);
        return;
    }

    fastThread = new std::thread(&NativePlayer::fastFunction, this);
}

void NativePlayer::fastReverse() {
    if (playbackState == PLAYBACK_STATE_INITIALIZED)
        return;
    reverse = true;

    LOGD("fastreverse");
    stop();

    if (threadGo) {
        playbackChange(1, reverse);
        return;
    }

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

void NativePlayer::setJavaVMObj(JNIEnv *env) {
    env->GetJavaVM(&jvm);
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

void NativePlayer::setThreadAffinity() {

    pid_t current_thread_id = gettid();
    cpu_set_t cpu_set;
    CPU_ZERO(&cpu_set);

    // If the callback cpu ids aren't specified then bind to the current cpu
    if (callback_cpu_ids_.empty()) {
        int current_cpu_id = sched_getcpu();
        LOGD("Current CPU ID is %d", current_cpu_id);
        CPU_SET(current_cpu_id, &cpu_set);
    } else {

        for (size_t i = 0; i < callback_cpu_ids_.size(); i++) {
            int cpu_id = callback_cpu_ids_.at(i);
            LOGD("CPU ID %d added to cores set", cpu_id);
            CPU_SET(cpu_id, &cpu_set);
        }
    }

    int result = sched_setaffinity(current_thread_id, sizeof(cpu_set_t), &cpu_set);
    if (result == 0) {
        LOGD("Thread affinity set");
    } else {
        LOGD("Error setting thread affinity. Error no: %d", result);
    }

    is_thread_affinity_set_ = true;
}
