#include "native-player.h"

#include "fftconvolver.h"

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
// Utilità

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



//----------------------------------

void playbackCallback(SLAndroidSimpleBufferQueueItf bq, void *context)
{
	audio::AudioBuffer leftBuffer, rightBuffer;

	if(waveReader->isEof() && !inLeft.hasData() && !inRight.hasData())
	{
		//controllo stato bufferqueue
		LOGD("dati finiti");
		stop();
		return;
	}

	audio::Status leftStat = inLeft.pullData(leftBuffer);
	audio::Status rightStat = inRight.pullData(rightBuffer);

	if(leftStat == audio::Status::OK && rightStat == audio::Status::OK)
	{
		for (int i = 0; i < audio::AudioBufferSize; i++)
		{
			buffers[bufcount][i * 2] = leftBuffer[i] * 32767;
			buffers[bufcount][i * 2 + 1] = rightBuffer[i] * 32767;
		}
	}

	(*player_buf_q)->Enqueue(player_buf_q, buffers[bufcount], audio::AudioBufferSize * 4);
	bufcount = (bufcount + 1) % 2;
	currentTime += 102400.0 / (double) mixer->getSamplingFrequency() / rateConverter->getRatio();
}

void stop()
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
		(*player)->SetPlayState(player, SL_PLAYSTATE_PAUSED);
	}

	playbackChange(0, true);
}

