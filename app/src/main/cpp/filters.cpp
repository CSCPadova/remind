#include "filters.h"

/*
 * Nei nastri veri in registrazione si applica il filtro e in riporduzione lo si toglie
 * La formula toglie l'eq dal nastro e in questo codice il punto di vista e' quello di riproduzione.
 * Per cui:
 * INV_FILTER toglie l'eq del nastro (formula senza il meno 1)
 * FILTER applica l'eq al nastro (formula con il meno 1)
 */

void FILTER(float *values, double t1, double t2, int endFreq, int len) {
    float step = endFreq / len;
    float f = 0;
    float w = 0;
    int index = 0;
    while (index < len) {
        w = 2 * M_PI * f;
        if (f > 20.0f && f < 20000.0f) {
            values[index] = (w * t1 * sqrt((1 + pow(w * t2, 2)) / (1 + pow(w * t1, 2)))) * -1;
        } else {
            values[index] = DEFAULT_FILTER_VALUE;
        }
        f = f + step;
        index++;
    }
}

void INV_FILTER(float *values, double t1, double t2, int endFreq, int len) {
    float step = (float) (endFreq) / (float) (len);
    float f = 0;
    float w = 0;
    int index = 0;

    while (index < len) {
        w = 2 * M_PI * f;
        if (f > 20.0f && f < 20000.0f) {
            values[index] = w * t1 * sqrt((1 + pow(w * t2, 2)) / (1 + pow(w * t1, 2)));
        } else {
            values[index] = DEFAULT_FILTER_VALUE;
        }
        f = f + step;
        index++;
    }
}

void NAB_3_75_FILTER(float *values, int endFreq, int len) {
    double t1 = 3180 * pow(10, -6);
    double t2 = 90 * pow(10, -6);
    FILTER(values, t1, t2, endFreq, len);
}

void NAB_7_5_FILTER(float *values, int endFreq, int len) {
    double t1 = 3180 * pow(10, -6);
    double t2 = 50 * pow(10, -6);
    FILTER(values, t1, t2, endFreq, len);
}

void NAB_15_FILTER(float *values, int endFreq, int len) {
    double t1 = 3180 * pow(10, -6);
    double t2 = 50 * pow(10, -6);
    FILTER(values, t1, t2, endFreq, len);
}

void NAB_30_FILTER(float *values, int endFreq, int len) {
    //t1=inf
    double t1 = 1000000;
    double t2 = 17.5 * pow(10, -6);
    FILTER(values, t1, t2, endFreq, len);
}

void NAB_3_75_INV_FILTER(float *values, int endFreq, int len) {
    double t1 = 3180 * pow(10, -6);
    double t2 = 90 * pow(10, -6);
    INV_FILTER(values, t1, t2, endFreq, len);
}

void NAB_7_5_INV_FILTER(float *values, int endFreq, int len) {
    double t1 = 3180 * pow(10, -6);
    double t2 = 50 * pow(10, -6);
    INV_FILTER(values, t1, t2, endFreq, len);
}

void NAB_15_INV_FILTER(float *values, int endFreq, int len) {
    double t1 = 3180 * pow(10, -6);
    double t2 = 50 * pow(10, -6);
    INV_FILTER(values, t1, t2, endFreq, len);
}

void NAB_30_INV_FILTER(float *values, int endFreq, int len) {
    //t1=inf
    double t1 = 1000000;
    double t2 = 17.5 * pow(10, -6);
    INV_FILTER(values, t1, t2, endFreq, len);
}

void CCIR_3_75_FILTER(float *values, int endFreq, int len) {
    double t1 = 3180 * pow(10, -6);
    double t2 = 90 * pow(10, -6);
    FILTER(values, t1, t2, endFreq, len);
}

void CCIR_7_5_FILTER(float *values, int endFreq, int len) {
    //t1=inf
    double t1 = 1000000;
    double t2 = 70 * pow(10, -6);
    FILTER(values, t1, t2, endFreq, len);
}

void CCIR_15_FILTER(float *values, int endFreq, int len) {
    //t1=inf
    double t1 = 1000000;
    double t2 = 35 * pow(10, -6);
    FILTER(values, t1, t2, endFreq, len);
}

void CCIR_30_FILTER(float *values, int endFreq, int len) {
    //t1=inf
    double t1 = 1000000;
    double t2 = 17.5 * pow(10, -6);
    FILTER(values, t1, t2, endFreq, len);
}

void CCIR_3_75_INV_FILTER(float *values, int endFreq, int len) {
    double t1 = 3180 * pow(10, -6);
    double t2 = 90 * pow(10, -6);
    INV_FILTER(values, t1, t2, endFreq, len);
}

void CCIR_7_5_INV_FILTER(float *values, int endFreq, int len) {
    //t1=inf
    double t1 = 1000000;
    double t2 = 70 * pow(10, -6);
    INV_FILTER(values, t1, t2, endFreq, len);
}

void CCIR_15_INV_FILTER(float *values, int endFreq, int len) {
    //t1=inf
    double t1 = 1000000;
    double t2 = 35 * pow(10, -6);
    INV_FILTER(values, t1, t2, endFreq, len);
}

void CCIR_30_INV_FILTER(float *values, int endFreq, int len) {
    //t1=inf
    double t1 = 1000000;
    double t2 = 17.5 * pow(10, -6);
    INV_FILTER(values, t1, t2, endFreq, len);
}
