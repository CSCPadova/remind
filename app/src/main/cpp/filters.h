#ifndef FILTERS_H
#define FILTERS_H

#include <cmath>
#include <ckfft.h>

static const float DEFAULT_FILTER_VALUE=1.0f;

void FILTER(float *values, double t1, double t2, int endFreq, int len);

void INV_FILTER(float *values, double t1, double t2, int endFreq, int len);

void NAB_3_75_FILTER(float *values, int endFreq, int len);

void NAB_7_5_FILTER(float *values, int endFreq, int len);

void NAB_15_FILTER(float *values, int endFreq, int len);

void NAB_30_FILTER(float *values, int endFreq, int len);

void NAB_3_75_INV_FILTER(float *values, int endFreq, int len);

void NAB_7_5_INV_FILTER(float *values, int endFreq, int len);

void NAB_15_INV_FILTER(float *values, int endFreq, int len);

void NAB_30_INV_FILTER(float *values, int endFreq, int len);

void CCIR_3_75_FILTER(float *values, int endFreq, int len);

void CCIR_7_5_FILTER(float *values, int endFreq, int len);

void CCIR_15_FILTER(float *values, int endFreq, int len);

void CCIR_30_FILTER(float *values, int endFreq, int len);

void CCIR_3_75_INV_FILTER(float *values, int endFreq, int len);

void CCIR_7_5_INV_FILTER(float *values, int endFreq, int len);

void CCIR_15_INV_FILTER(float *values, int endFreq, int len);

void CCIR_30_INV_FILTER(float *values, int endFreq, int len);

#endif //FILTERS_H