void play()
{
	LOGD("Into PLAY");
	if(threadGo)
	{
		threadGo = false;
		fastThread->join();
	}

	if(waveReader->isEof())
		return;

	playbackChange(0, false);

	if(playbackState != PLAYBACK_STATE_STOPPED)
		return;

	playbackState = PLAYBACK_STATE_PLAYING;
	(*player)->SetPlayState(player, SL_PLAYSTATE_PLAYING);
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

void speedChange()
{
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

void playbackChange(int type, bool stop)
{
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

void songLoaded()
{
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

void timeUpdate(SLPlayItf caller, void * context, SLuint32 event)
{
	JNIEnv *env;
	int check = (jvm)->GetEnv((void **) &env, JNI_VERSION_1_6);

	if (check == JNI_EDETACHED)
		jvm->AttachCurrentThread(&env, NULL);

	if (onTimeUpdateMethodID != 0)
		env->CallVoidMethod(javaobj, onTimeUpdateMethodID, currentTime);

	if (check == JNI_EDETACHED)
		jvm->DetachCurrentThread();
}

void fastFunction()
{
	LOGD("thread partito");
	std::chrono::milliseconds dura(100);

	float speedRatio;
	double songDuration = waveReader->getSongDuration();
	SongSpeed originalSongSpeed = rateConverter->getOriginalSongSpeed();

	speedRatio = pow(2, originalSongSpeed - SONG_SPEED_30) * 0.25;
	threadGo = true;
	playbackChange(1, reverse);
	bool igo = true;

	while (threadGo && igo)
	{
		currentTime = currentTime + (reverse ? -10 : 10) / speedRatio;
		if (currentTime >= songDuration)
		{
			currentTime = songDuration;
			igo = false;
		}
		else if (currentTime <= 0)
		{
			currentTime = 0;
			igo = false;
		}

		timeUpdate(NULL, NULL, 0);
		std::this_thread::sleep_for(dura);
	}

	seek(currentTime);
	threadGo = false;
	fastThread->detach();
}

extern "C"
{
	JNIEXPORT void JNICALL Java_unipd_dei_magnetophone_MusicService_init(JNIEnv* env, jobject obj)
	{
		slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);

		(*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
		(*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineItf);

		env->GetJavaVM( &jvm);
		//assert (rs == JNI_OK);

		jclass cls = env->GetObjectClass( obj);
		javaobj = (jobject) env->NewGlobalRef(obj);
		onTimeUpdateMethodID = env->GetMethodID(cls, "onTimeUpdate", "(D)V");
		songSpeedCallbackID = env->GetMethodID(cls, "songSpeedCallback", "()V");
		songLoadedCallbackID = env->GetMethodID(cls, "songLoadedCallback", "()V");
		playbackStateCallbackID = env->GetMethodID(cls, "playbackStateCallback", "(IZ)V");

		playbackState = PLAYBACK_STATE_INITIALIZED;
	}

// create the engine
	void setupAudioEngine(int sampleRate, int bitPerSample)
	{
		//create the Engine object
		(*engineItf)->CreateOutputMix(engineItf, &output_mix_obj, 0, NULL, NULL);
		(*output_mix_obj)->Realize(output_mix_obj, SL_BOOLEAN_FALSE);
		// configure audio source
		SLDataLocator_AndroidSimpleBufferQueue loc_bq = { SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2 };

		SLuint32 slSampleRate;
		switch (sampleRate)
		{
			case 96000:
				slSampleRate = SL_SAMPLINGRATE_96; //!!! non supportata
				break;
			case 48000:
				slSampleRate = SL_SAMPLINGRATE_48;
				break;
			case 22050:
				slSampleRate = SL_SAMPLINGRATE_22_05;
				break;
			case 24000:
				slSampleRate = SL_SAMPLINGRATE_24;
				break;
			case 32000:
				slSampleRate = SL_SAMPLINGRATE_32;
				break;
			case 16000:
				slSampleRate = SL_SAMPLINGRATE_16;
				break;
			case 8000:
				slSampleRate = SL_SAMPLINGRATE_16;
				break;
			default:
				slSampleRate = SL_SAMPLINGRATE_44_1;
				break;
		}

		SLuint32 slPcmFormat;
		switch (bitPerSample)
		{
			case SF_FORMAT_PCM_S8:
				slPcmFormat = SL_PCMSAMPLEFORMAT_FIXED_8;
				break;
			case SF_FORMAT_PCM_24:
				slPcmFormat = SL_PCMSAMPLEFORMAT_FIXED_24;
				break;
			case SF_FORMAT_PCM_32:
				slPcmFormat = SL_PCMSAMPLEFORMAT_FIXED_32;
				break;
			default:
				slPcmFormat = SL_PCMSAMPLEFORMAT_FIXED_16;
				break;
		}

		SLDataFormat_PCM format_pcm = { SL_DATAFORMAT_PCM, 2, slSampleRate, slPcmFormat, slPcmFormat, 0, SL_BYTEORDER_LITTLEENDIAN };
		SLDataSource audioSrc = { &loc_bq, &format_pcm };
		// configure audio sink
		SLDataLocator_OutputMix loc_outmix = { SL_DATALOCATOR_OUTPUTMIX, output_mix_obj };
		SLDataSink audioSnk = { &loc_outmix, NULL };
		//create the object
		const SLInterfaceID ids[] = { SL_IID_BUFFERQUEUE };
		const SLboolean req[] = { SL_BOOLEAN_TRUE };
		(*engineItf)->CreateAudioPlayer(engineItf, &player_obj, &audioSrc, &audioSnk, 1, ids, req);
		(*player_obj)->Realize(player_obj, SL_BOOLEAN_FALSE);
		(*player_obj)->GetInterface(player_obj, SL_IID_PLAY, &player);

		(*player_obj)->GetInterface(player_obj, SL_IID_BUFFERQUEUE, &player_buf_q);

		(*player)->SetPositionUpdatePeriod(player, 100);
		//assert(SL_RESULT_SUCCESS == result);

		(*player)->RegisterCallback(player, timeUpdate, NULL);
		//assert(SL_RESULT_SUCCESS == result);

		// register callback on the buffer queue
		(*player_buf_q)->RegisterCallback(player_buf_q, playbackCallback, NULL);

		currentTime = 0;

		LOGD("creato audio engine");
	}

	void releaseAudioEngine()
	{
		(*player_obj)->Destroy(player_obj);
		(*output_mix_obj)->Destroy(output_mix_obj);
	}

	JNIEXPORT void JNICALL Java_unipd_dei_magnetophone_MusicService_terminate(JNIEnv* env, jclass clazz)
	{
		if(playbackState != PLAYBACK_STATE_INITIALIZED)
			return;

		(*engineObject)->Destroy(engineObject);
		LOGD("terminate");
	}

	JNIEXPORT jint JNICALL Java_unipd_dei_magnetophone_MusicService_getPlaybackState(JNIEnv* env, jclass clazz)
	{
		return playbackState;
	}

	JNIEXPORT jint JNICALL Java_unipd_dei_magnetophone_MusicService_getTime(JNIEnv* env, jclass clazz)
	{
		return currentTime;
	}

	JNIEXPORT void JNICALL Java_unipd_dei_magnetophone_MusicService_play(JNIEnv* env, jclass clazz)
	{
		play();
	}

	JNIEXPORT void JNICALL Java_unipd_dei_magnetophone_MusicService_stop(JNIEnv* env, jclass clazz)
	{
		stop();
	}

	JNIEXPORT void JNICALL Java_unipd_dei_magnetophone_MusicService_unloadSong(JNIEnv* env, jclass clazz)
	{
		if(playbackState != PLAYBACK_STATE_STOPPED)
		return;

		releaseAudioEngine();
		if(mixer != NULL)mixer->scheduleStop();

		rateConverter->scheduleStop();
		if(waveReader != NULL)
		waveReader->scheduleStop();

		fftconvolver[0].scheduleStop();
		fftconvolver[1].scheduleStop();
		fftconvolver[2].scheduleStop();
		fftconvolver[3].scheduleStop();

		LOGD("scheduleStop tutto");
		if(mixer != NULL)
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

	JNIEXPORT void JNICALL Java_unipd_dei_magnetophone_MusicService_loadSong(JNIEnv* env, jclass clazz, jobjectArray pathsArray, jint songTypeNum, jint songSpeedNum, jstring songEquStr)
	{
		if(playbackState != PLAYBACK_STATE_INITIALIZED )
			return;

		int ntracce = 0;
		
		// --- Imposta velocità, tipo Song e equalizzazioni ---
		switch(songSpeedNum)
		{
			default:
			case SONG_SPEED_3_75: songSpeed = SONG_SPEED_3_75; break;
			case SONG_SPEED_7_5:  songSpeed = SONG_SPEED_7_5;  break;
			case SONG_SPEED_15:   songSpeed = SONG_SPEED_15;   break;
			case SONG_SPEED_30:   songSpeed = SONG_SPEED_30;   break;
		}

		switch(songTypeNum)
		{
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
		char const * paths [4] = {nullptr};
		for(int i = 0; i < ntracce; i++)
		{
			string[i] = (jstring) env->GetObjectArrayElement(pathsArray, i);
			paths[i] = env->GetStringUTFChars(string[i], nullptr);
		}

		waveReader = new WaveReader(paths, ntracce);
		
		for(int i = 1; i < ntracce; i++)
			env->ReleaseStringUTFChars(string[i], paths[i]);

		if(!waveReader->isValid()) {
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
		audio::connect(fftconvolver[0].output   , rateConverter->inStreams[0]);
		audio::connect(rateConverter->outStreams[0] , mixer->inputs[0]);

		if(songType == SONG_TYPE_1S)
		{
			audio::connect(waveReader->outStreams[1], fftconvolver[1].input);
			audio::connect(fftconvolver[1].output   , rateConverter->inStreams[1]);
			audio::connect(rateConverter->outStreams[1] , mixer->inputs[1]);
		}

		for(int i = 1; i < ntracce; i++)
		{
			audio::connect(waveReader->outStreams[i], fftconvolver[i].input);
			audio::connect(fftconvolver[i].output   , rateConverter->inStreams[i]);
			audio::connect(rateConverter->outStreams[i] , mixer->inputs[i]);
		}

		audio::connect(mixer->outputLeft , inLeft);
		audio::connect(mixer->outputRight, inRight);
		// ------------------------------

		setupAudioEngine(songSampleRate, bitPerSample);

		playbackState = PLAYBACK_STATE_STOPPED;
		seek(0);
		songLoaded();
		speedChange();
		playbackChange(0,true);
	}

	JNIEXPORT void JNICALL Java_unipd_dei_magnetophone_MusicService_setSatellitePosition(JNIEnv* env, jclass clazz, jint channelNumber, jint position)
	{
		if(mixer != NULL)
			mixer->setChannelSatellitePosition(channelNumber, position);
	}

	JNIEXPORT void JNICALL Java_unipd_dei_magnetophone_MusicService_setChannelEnabled(JNIEnv* env, jclass clazz, jint channelNumber, jint enabled)
	{
		if(mixer != NULL) {
			LOGD("%d %d",channelNumber,enabled);
			mixer->setChannelEnabled(channelNumber, enabled);
		}
	}

	JNIEXPORT void JNICALL Java_unipd_dei_magnetophone_MusicService_setTrackChannel(JNIEnv* env, jclass clazz,jint trackNumber, jint channelNumber)
	{
		if(mixer != NULL)
			mixer->setTrackChannel(trackNumber,channelNumber);
	}

	JNIEXPORT jintArray JNICALL Java_unipd_dei_magnetophone_MusicService_getTrackMap(JNIEnv* env, jclass clazz)
	{
		jintArray result = env->NewIntArray(4);
		if(result == NULL)
			return NULL;

		// fill a temp structure to use to populate the java int array
		jint map[4];
		if(mixer != NULL)
			mixer->getTrackMap(map);

		env->SetIntArrayRegion(result, 0, 4, map);
		return result;
	}

	JNIEXPORT jint JNICALL Java_unipd_dei_magnetophone_MusicService_getChannelSatellitePosition(JNIEnv* env, jclass clazz, jint channel)
	{
		if(mixer != NULL)
			return mixer->getChannelSatellitePosition(channel);
		return -1;
	}

	JNIEXPORT jint JNICALL Java_unipd_dei_magnetophone_MusicService_getChannelEnabled(JNIEnv* env, jclass clazz, jint channel)
	{
		if(mixer != NULL)
			return mixer->getChannelEnabled(channel);
		return -1;
	}

	JNIEXPORT void JNICALL Java_unipd_dei_magnetophone_MusicService_setSpeed(JNIEnv* env, jclass clazz, jint speed)
	{
		if(playbackState == PLAYBACK_STATE_INITIALIZED)
			return;

		switch(speed)
		{
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

	// Imposta l'equalizzazione di riproduzione
	JNIEXPORT void JNICALL Java_unipd_dei_magnetophone_MusicService_setEqualization(JNIEnv* env, jclass clazz, jstring equal)
	{
		desiredEqu = convertJavaEqualization(env, equal);
		setFFTFilters(songEqu, desiredEqu);
	}

	JNIEXPORT void JNICALL Java_unipd_dei_magnetophone_MusicService_fastForward(JNIEnv* env, jclass clazz)
	{
		if(playbackState == PLAYBACK_STATE_INITIALIZED)
			return;
		reverse = false;

		if(threadGo)
		{
			playbackChange(1,reverse);
			return;
		}
		LOGD("fastforward");
		stop();
		LOGD("stopped");
		delete fastThread;
		fastThread = new std::thread(&fastFunction);
	}

	JNIEXPORT void JNICALL Java_unipd_dei_magnetophone_MusicService_fastReverse(JNIEnv* env, jclass clazz)
	{
		if(playbackState == PLAYBACK_STATE_INITIALIZED)
			return;
		reverse = true;

		if(threadGo)
		{
			playbackChange(1,reverse);
			return;
		}

		LOGD("fastreverse");
		stop();

		delete fastThread;
		fastThread = new std::thread(&fastFunction);
	}

	JNIEXPORT jfloat JNICALL Java_unipd_dei_magnetophone_MusicService_getRatio(JNIEnv* env, jclass clazz)
	{
		if (rateConverter == NULL)
			return 1.0f;

		if (threadGo)
			//sono in fast
			return pow(0.5, rateConverter->getOriginalSongSpeed() - (SONG_SPEED_FAST + 1));

		return 1.0f / rateConverter->getRatio();
	}
}
