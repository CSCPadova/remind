#include "QuadraphonicFilter.h"

QuadraphonicFilter::QuadraphonicFilter(int sampleRate)
	: reverberLeft(sampleRate), reverberRight(sampleRate)
{
}

QuadraphonicFilter::~QuadraphonicFilter() {
}

void QuadraphonicFilter::process(const QuadStream stream, audio::AudioBuffer &outLeft, audio::AudioBuffer &outRight)
{
	float outSampleLeft;
	float outSampleRight;

	this->reverberLeft.process(stream.inBehindLeft, stream.inBehindLeft);
	this->reverberRight.process(stream.inBehindRight, stream.inBehindRight);

	for(unsigned int i = 0; i<outLeft.size(); i++) {
		outSampleLeft  = stream.inFrontLeft[i]  + stream.inBehindLeft[i];
		outSampleRight = stream.inFrontRight[i] + stream.inBehindRight[i];
		outLeft[i]  = outSampleLeft;
		outRight[i] = outSampleRight;
	}
}
