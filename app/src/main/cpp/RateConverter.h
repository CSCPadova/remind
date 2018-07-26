#ifndef RATE_CONVERTER_H
#define RATE_CONVERTER_H

#include <libsamplerate/samplerate.h>
#include <log.h>
#include <audiostreams.h>
#include <filterbase.h>
#include <filterbase.h>
#include <tgmath.h>

enum SongSpeed {
	SONG_SPEED_3_75,
	SONG_SPEED_7_5,
	SONG_SPEED_15,
	SONG_SPEED_30,
	SONG_SPEED_FAST // non usata dal rate converter
};

class RateConverter: public FilterBase {

public:
	audio::InputStream inStreams[4];
	audio::OutputStream outStreams[4];

	RateConverter(SongSpeed songSpeed, int nStreams);
	~RateConverter();

	void run();
	void execute();

	void join();
	void setSpeed(SongSpeed newSpeed);
	double getRatio();
	SongSpeed getOriginalSongSpeed();
	void flush() ;
private:
	SRC_STATE * state;
	SRC_DATA data;
	int nStreams;
	SongSpeed originalSongSpeed;
	SongSpeed currentSpeed;
	double ratio = 1.0f;
	int offU = 0, offP=0;
	float usabili [20000];
	float prodotti [20000];
	audio::AudioBuffer bufferIn;
	audio::AudioBuffer buffersOut[4];
	bool loop();
};

#endif
