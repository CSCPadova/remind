#include "Mixer.h"

#include <stdio.h>
#include <stdlib.h>
#include <utility>		// swap#include <algorithm>	// max, min#include "log.h"

Mixer::Mixer(SongType songType, int samplingFrequency)
{
	this->samplingFrequency = samplingFrequency;
	switch (songType)
	{
		case SONG_TYPE_1M:
			this->inputBufferCount = 1;
			processor = new MixerProcessor1M(this);
			break;

		case SONG_TYPE_1S: // !!!
		case SONG_TYPE_2M:
			this->inputBufferCount = 2;

			this->trackMap[0] = 0;
			this->trackMap[1] = 1;

			this->channelSatellitePosition[0] = MIN_SATELLITE_POSITION;
			this->channelEnabled[0] = true;

			this->channelSatellitePosition[1] = MIN_SATELLITE_POSITION;
			this->channelEnabled[1] = true;

			processor = new MixerProcessor2M(this);
			break;

		case SONG_TYPE_4M:
			this->inputBufferCount = 4;

			this->trackMap[0] = 0;
			this->trackMap[1] = 1;
			this->trackMap[2] = 2;
			this->trackMap[3] = 3;

			this->channelSatellitePosition[0] = MIN_SATELLITE_POSITION;
			this->channelSatellitePosition[1] = MIN_SATELLITE_POSITION;
			this->channelSatellitePosition[2] = MIN_SATELLITE_POSITION;
			this->channelSatellitePosition[3] = MIN_SATELLITE_POSITION;

			this->channelEnabled[0] = true;
			this->channelEnabled[1] = true;
			this->channelEnabled[2] = true;
			this->channelEnabled[3] = true;
			processor = new MixerProcessor4M(this);
			break;
	}

	this->songType = songType;
}

Mixer::~Mixer()
{
	delete this->processor;
}

void Mixer::setChannelSatellitePosition(int channelNumber, int position)
{
	switch (this->songType)
	{
		case SONG_TYPE_4M:
			if (channelNumber >= 3 && channelNumber <= 4)
			{
				this->channelSatellitePosition[channelNumber - 1] = std::max(
				MIN_SATELLITE_POSITION, std::min(MAX_SATELLITE_POSITION, position));
			}
		case SONG_TYPE_2M:
		case SONG_TYPE_1S:
			if (channelNumber >= 1 && channelNumber <= 2)
			{
				this->channelSatellitePosition[channelNumber - 1] = std::max(
				MIN_SATELLITE_POSITION, std::min(MAX_SATELLITE_POSITION, position));
			}
		default:
			break;
	}
}

void Mixer::setChannelEnabled(int channelNumber, bool enabled)
{
	switch (this->songType)
	{
		case SONG_TYPE_4M:
			if (channelNumber >= 3 && channelNumber <= 4)
			{
				this->channelEnabled[channelNumber - 1] = enabled;
			}
		case SONG_TYPE_2M:
		case SONG_TYPE_1S:
			if (channelNumber >= 1 && channelNumber <= 2)
			{
				this->channelEnabled[channelNumber - 1] = enabled;
			}
		default:
			break;
	}
}

void Mixer::getTrackMap(int * trackMap)
{
	switch (this->songType)
	{
		case SONG_TYPE_4M:
			trackMap[3] = this->trackMap[3];
			trackMap[2] = this->trackMap[2];
		case SONG_TYPE_2M:
		case SONG_TYPE_1S:	// !!!
			trackMap[1] = this->trackMap[1];
			trackMap[0] = this->trackMap[0];
		default:
			break;
	}
}

int Mixer::getChannelSatellitePosition(int channel)
{
	return this->channelSatellitePosition[channel - 1];
}

int Mixer::getChannelEnabled(int channel)
{
	return this->channelEnabled[channel - 1];
}

void Mixer::setTrackChannel(int trackNumber, int channelNumber)
{
	channelNumber = channelNumber - 1;

	switch (this->songType)
	{
		case SONG_TYPE_2M:
		case SONG_TYPE_1S:	// !!!
			if (channelNumber < 0 || channelNumber > 1)
				return;
			if (trackNumber < 0 || trackNumber > 1)
				return;
			break;
		case SONG_TYPE_4M:
			if (channelNumber < 0 || channelNumber > 3)
				return;
			if (trackNumber < 0 || trackNumber > 3)
				return;
			break;
		default:
			return;
	}

	int i;
	for (i = 0; i < 4; i++)
	{
		if (this->trackMap[i] == trackNumber)
			break;
	}
	/*int temp = this->trackMap[channelNumber];
	 this->trackMap[channelNumber] = trackNumber;
	 this->trackMap[i] = temp;*/
	std::swap(this->trackMap[channelNumber], this->trackMap[i]);

	return;
}

