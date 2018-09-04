
#include "fftconvolver.h"

#include "log.h"

#include "ckfft/ckfft.h"
#include <algorithm>
#include <vector>

#include <iostream>

const int blockSize = audio::AudioBufferSize;


FFTConvolver::FFTConvolver()
        : extendedBlock(2 * blockSize),        // blocco in ingresso esteso con zeri
          transformedBlock(blockSize + 1),    // extendedBlock passato nella FFT
          filteredBlock(blockSize * 2),        // blocco filtrato e antitrasformato
          fftTmp(blockSize + 1),                // temporaneo per le operazioni di FFT/IFFT
          overflowData(blockSize, 0.0f),    // pezzo avanzato dall'ultimo blocco elaborato
          filter(blockSize +
                 1),                // risposta in frequenza del filtro, da usare per la convoluzione
          filterLoaded(false) {
}

/**
 * Costruttore. Inizializza il filtro come se fosse chiamato setFilter().
 */
FFTConvolver::FFTConvolver(float *filterData, int filterLen)
        : FFTConvolver() {
    setFilter(filterData, filterLen);
}

FFTConvolver::~FFTConvolver() {
    CkFftShutdown(fftContext);
}

/**
 * Imposta il filtro.
 * Il filtro è sempre lungo audio::AudioBuffer().size(); filterLen può essere
 * minore di quel valore, in tal caso il resto del filtro sarà riempito con 0.
 * Se filterLen è maggiore, i dati aggiuntivi saranno ignorati.
 * @param filterData filtro da applicare (è copiato internamente)
 * @param filterLen lunghezza del filtro (come descritto sopra)
 */
void FFTConvolver::setFilter(float *filterData, int filterLen) {
    if (fftContext == nullptr)
        fftContext = CkFftInit(2 * blockSize, kCkFftDirection_Both, NULL, NULL);

    for (int i = 0; i < filterLen; i++) {
        filter[i].real = filterData[i];
        filter[i].imag = 0.0f;
    }

    filterLoaded = true;
}

/**
 * resetta lo stato interno p.es. per iniziare un nuovo segnale
 */
void FFTConvolver::reset() {
    std::fill(overflowData.begin(), overflowData.end(), 0.0f);
    std::fill(inputBuffers[0].begin(), inputBuffers[0].end(), 0.0f);
    std::fill(inputBuffers[1].begin(), inputBuffers[1].end(), 0.0f);
    output.flush();
    currentBuffer = 0;
}

/**
 * loop() da FilterBase
 */
bool FFTConvolver::loop() {
    auto stat = output.waitIfFull();
    if (stat == audio::Status::ERROR) {
        __android_log_print(ANDROID_LOG_ERROR, "ERROR", "FFTConvolver: pushData output NOT ok");
        return false;
    }
    if (stat == audio::Status::TIMEOUT)
        return true;

    stat = input.waitIfEmpty();
    if (stat == audio::Status::ERROR) {
        __android_log_print(ANDROID_LOG_ERROR, "ERROR", "FFTConvolver: pullData input NOT ok");
        return false;
    }
    if (stat == audio::Status::TIMEOUT)
        return true;

    audio::AudioBuffer resultBuffer;
    int oldBuffer = currentBuffer;
    int newBuffer = 1 - currentBuffer;

    auto pullStatus = input.pullData(inputBuffers[newBuffer]);

    if (pullStatus == audio::Status::ERROR)
        return false;
    else if (pullStatus ==
             audio::Status::TIMEOUT) {        // Non possiamo continuare questa iterazione, ma continua a ciclare il filtro
        //LOGD("[FFTConvlv] Timeout PULLdata");
        return true;
    }

    filterBlock(inputBuffers[oldBuffer], inputBuffers[newBuffer], resultBuffer);

    output.pushData(resultBuffer);

    currentBuffer = newBuffer;
    return true;
}

/**
 *
 * @param prevBlock il blocco di dati precedente a block
 * @param block il blocco di dati da elaborare
 * @param result il blocco risultante
 */
void FFTConvolver::filterBlock(audio::AudioBuffer &prevBlock, audio::AudioBuffer &block,
                               audio::AudioBuffer &result) {
    // Prendi in ingresso 2 blocchi di dati
    std::copy(prevBlock.begin(), prevBlock.end(), extendedBlock.begin());
    std::copy(block.begin(), block.end(), extendedBlock.begin() + prevBlock.size());

    // Trasforma l'input
    CkFftRealForward(fftContext, extendedBlock.size(), extendedBlock.data(),
                     transformedBlock.data());

    // Convoluzione nel tempo tramite moltiplicazione in frequenza
    for (unsigned int i = 0; i < filter.size(); i++) {
        auto f = filter[i];
        auto b = transformedBlock[i];
        transformedBlock[i].real = b.real * f.real - b.imag * f.imag;    // prodotto complesso
        transformedBlock[i].imag = b.real * f.imag + b.imag * f.real;
    }

    // Si antitrasforma il risultato ottenendo blockSize*2 valori, metà di questi sono della finestra corrente e l'altra metà
    // sono da aggiungere alla finestra successiva.
    CkFftRealInverse(fftContext, filteredBlock.size(), transformedBlock.data(),
                     filteredBlock.data(), fftTmp.data());

    // la trasformazione inserisce uno scalamento pari alla lunghezza del blocco trasformato,
    // quindi dobbiamo toglierlo sia dopo la FFT che dopo la IFFT
    float invTransformScale = 1.0f / (block.size() * 4.0f);

    for (unsigned int i = 0; i < result.size(); i++) {
        float weight = float(i) / block.size();
        result[i] = filteredBlock[i] * weight + overflowData[i] * (1.0f - weight);
        result[i] *= invTransformScale;
    }

    // Copio la seconda metà di filteredBlock nel buffer di overflow così viene poi sommato al prossimo blocco elaborato
    std::copy(filteredBlock.begin() + block.size(), filteredBlock.end(), overflowData.begin());
}
