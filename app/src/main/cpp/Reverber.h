/*
 * Reverber
 *
 * Questa classe rappresenta l'oggetto Reverber che si occupa
 * di processare un segnale audio in formato PCM a frequenza
 * scelta
 *
 */
#ifndef REVERBER_H
#define REVERBER_H

#include <audiostreams.h>
#include <DelayBuffer.h>
#include <vector>

class Reverber
{
	private:
		DelayBuffer delayBuffer;

	public:
		Reverber(int sampleRate);
		~Reverber();
		void process(const audio::AudioBuffer& input, audio::AudioBuffer& output);
};
#endif