/*void Mixer::mix(float ** buffers, float * outLeft, float * outRight, int lenght){
 this->processor->process(buffers, outLeft, outRight, lenght);
 }*/

bool Mixer::loop()
{
	audio::AudioBuffer temp[4], outLeft, outRight;

	// --- Legge il numero di input adeguato ---
	int chansToPull = 0;
	switch (this->songType)
	{
		case SONG_TYPE_4M:
			chansToPull = 4;
			break;
		case SONG_TYPE_2M:
			chansToPull = 2;
			break;
		case SONG_TYPE_1S:
			chansToPull = 2;
			break;
		case SONG_TYPE_1M:
			chansToPull = 1;
			break;
	}

	audio::Status pullStatus;
	for (int i = 0; i < chansToPull; i++)
	{
		pullStatus = inputs[i].waitIfEmpty();

		if (pullStatus == audio::Status::ERROR) {		// se un pullData è fallito per qualche motivo
			LOGD("pull mixer errore");
			return false;		// ritorna false così si interrompe l'elaborazione
		}
	}

	for (int i = 0; i < chansToPull; i++)
		inputs[i].pullData(temp[i]);

	/*for (int i = 0; i < chansToPull; i++)
	{
		//inputs[i].waitIfEmpty();
		pullStatus = inputs[i].pullData(temp[i]);
	}

	if (pullStatus == audio::Status::ERROR)
	{		// se un pullData è fallito per qualche motivo
		LOGD("pull mixer errore");
		return false;		// ritorna false così si interrompe l'elaborazione
	}*/

	// --- Esegue l'elaborazione ---
	this->processor->process(temp, outLeft, outRight);

	// --- Scrive i risultati ---
	auto pushStatusL = outputLeft.pushData(outLeft);
	auto pushStatusR = outputRight.pushData(outRight);
	if (pushStatusL == audio::Status::ERROR || pushStatusR == audio::Status::ERROR)
	{
		LOGD("push mixer non ok");
		return false;
	}

	return true;
}

MixerProcessor::MixerProcessor(Mixer * mixer)
{
	this->mixer = mixer;
	for (unsigned int i = 0; i < silence.size(); i++)
		silence[i] = 0.0f;
}

void MixerProcessor1M::process(audio::AudioBuffer (&buffers)[4], audio::AudioBuffer& outLeft, audio::AudioBuffer& outRight)
{
	auto& inputBuffer = buffers[0];
	for (unsigned int i = 0; i < outLeft.size(); i++)
		outLeft[i] = outRight[i] = inputBuffer[i] * 0.5f;
}

void MixerProcessor1S::process(audio::AudioBuffer (&buffers)[4], audio::AudioBuffer& outLeft, audio::AudioBuffer& outRight)
{
}

int calculateDelay(int pos, float quadDistance)
{
	float fpos = ((float) pos * 0.015f) - HALF_HEAD_WIDTH;
	float diff = sqrt(quadDistance + fpos * fpos + fpos * HEAD_WIDTH * 2.0f + HEAD_WIDTH_QUAD) - sqrt(quadDistance + fpos * fpos);
	return (int) (diff / HEAD_WIDTH * 100.0f);
}

void MixerProcessor2M::process(audio::AudioBuffer (&buffers)[4], audio::AudioBuffer& outLeft, audio::AudioBuffer& outRight)
{

	auto& leftInputBuffer = mixer->channelEnabled[0] ? buffers[mixer->trackMap[0]] : silence;
	auto& rightInputBuffer = mixer->channelEnabled[1] ? buffers[mixer->trackMap[1]] : silence;

	float leftInputSample, rightInputSample;

	int d1 = calculateDelay(100 - mixer->channelSatellitePosition[1], FRONT_DISTANCE_QUAD);
	int d0 = calculateDelay(100 - mixer->channelSatellitePosition[0], FRONT_DISTANCE_QUAD);

	leftDelayBuffer.setDelay(d1);
	rightDelayBuffer.setDelay(d0);

	float leftFrontSignalGain = 1.0f - 0.002f * d1;
	float rightFrontSignalGain = 1.0f - 0.002f * d0;

	for (unsigned int i = 0; i < outLeft.size(); i++)
	{
		leftInputSample = leftInputBuffer[i];
		rightInputSample = rightInputBuffer[i];

		this->rightDelayBuffer.write(leftInputSample);
		this->leftDelayBuffer.write(rightInputSample);

		outLeft[i] = leftInputSample + leftDelayBuffer.read() * leftFrontSignalGain;
		outRight[i] = rightInputSample + rightDelayBuffer.read() * rightFrontSignalGain;
		/*
		 outLeft[i] = leftInputSample * leftFrontSignalGain
		 + leftDelayBuffer.read() * (0.3f + (rightFrontSignalGain - 0.6f) / 0.2f * 0.5f);
		 outRight[i] = rightInputSample * rightFrontSignalGain
		 + rightDelayBuffer.read() * (0.3f + (leftFrontSignalGain - 0.6f) / 0.2f * 0.5f);
		 */
	}
}

