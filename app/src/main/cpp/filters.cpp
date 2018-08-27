#include "filters.h"

void NAB_3_75_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points) {
    double step = (endFreq - startFreq) / points;

    double t1 = 3180 * pow(10, -6);
    double t2 = 90 * pow(10, -6);

    double f = startFreq;
    double w = 0;

    int index = 0;
    while (f < endFreq) {
        w = 2 * M_PI * f;
        CkFftComplex value;
        value.real = w * t1 * sqrt((1 + pow(w * t2, 2)) / (1 + pow(w * t1, 2)));
        value.imag = 0;
        values[index] = value;

        f = f + step;
        index++;
    }
}

void NAB_7_5_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points) {
    double step = (endFreq - startFreq) / points;

    double t1 = 3180 * pow(10, -6);
    double t2 = 50 * pow(10, -6);

    double f = startFreq;
    double w = 0;

    int index = 0;
    while (f < endFreq) {
        w = 2 * M_PI * f;
        CkFftComplex value;
        value.real = w * t1 * sqrt((1 + pow(w * t2, 2)) / (1 + pow(w * t1, 2)));
        value.imag = 0;
        values[index] = value;

        f = f + step;
        index++;
    }
}

void NAB_15_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points) {
    NAB_7_5_FILTER(values, startFreq, endFreq, points);
}

void NAB_30_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points);

void NAB_30_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points) {
    double step = (endFreq - startFreq) / points;

    //t1=inf
    double t1 = 100000;// 3180*pow(10,-6);
    double t2 = 17.5 * pow(10, -6);

    double f = startFreq;
    double w = 0;

    int index = 0;
    while (f < endFreq) {
        w = 2 * M_PI * f;
        CkFftComplex value;
        value.real = w * t1 * sqrt((1 + pow(w * t2, 2)) / (1 + pow(w * t1, 2)));
        value.imag = 0;
        values[index] = value;

        f = f + step;
        index++;
    }
}

void NAB_3_75_INV_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points) {
    double step = (endFreq - startFreq) / points;

    double t1 = 3180 * pow(10, -6);
    double t2 = 90 * pow(10, -6);

    double f = startFreq;
    double w = 0;

    int index = 0;
    while (f < endFreq) {
        w = 2 * M_PI * f;
        CkFftComplex value;
        value.real = w * t1 * sqrt((1 + pow(w * t2, 2)) / (1 + pow(w * t1, 2))) * -1.0;
        value.imag = 0;
        values[index] = value;

        f = f + step;
        index++;
    }
}

void NAB_7_5_INV_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points) {
    double step = (endFreq - startFreq) / points;

    double t1 = 3180 * pow(10, -6);
    double t2 = 50 * pow(10, -6);

    double f = startFreq;
    double w = 0;

    int index = 0;
    while (f < endFreq) {
        w = 2 * M_PI * f;
        CkFftComplex value;
        value.real = w * t1 * sqrt((1 + pow(w * t2, 2)) / (1 + pow(w * t1, 2))) * -1.0;
        value.imag = 0;
        values[index] = value;

        f = f + step;
        index++;
    }
}

void NAB_15_INV_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points) {
    NAB_7_5_INV_FILTER(values, startFreq, endFreq, points);
}

void NAB_30_INV_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points) {
    double step = (endFreq - startFreq) / points;

    //t1=inf
    double t1 = 100000;// 3180*pow(10,-6);
    double t2 = 90 * pow(10, -6);

    double f = startFreq;
    double w = 0;

    int index = 0;
    while (f < endFreq) {
        w = 2 * M_PI * f;
        CkFftComplex value;
        value.real = w * t1 * sqrt((1 + pow(w * t2, 2)) / (1 + pow(w * t1, 2))) * -1.0;
        value.imag = 0;
        values[index] = value;

        f = f + step;
        index++;
    }
}

void CCIR_3_75_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points) {
    NAB_3_75_FILTER(values, startFreq, endFreq, points);
}

void CCIR_7_5_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points) {
    double step = (endFreq - startFreq) / points;

    //t1=inf
    double t1 = 100000;// 3180*pow(10,-6);
    double t2 = 70 * pow(10, -6);

    double f = startFreq;
    double w = 0;

    int index = 0;
    while (f < endFreq) {
        w = 2 * M_PI * f;
        CkFftComplex value;
        value.real = w * t1 * sqrt((1 + pow(w * t2, 2)) / (1 + pow(w * t1, 2)));
        value.imag = 0;
        values[index] = value;

        f = f + step;
        index++;
    }
}

void CCIR_15_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points) {
    double step = (endFreq - startFreq) / points;

    //t1=inf
    double t1 = 100000;// 3180*pow(10,-6);
    double t2 = 35 * pow(10, -6);

    double f = startFreq;
    double w = 0;

    int index = 0;
    while (f < endFreq) {
        w = 2 * M_PI * f;
        CkFftComplex value;
        value.real = w * t1 * sqrt((1 + pow(w * t2, 2)) / (1 + pow(w * t1, 2)));
        value.imag = 0;
        values[index] = value;

        f = f + step;
        index++;
    }
}

void CCIR_30_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points) {
    NAB_30_FILTER(values, startFreq, endFreq, points);
}

void CCIR_3_75_INV_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points) {
    double step = (endFreq - startFreq) / points;

    double t1 = 3180 * pow(10, -6);
    double t2 = 90 * pow(10, -6);

    double f = startFreq;
    double w = 0;

    int index = 0;
    while (f < endFreq) {
        w = 2 * M_PI * f;
        CkFftComplex value;
        value.real = w * t1 * sqrt((1 + pow(w * t2, 2)) / (1 + pow(w * t1, 2))) * -1.0;
        value.imag = 0;
        values[index] = value;

        f = f + step;
        index++;
    }
}

void CCIR_7_5_INV_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points) {
    double step = (endFreq - startFreq) / points;

    //t1=inf
    double t1 = 100000;// 3180*pow(10,-6);
    double t2 = 70 * pow(10, -6);

    double f = startFreq;
    double w = 0;

    int index = 0;
    while (f < endFreq) {
        w = 2 * M_PI * f;
        CkFftComplex value;
        value.real = w * t1 * sqrt((1 + pow(w * t2, 2)) / (1 + pow(w * t1, 2))) * -1.0;
        value.imag = 0;
        values[index] = value;

        f = f + step;
        index++;
    }
}

void CCIR_15_INV_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points) {
    double step = (endFreq - startFreq) / points;

    //t1=inf
    double t1 = 100000;// 3180*pow(10,-6);
    double t2 = 35 * pow(10, -6);

    double f = startFreq;
    double w = 0;

    int index = 0;
    while (f < endFreq) {
        w = 2 * M_PI * f;
        CkFftComplex value;
        value.real = w * t1 * sqrt((1 + pow(w * t2, 2)) / (1 + pow(w * t1, 2))) * -1.0;
        value.imag = 0;
        values[index] = value;

        f = f + step;
        index++;
    }
}

void CCIR_30_INV_FILTER(CkFftComplex *values, int startFreq, int endFreq, int points) {
    NAB_30_INV_FILTER(values, startFreq, endFreq, points);
}
