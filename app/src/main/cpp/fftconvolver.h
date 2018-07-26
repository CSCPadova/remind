#ifndef FFTCONVOLVER_H
#define FFTCONVOLVER_H

#include <filterbase.h>
#include <audiostreams.h>

#include <ckfft/ckfft.h>

/**
 * Filtro che applica una FFT al segnale di ingresso.
 */
class FFTConvolver : public FilterBase
{
	public:
		audio::InputStream  input;
		audio::OutputStream output;

		FFTConvolver();
		explicit FFTConvolver(float *filterData, int filterLen);
		~FFTConvolver();

		void setFilter(float *filterData, int filterLen);

		void reset();		// resetta lo stato interno p.es. per iniziare un nuovo segnale

	private:
		bool loop();	// FilterBase

		CkFftContext* fftContext = nullptr;			// contesto CricketFFT per fare la FFT e IFFT

		void filterBlock(audio::AudioBuffer& prevBlock, audio::AudioBuffer& block, audio::AudioBuffer& result);

		std::vector<float> extendedBlock;	// Buffer temporanei per filterBlock (messi qua per non doverli ricreare per ogni blocco)
		std::vector<CkFftComplex> transformedBlock;
		std::vector<float> filteredBlock;
		std::vector<CkFftComplex> fftTmp;

		std::vector<float> overflowData;	// dati che avanzano dall'elaborazione precedente
		std::vector<CkFftComplex> filter;	// il filtro da applicare (inizializzato durante la costruzione)

		audio::AudioBuffer inputBuffers[2];
		int currentBuffer = 0;
		bool filterLoaded = false;			// false se non è mai stato caricato un filtro valido, true altrimenti.
};

#endif // FFTCONVOLVER_H
