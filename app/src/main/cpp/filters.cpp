#include "filters.h"

/*
 * Nei nastri veri in registrazione si applica il filtro e in riporduzione lo si toglie
 * La formula toglie l'eq dal nastro e in questo codice il punto di vista e' quello di riproduzione.
 * Per cui:
 * INV_FILTER toglie l'eq del nastro (formula senza il meno 1)
 * FILTER applica l'eq al nastro (formula con il meno 1)
 */

void FILTER(float *values, double t1, double t2, int startFreq, int endFreq, int points) {
    float step = (endFreq - startFreq) / points;
    float f = startFreq;
    float w = 0;
    int index = 0;
    while (index<points) {
        w = 2 * M_PI * f;
        CkFftComplex value;
        values[index] = w * t1 * sqrt((1 + pow(w * t2, 2)) / (1 + pow(w * t1, 2)))*-1;
        f = f + step;
        index++;
    }
}

void INV_FILTER(float *values, double t1, double t2, int startFreq, int endFreq, int points) {
    float step = (endFreq - startFreq) / points;
    float f = startFreq;
    float w = 0;
    int index = 0;
    while (index<points) {
        w = 2 * M_PI * f;
        CkFftComplex value;
        values[index] = w * t1 * sqrt((1 + pow(w * t2, 2)) / (1 + pow(w * t1, 2)));
        f = f + step;
        index++;
    }
}

void NAB_3_75_FILTER(float *values, int startFreq, int endFreq, int points) {
    double t1 = 3180 * pow(10, -6);
    double t2 = 90 * pow(10, -6);
    FILTER(values, t1, t2, startFreq, endFreq, points);
}

void NAB_7_5_FILTER(float *values, int startFreq, int endFreq, int points) {
    double t1 = 3180 * pow(10, -6);
    double t2 = 50 * pow(10, -6);
    FILTER(values, t1, t2, startFreq, endFreq, points);
}

void NAB_15_FILTER(float *values, int startFreq, int endFreq, int points) {
    double t1 = 3180 * pow(10, -6);
    double t2 = 50 * pow(10, -6);
    FILTER(values, t1, t2, startFreq, endFreq, points);
}

void NAB_30_FILTER(float *values, int startFreq, int endFreq, int points) {
    //t1=inf
    double t1 = 1000000;
    double t2 = 17.5 * pow(10, -6);
    FILTER(values, t1, t2, startFreq, endFreq, points);
}

void NAB_3_75_INV_FILTER(float *values, int startFreq, int endFreq, int points) {
    double t1 = 3180 * pow(10, -6);
    double t2 = 90 * pow(10, -6);
    INV_FILTER(values, t1, t2, startFreq, endFreq, points);
}

void NAB_7_5_INV_FILTER(float *values, int startFreq, int endFreq, int points) {
    double t1 = 3180 * pow(10, -6);
    double t2 = 50 * pow(10, -6);
    INV_FILTER(values, t1, t2, startFreq, endFreq, points);
}

void NAB_15_INV_FILTER(float *values, int startFreq, int endFreq, int points) {
    double t1 = 3180 * pow(10, -6);
    double t2 = 50 * pow(10, -6);
    INV_FILTER(values, t1, t2, startFreq, endFreq, points);
}

void NAB_30_INV_FILTER(float *values, int startFreq, int endFreq, int points) {
    //t1=inf
    double t1 = 1000000;
    double t2 = 17.5 * pow(10, -6);
    INV_FILTER(values, t1, t2, startFreq, endFreq, points);
}

void CCIR_3_75_FILTER(float *values, int startFreq, int endFreq, int points) {
    double t1 = 3180 * pow(10, -6);
    double t2 = 90 * pow(10, -6);
    FILTER(values, t1, t2, startFreq, endFreq, points);
}

void CCIR_7_5_FILTER(float *values, int startFreq, int endFreq, int points) {
    //t1=inf
    double t1 = 1000000;
    double t2 = 70 * pow(10, -6);
    FILTER(values, t1, t2, startFreq, endFreq, points);
}

void CCIR_15_FILTER(float *values, int startFreq, int endFreq, int points) {
    //t1=inf
    double t1 = 1000000;
    double t2 = 35 * pow(10, -6);
    FILTER(values, t1, t2, startFreq, endFreq, points);
}

void CCIR_30_FILTER(float *values, int startFreq, int endFreq, int points) {
    //t1=inf
    double t1 = 1000000;
    double t2 = 17.5 * pow(10, -6);
    FILTER(values, t1, t2, startFreq, endFreq, points);
}

void CCIR_3_75_INV_FILTER(float *values, int startFreq, int endFreq, int points) {
    double t1 = 3180 * pow(10, -6);
    double t2 = 90 * pow(10, -6);
    INV_FILTER(values, t1, t2, startFreq, endFreq, points);
}

void CCIR_7_5_INV_FILTER(float *values, int startFreq, int endFreq, int points) {
    //t1=inf
    double t1 = 1000000;
    double t2 = 70 * pow(10, -6);
    INV_FILTER(values, t1, t2, startFreq, endFreq, points);
}

void CCIR_15_INV_FILTER(float *values, int startFreq, int endFreq, int points) {
    //t1=inf
    double t1 = 1000000;
    double t2 = 35 * pow(10, -6);
    INV_FILTER(values, t1, t2, startFreq, endFreq, points);
}

void CCIR_30_INV_FILTER(float *values, int startFreq, int endFreq, int points) {
    //t1=inf
    double t1 = 1000000;
    double t2 = 17.5 * pow(10, -6);
    INV_FILTER(values, t1, t2, startFreq, endFreq, points);
}
