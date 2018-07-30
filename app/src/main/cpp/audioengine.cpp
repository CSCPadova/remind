#include <assert.h>
#include <cstring>
#include <jni.h>
#include "audiocommon.h"
#include "log.h"
#include "audioengine.h"
#include "RateConverter.h"
#include "fftconvolver.h"
#include "native-player.h"

#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>

#include <android/trace.h>

short buffers[2][20000];		// <--- ugh

enum class SongEqualization {
    CCIR, NAB, FLAT
};

SongSpeed songSpeed;
SongEqualization songEqu, desiredEqu;
bool threadGo = false;

//-----------------------------------
// Filtri

FFTConvolver fftconvolver[4];

//-----------------------------------
//<Utilita'>
void setFFTFilters(SongEqualization inputEqu, SongEqualization outputEqu)
{
    constexpr int size = audio::AudioBufferSize;
    std::vector<float> filter(size, 0.0f);

    //--- filtri passa basso temporanei per test ---
    int to = outputEqu == SongEqualization::CCIR ? size / 12 :
             outputEqu == SongEqualization::NAB ? size / 4 : size;

    for(int i=0; i < to; i++) {
        filter[i] = 1.0f;
    }
    //----------------------------------------------

    fftconvolver[0].setFilter(filter.data(), size);
    fftconvolver[1].setFilter(filter.data(), size);
    fftconvolver[2].setFilter(filter.data(), size);
    fftconvolver[3].setFilter(filter.data(), size);
}

SongEqualization convertJavaEqualization(JNIEnv* env, jstring javaEqu)
{
    SongEqualization outEqu = SongEqualization::FLAT;

    const char *equName = env->GetStringUTFChars(javaEqu, nullptr);
    LOGD("Equalizzazione %s", equName);

    // Non la cosa più elegante, ma non si possono fare gli switch sui char* in C/C++
    if(strcmp(equName, "CCIR") == 0)
        outEqu = SongEqualization::CCIR;
    else if(strcmp(equName, "NAB") == 0)
        outEqu = SongEqualization::NAB;
    else if(strcmp(equName, "FLAT") == 0)
        outEqu = SongEqualization::FLAT;
    else {
        // wut?
        LOGE("setEqualization: passata equalizzazione non valida: %s", equName);
    }

    env->ReleaseStringUTFChars(javaEqu, equName);
    return outEqu;
}

//</Utilita'>


//costruttore
AudioEngine::AudioEngine() {

    // Initialize the trace functions, this enables you to output trace statements without
    // blocking. See https://developer.android.com/studio/profile/systrace-commandline.html

    mySampleRate_=-1;

    sampleChannels_ =-1;// kStereoChannelCount;
    sampleFormat_ =AAUDIO_FORMAT_INVALID;// AAUDIO_FORMAT_PCM_FLOAT;

    // Create the output stream. By not specifying an audio device id we are telling AAudio that
    // we want the stream to be created using the default playback audio device.
    createPlaybackStream();
}

AudioEngine::~AudioEngine(){//It's the destructor, it destroys the instance, frees up memory, etc. etc.

    closeOutputStream();
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
    assert(userData && audioData);//if both zero a message is written to the standard error device and abort is called, terminating the program execution
    AudioEngine *audioEngine = reinterpret_cast<AudioEngine *>(userData);//when you call reinterpret_cast the CPU does not invoke any calculations. It just treats a set of bits in the memory like if it had another type
    return audioEngine->dataCallback(stream, audioData, numFrames);
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
    AudioEngine *audioEngine = reinterpret_cast<AudioEngine *>(userData);
    audioEngine->errorCallback(stream, error);
}

/**
 * @see errorCallback function at top of this file
 */
void AudioEngine::errorCallback(AAudioStream *stream,
                                aaudio_result_t error){

    assert(stream == playStream_);
    LOGD("errorCallback result: %s", AAudio_convertResultToText(error));

    aaudio_stream_state_t streamState = AAudioStream_getState(playStream_);
    if (streamState == AAUDIO_STREAM_STATE_DISCONNECTED){

        // Handle stream restart on a separate thread
        std::function<void(void)> restartStream = std::bind(&AudioEngine::restartStream, this);
        std::thread streamRestartThread(restartStream);
        streamRestartThread.detach();
    }
}

