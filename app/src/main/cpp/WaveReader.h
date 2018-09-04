#ifndef WAVE_READER_H
#define WAVE_READER_H
#include "filterbase.h"
#include <libsndfile/sndfile.hh>
#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#include "audiostreams.h"

// TODO sostituire con veri oggetti ecccezione (o toglierle altogether)
#define FILE_FORMAT_NOT_SUPPORTED_EXCEPTION 1
#define FILE_NOT_SEEKABLE_EXCEPTION 2

class WaveReader : public FilterBase
{
	private:
		SNDFILE * fileAudio[4] = {nullptr};
		int inTracks = -1;
		int outTracks = -1;
		SF_INFO info;
		bool eof = false;
		bool validReader = false;
		float tempBuffer[audio::AudioBufferSize*2];

		bool loop();

	public:
		audio::OutputStream outStreams[4];
		audio::AudioBuffer buffers[4];

		WaveReader(const char * paths [], int nPath);
		~WaveReader();

		int getAudioFileFormat();
		int getChannelCount();
		int getBitPerSample();
		int getSamplerate();
		double getSongDuration();
		void seek(double timeCentiSec);

		void flush();
		bool isEof();

		bool isValid();		// ritorna false se l'inizializzazione è fallita
};

#endif
