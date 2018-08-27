#ifndef FILTERS_H
#define FILTERS_H

#include <cmath>
#include <ckfft.h>

void NAB_3_75_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points);

void NAB_7_5_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points);

void NAB_15_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points);

void NAB_30_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points);

void NAB_3_75_INV_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points);

void NAB_7_5_INV_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points);

void NAB_15_INV_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points);

void NAB_30_INV_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points);

void CCIR_3_75_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points);

void CCIR_7_5_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points);

void CCIR_15_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points);

void CCIR_30_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points);

void CCIR_3_75_INV_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points);

void CCIR_7_5_INV_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points);

void CCIR_15_INV_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points);

void CCIR_30_INV_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points);

#endif //FILTERS_H