void AudioEngine::restartStream(){

    LOGD("Restarting stream");

    if (restartingLock_.try_lock()){
        closeOutputStream();
        createPlaybackStream();
        restartingLock_.unlock();
    } else {
        LOGD("Restart stream operation already in progress - ignoring this request");
        // We were unable to obtain the restarting lock which means the restart operation is currently
        // active. This is probably because we received successive "stream disconnected" events.
        // Internal issue b/63087953
    }
}

void AudioEngine::closeOutputStream(){

    if (playStream_ != nullptr){
        aaudio_result_t result = AAudioStream_requestStop(playStream_);
        if (result != AAUDIO_OK){
            LOGE("Error stopping output stream. %s", AAudio_convertResultToText(result));
        }

        result = AAudioStream_close(playStream_);
        if (result != AAUDIO_OK){
            LOGE("Error closing output stream. %s", AAudio_convertResultToText(result));
        }
    }
}

/**
 * Creates a stream builder which can be used to construct streams
 * @return a new stream builder object
 */
AAudioStreamBuilder* AudioEngine::createStreamBuilder() {

    AAudioStreamBuilder *builder = nullptr;//serves as a universal null pointer literal, replacing the buggy and weakly-typed literal 0 and the infamous NULL macro
    aaudio_result_t result = AAudio_createStreamBuilder(&builder);
    if (result != AAUDIO_OK) {
        LOGE("Error creating stream builder: %s", AAudio_convertResultToText(result));
    }
    return builder;
}

/**
 * Creates an audio stream for playback. The audio device used will depend on playbackDeviceId_.
 */
void AudioEngine::createPlaybackStream(){

    AAudioStreamBuilder* builder = createStreamBuilder();

    if (builder != nullptr){

        setupPlaybackStreamParameters(builder);

        aaudio_result_t result = AAudioStreamBuilder_openStream(builder, &playStream_);

        if (result == AAUDIO_OK && playStream_ != nullptr){

            // check that we got PCM_FLOAT format
            if (sampleFormat_ != AAudioStream_getFormat(playStream_)) {
                LOGD("Sample format is not PCM_FLOAT");
            }

            sampleRate_ = AAudioStream_getSampleRate(playStream_);
            framesPerBurst_ = AAudioStream_getFramesPerBurst(playStream_);

            // Set the buffer size to the burst size - this will give us the minimum possible latency
            AAudioStream_setBufferSizeInFrames(playStream_, framesPerBurst_);
            bufSizeInFrames_ = framesPerBurst_;

            PrintAudioStreamInfo(playStream_);

            // Start the stream - the dataCallback function will start being called
            result = AAudioStream_requestStart(playStream_);
            if (result != AAUDIO_OK) {
                LOGE("Error starting stream. %s", AAudio_convertResultToText(result));
            }

            // Store the underrun count so we can tune the latency in the dataCallback
            playStreamUnderrunCount_ = AAudioStream_getXRunCount(playStream_);

        } else {
            LOGE("Failed to create stream. Error: %s", AAudio_convertResultToText(result));
        }

        AAudioStreamBuilder_delete(builder);

    } else {
        LOGE("Unable to obtain an AAudioStreamBuilder object");
    }
}

/**
 * Sets the stream parameters which are specific to playback, including device id and the
 * dataCallback function, which must be set for low latency playback.
 * @param builder The playback stream builder
 */
