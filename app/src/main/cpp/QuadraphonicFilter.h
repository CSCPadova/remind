#ifndef QUADRAPHONICFILTER_H
#define QUADRAPHONICFILTER_H

#include "Reverber.h"
#include <audiostreams.h>

struct QuadStream {
	audio::AudioBuffer & inFrontLeft;
	audio::AudioBuffer & inFrontRight;
	audio::AudioBuffer & inBehindLeft;
	audio::AudioBuffer & inBehindRight;
};

class QuadraphonicFilter {

	private:
		Reverber reverberLeft;
		Reverber reverberRight;

	public:
		QuadraphonicFilter(int sampleRate);
		~QuadraphonicFilter();

		void process(const QuadStream stream, audio::AudioBuffer & outLeft, audio::AudioBuffer & outRight);

};
#endif
