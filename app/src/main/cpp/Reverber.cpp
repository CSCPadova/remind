#include "Reverber.h"


Reverber::Reverber(int sampleRate)
	: delayBuffer(sampleRate)
{
}

Reverber::~Reverber() {
}

void Reverber::process(const audio::AudioBuffer &input, audio::AudioBuffer& output)
{
	float reverbAttenuation = 0.9999f;
	float signalAttenuation = 0.99999f;

	for(unsigned int i=0; i<input.size(); i++) {
		float storedSample = this->delayBuffer.read();

		float inputSample = input[i];
		float outputSample = (inputSample * signalAttenuation + storedSample) / 2.0f;

		this->delayBuffer.write(outputSample * reverbAttenuation);
		output[i] = outputSample;
	}
}