void AudioEngine::setupPlaybackStreamParameters(AAudioStreamBuilder *builder) {
    AAudioStreamBuilder_setDeviceId(builder, playbackDeviceId_);
    AAudioStreamBuilder_setFormat(builder, sampleFormat_);
    AAudioStreamBuilder_setChannelCount(builder, sampleChannels_);

    AAudioStreamBuilder_setSampleRate(builder, mySampleRate_);

    // We request EXCLUSIVE mode since this will give us the lowest possible latency.
    // If EXCLUSIVE mode isn't available the builder will fall back to SHARED mode.
    AAudioStreamBuilder_setSharingMode(builder, AAUDIO_SHARING_MODE_EXCLUSIVE);
    AAudioStreamBuilder_setPerformanceMode(builder, AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
    AAudioStreamBuilder_setDirection(builder, AAUDIO_DIRECTION_OUTPUT);
    AAudioStreamBuilder_setDataCallback(builder, ::dataCallback, this);
    AAudioStreamBuilder_setErrorCallback(builder, ::errorCallback, this);
}

aaudio_result_t
AudioEngine::calculateCurrentOutputLatencyMillis(AAudioStream *stream, double *latencyMillis) {

    // Get the time that a known audio frame was presented for playing
    int64_t existingFrameIndex;
    int64_t existingFramePresentationTime;
    aaudio_result_t result = AAudioStream_getTimestamp(stream,
                                                       CLOCK_MONOTONIC,
                                                       &existingFrameIndex,
                                                       &existingFramePresentationTime);

    if (result == AAUDIO_OK){

        // Get the write index for the next audio frame
        int64_t writeIndex = AAudioStream_getFramesWritten(stream);

        // Calculate the number of frames between our known frame and the write index
        int64_t frameIndexDelta = writeIndex - existingFrameIndex;

        // Calculate the time which the next frame will be presented
        int64_t frameTimeDelta = (frameIndexDelta * NANOS_PER_SECOND) / sampleRate_;
        int64_t nextFramePresentationTime = existingFramePresentationTime + frameTimeDelta;

        // Assume that the next frame will be written at the current time
        int64_t nextFrameWriteTime = get_time_nanoseconds(CLOCK_MONOTONIC);

        // Calculate the latency
        *latencyMillis = (double) (nextFramePresentationTime - nextFrameWriteTime)
                         / NANOS_PER_MILLISECOND;
    } else {
        LOGE("Error calculating latency: %s", AAudio_convertResultToText(result));
    }

    return result;
}

/**
 * @see dataCallback function at top of this file
 */
aaudio_data_callback_result_t AudioEngine::dataCallback(AAudioStream *stream,
                                                        void *audioData,
                                                        int32_t numFrames) {
    assert(stream == playStream_);

    //controlla che il buffer non sia mai stato vuoto
    int32_t underrunCount = AAudioStream_getXRunCount(playStream_);
    aaudio_result_t bufferSize = AAudioStream_getBufferSizeInFrames(playStream_);
    bool hasUnderrunCountIncreased = false;
    bool shouldChangeBufferSize = false;

    if (underrunCount > playStreamUnderrunCount_){
        playStreamUnderrunCount_ = underrunCount;
        hasUnderrunCountIncreased = true;
    }

    if (hasUnderrunCountIncreased && bufferSizeSelection_ == BUFFER_SIZE_AUTOMATIC){

        /**
         * This is a buffer size tuning algorithm. If the number of underruns (i.e. instances where
         * we were unable to supply sufficient data to the stream) has increased since the last callback
         * we will try to increase the buffer size by the burst size, which will give us more protection
         * against underruns in future, at the cost of additional latency.
         */
        bufferSize += framesPerBurst_; // Increase buffer size by one burst
        shouldChangeBufferSize = true;
    } else if (bufferSizeSelection_ > 0 && (bufferSizeSelection_ * framesPerBurst_) != bufferSize){

        // If the buffer size selection has changed then update it here
        bufferSize = bufferSizeSelection_ * framesPerBurst_;
        shouldChangeBufferSize = true;
    }

    if (shouldChangeBufferSize){
        LOGD("Setting buffer size to %d", bufferSize);
        bufferSize = AAudioStream_setBufferSizeInFrames(stream, bufferSize);
        if (bufferSize > 0) {
            bufSizeInFrames_ = bufferSize;
        } else {
            LOGE("Error setting buffer size: %s", AAudio_convertResultToText(bufferSize));
        }
    }

    /**
     * The following output can be seen by running a systrace. Tracing is preferable to logging
     * inside the callback since tracing does not block.
     *
     * See https://developer.android.com/studio/profile/systrace-commandline.html
     */
    //Trace::beginSection("numFrames %d, Underruns %d, buffer size %d",
    //                    numFrames, underrunCount, bufferSize);

    int32_t samplesPerFrame = sampleChannels_;

    //azzero il buffer audio prima di copiarci dentro dati
    memset(static_cast<uint8_t *>(audioData), 0,
           sizeof(float) * samplesPerFrame * numFrames);

    int errors=0;

    if(waveReader->isEof() && !inLeft.hasData() && !inRight.hasData())
    {
        //controllo stato bufferqueue
        LOGD("dati finiti");
        stop();
        errors++;
    }

    if(errors==0)
    {
        //TODO channel stride al posto di 2
        audio::Status leftStat = inLeft.pullData(static_cast<float *>(audioData), 2, numFrames, 32767);
        audio::Status rightStat = inRight.pullData(static_cast<float *>(audioData)+1, 2, numFrames, 32767);

        if(leftStat != audio::Status::OK && rightStat != audio::Status::OK)
        {
            errors++;
        }
        currentTime += 102400.0 / (double) mixer->getSamplingFrequency() / rateConverter->getRatio();
    }

    calculateCurrentOutputLatencyMillis(stream, &currentOutputLatencyMillis_);

    return AAUDIO_CALLBACK_RESULT_CONTINUE;
}

void AudioEngine::setBufferSizeInBursts(int32_t numBursts){
    AudioEngine::bufferSizeSelection_ = numBursts;
}

void AudioEngine::stop()
{
    if(threadGo)
    {
        threadGo = false;
        if (fastThread->joinable())
            fastThread->join();
    }

    if(playbackState == PLAYBACK_STATE_PLAYING)
    {
        playbackState = PLAYBACK_STATE_STOPPED;
        //(*player)->SetPlayState(player, SL_PLAYSTATE_PAUSED);
    }

    if (playStream_ != nullptr) {
        aaudio_result_t result = AAudioStream_requestStop(playStream_);
        if (result != AAUDIO_OK) {
            LOGE("Error stopping output stream. %s", AAudio_convertResultToText(result));
        }
    }
    playbackChange(0, true);
}

void AudioEngine::play()
{
    LOGD("Into PLAY");
    if(threadGo)
    {
        threadGo = false;
        if (fastThread->joinable())
            fastThread->join();
    }

    if(waveReader->isEof())
        return;

    playbackChange(0, false);

    if(playbackState != PLAYBACK_STATE_STOPPED)
        return;

    playbackState = PLAYBACK_STATE_PLAYING;
    // Start the stream - the dataCallback function will start being called
    aaudio_result_t result result = AAudioStream_requestStart(playStream_);
    if (result != AAUDIO_OK) {
        LOGE("Error starting stream. %s", AAudio_convertResultToText(result));
    }

    // Store the underrun count so we can tune the latency in the dataCallback
    playStreamUnderrunCount_ = AAudioStream_getXRunCount(playStream_);
}

void seek(double timeCentisec)
{
	if(playbackState != PLAYBACK_STATE_STOPPED)
		return;

	(*player)->SetPlayState(player, SL_PLAYSTATE_STOPPED);

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
	static std::mutex threadJoinMtx;

	threadJoinMtx.lock();	// Prova a fare un lock sul mutex; se si blocca, vuol dire che c'è ancora un thread che sta reinizializzando
	threadJoinMtx.unlock();	// i filtri, quindi blocca finchè non si libera. Appena il lock() ha successo, lo sblocchiamo subito.

	std::thread th([timeCentisec]
				   {
					   std::lock_guard<std::mutex> guard(threadJoinMtx);	// Impedisce di rientrare finchè non finiamo

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

					   if (waveReader != NULL)
					   {
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

					   if (waveReader != NULL)
					   {
						   LOGD("partito wave reader");
						   waveReader->run();
					   }

					   fftconvolver[0].run();
					   fftconvolver[1].run();
					   fftconvolver[2].run();
					   fftconvolver[3].run();

					   LOGD("tutti filtri ok");
					   (*player)->SetCallbackEventsMask(player, 0);
					   (*player_buf_q)->Clear(player_buf_q);
					   (*player)->SetCallbackEventsMask(player, SL_PLAYEVENT_HEADATNEWPOS);
					   if (!waveReader->isEof())
					   {
						   playbackCallback(NULL, NULL); //riempio i buffer
						   playbackCallback(NULL, NULL);
					   }
					   (*player)->SetPlayState(player, SL_PLAYSTATE_PAUSED);
				   });
	th.detach();

	playbackChange(0, true);
}


