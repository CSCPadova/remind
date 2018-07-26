#ifndef MIXER_H
#define MIXER_H

#include "QuadraphonicFilter.h"
#include <audiostreams.h>
#include <filterbase.h>
#include <tgmath.h>

#define FRONT_DISTANCE_QUAD 0.36f
#define BACK_DISTANCE_QUAD 2.25f
#define HEAD_WIDTH 0.25f
#define HALF_HEAD_WIDTH 0.125f
#define HEAD_WIDTH_QUAD 0.0625f

enum SongType
{
	SONG_TYPE_1M, SONG_TYPE_1S, SONG_TYPE_2M, SONG_TYPE_4M
};

#define MAX_SATELLITE_POSITION 100
#define MIN_SATELLITE_POSITION 0

class MixerProcessor;

class Mixer: public FilterBase
{
	public:
		audio::InputStream inputs[4];
		audio::OutputStream outputLeft, outputRight;

		Mixer(SongType songType, int samplingFrequency);
		~Mixer();

		void setChannelSatellitePosition(int channelNumber, int position);
		void setChannelEnabled(int channelNumber, bool enabled);
		void getTrackMap(int * trackMap);
		void setTrackChannel(int trackNumber, int channelNumber);
		int getChannelSatellitePosition(int channel);
		int getChannelEnabled(int channel);
		int getSamplingFrequency()
		{
			return samplingFrequency;
		}
		void flush();
		//void mix(float ** buffers, float * outLeft, float * outRight, int length);

	private:
		friend class MixerProcessor1M;
		friend class MixerProcessor1S;
		friend class MixerProcessor2M;
		friend class MixerProcessor4M;

		SongType songType;
		int samplingFrequency;
		int inputBufferCount;
		int trackMap[4];
		int channelSatellitePosition[4];
		bool channelEnabled[4];
		MixerProcessor * processor;

		bool loop();	// da FilterBase
};

class MixerProcessor
{
	protected:
		Mixer * mixer;
		audio::AudioBuffer silence;
	public:
		MixerProcessor(Mixer * mixer);
		virtual ~MixerProcessor()
		{
		}
		virtual void process(audio::AudioBuffer (&buffers)[4], audio::AudioBuffer& outLeft, audio::AudioBuffer& outRight) = 0;
};

class MixerProcessor1M: public MixerProcessor
{
	public:
		MixerProcessor1M(Mixer * mixer)
				: MixerProcessor(mixer)
		{
		}
		void process(audio::AudioBuffer (&buffers)[4], audio::AudioBuffer& outLeft, audio::AudioBuffer& outRight);
};

class MixerProcessor1S: public MixerProcessor
{
	public:
		MixerProcessor1S(Mixer * mixer)
				: MixerProcessor(mixer)
		{
		}
		void process(audio::AudioBuffer (&buffers)[4], audio::AudioBuffer& outLeft, audio::AudioBuffer& outRight);
};

class MixerProcessor2M: public MixerProcessor
{
	private:
		DelayBuffer leftDelayBuffer, rightDelayBuffer;
	public:
		MixerProcessor2M(Mixer * mixer)
				: MixerProcessor(mixer), leftDelayBuffer(mixer->samplingFrequency * 8 / 1000), rightDelayBuffer(mixer->samplingFrequency * 8 / 1000)
		{
		}
		void process(audio::AudioBuffer (&buffers)[4], audio::AudioBuffer& outLeft, audio::AudioBuffer& outRight);
};

class MixerProcessor4M: public MixerProcessor
{
	private:
		DelayBuffer leftFrontDelayBuffer, rightFrontDelayBuffer;
		DelayBuffer leftBackDelayBuffer, rightBackDelayBuffer;
		DelayBuffer reverb[3];
		float lowPassLeft[2];
		float lowPassRight[2];
	public:
		MixerProcessor4M(Mixer * mixer)
				: MixerProcessor(mixer), leftFrontDelayBuffer(mixer->samplingFrequency * 7 / 10000), rightFrontDelayBuffer(
						mixer->samplingFrequency * 7 / 10000), leftBackDelayBuffer(mixer->samplingFrequency * 7 / 10000), rightBackDelayBuffer(
						mixer->samplingFrequency * 7 / 10000), reverb
						{ mixer->samplingFrequency * 44 / 1000, mixer->samplingFrequency * 40 / 1000, mixer->samplingFrequency * 38 / 1000 }
		{
		}
		~MixerProcessor4M()
		{
			lowPassLeft[0] =  0.0f;
			lowPassRight[0] = 0.0f;
			lowPassLeft[1] =  0.0f;
			lowPassRight[1] = 0.0f;
		}
		void process(audio::AudioBuffer (&buffers)[4], audio::AudioBuffer& outLeft, audio::AudioBuffer& outRight);
};
#endif