void MixerProcessor4M::process(audio::AudioBuffer (&buffers)[4], audio::AudioBuffer& outLeft, audio::AudioBuffer& outRight)
{

	auto& leftFrontInputBuffer = mixer->channelEnabled[0] ? buffers[mixer->trackMap[0]] : silence;
	auto& rightFrontInputBuffer = mixer->channelEnabled[1] ? buffers[mixer->trackMap[1]] : silence;
	auto& leftBackInputBuffer = mixer->channelEnabled[2] ? buffers[mixer->trackMap[2]] : silence;
	auto& rightBackInputBuffer = mixer->channelEnabled[3] ? buffers[mixer->trackMap[3]] : silence;

	int d0 = calculateDelay(100 - mixer->channelSatellitePosition[0], BACK_DISTANCE_QUAD);
	int d1 = calculateDelay(100 - mixer->channelSatellitePosition[1], BACK_DISTANCE_QUAD);
	int d2 = calculateDelay(100 - mixer->channelSatellitePosition[2], BACK_DISTANCE_QUAD);
	int d3 = calculateDelay(100 - mixer->channelSatellitePosition[3], BACK_DISTANCE_QUAD);

	leftFrontDelayBuffer.setDelay(d1);
	rightFrontDelayBuffer.setDelay(d0);
	leftBackDelayBuffer.setDelay(d3);
	rightBackDelayBuffer.setDelay(d2);

	float leftFrontSignalGain = 1.0f - 0.002f * d1;
	float rightFrontSignalGain = 1.0f - 0.002f * d0;
	float leftBackSignalGain = 1.0f - 0.002f * d3;
	float rightBackSignalGain = 1.0f - 0.002f * d2;

	float leftFrontInputSample, rightFrontInputSample, leftBackInputSample, rightBackInputSample;

	for(unsigned int i = 0; i < outLeft.size(); i++)
	{
		leftFrontInputSample = leftFrontInputBuffer[i];
		rightFrontInputSample = rightFrontInputBuffer[i];

		leftBackInputSample = (leftBackInputBuffer[i] * 0.64 + lowPassLeft[0] + lowPassLeft[1]) / 3.0f;
		lowPassLeft[1] = lowPassLeft[0];
		lowPassLeft[0] = leftBackInputBuffer[i];

		rightBackInputSample = (rightBackInputBuffer[i] * 0.64 + lowPassRight[0] + lowPassRight[1]) / 3.0f;
		lowPassRight[1] = lowPassRight[0];
		lowPassRight[0] = rightBackInputBuffer[i];

		float reverbSample = 0.0f;

		for (int j = 0; j < 3; j++)
		{
			float rev = reverb[j].read();
			reverbSample += rev;
			reverb[j].write((leftBackInputSample + rightBackInputSample) * 0.3);
		}

		this->rightFrontDelayBuffer.write(leftFrontInputSample);
		this->leftFrontDelayBuffer.write(rightFrontInputSample);

		this->rightBackDelayBuffer.write(leftBackInputSample);
		this->leftBackDelayBuffer.write(rightBackInputSample);

		outLeft[i] = leftFrontInputSample + leftFrontDelayBuffer.read() * leftFrontSignalGain;
		outRight[i] = rightFrontInputSample + rightFrontDelayBuffer.read() * rightFrontSignalGain;

		outLeft[i] += leftBackInputSample + leftBackDelayBuffer.read() * leftBackSignalGain;
		outRight[i] += rightBackInputSample + rightBackDelayBuffer.read() * rightBackSignalGain;

		outLeft[i] += (reverbSample / 2.0f);
		outRight[i] += (reverbSample / 2.0f);
	}
}

void Mixer::flush()
{
	outputLeft.flush();
	outputRight.flush();
}
