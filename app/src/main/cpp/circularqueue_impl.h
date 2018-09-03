#ifndef CIRCULARQUEUE_IMPL_H
#define CIRCULARQUEUE_IMPL_H

#include <circularqueue.h>
#include <unistd.h>
#include "log.h"

#define WAIT_TIMEOUT_USEC (1000)

template<typename T>
CircularQueue<T>::CircularQueue(int bufferLen)
        : queue(bufferLen >= 2 ? bufferLen
                               : 2) {}    // Deve avere almeno 2 buffer nella coda per funzionare correttamente

template<typename T>
CircularQueue<T>::~CircularQueue() {}

// True se la coda è vuota (non ci sono elementi da leggere)
template<typename T>
bool CircularQueue<T>::isEmpty() {
    return readIndex == writeIndex;
}

// True se la coda è piena (non ci sono posizioni in cui scrivere)
template<typename T>
bool CircularQueue<T>::isFull() {
    return (writeIndex + 1) % queue.size() == readIndex;
    // in realtà rimarrebbe un buchino libero (quello correntemente puntato da writeIndex),
    // però così è più facile da gestire.
}

// Attende fintanto che la coda è piena, ritorna false in caso di timeout o true se nella coda c'i sono elementi c'è spazio
template<typename T>
bool CircularQueue<T>::waitIfFull() {
    if (isFull()) {
        usleep(WAIT_TIMEOUT_USEC);
        return false;
    }
    return true;
}

// Attende fintanto che la coda è quote; ritorna false in caso di timeout o true se nella coda ci sono elementi
template<typename T>
bool CircularQueue<T>::waitIfEmpty() {
    // Deve attendere che avvenga una scrittura in caso la coda sia vuota
    if (isEmpty()) {
        usleep(WAIT_TIMEOUT_USEC);
        return false;
    }
    return true;
}

// Incrementa il valore in modulo dimensione della coda
template<typename T>
int CircularQueue<T>::incrementWrap(int val) {
    return (val + 1) % queue.size();
}

// Elimina tutti gli elementi e svuota la coda
template<typename T>
void CircularQueue<T>::erase() {
    readIndex = 0;
    writeIndex = 0;
}

// Ritorna l'elemento in testa; se non è disponibile uno, attende finchè non
// ci sono ulteriori dati
template<typename T>
const T *CircularQueue<T>::top() {
    return waitIfEmpty() ? &queue[readIndex] : nullptr;
}

// Libera il buffer di testa e si posiziona sul prossimo
template<typename T>
void CircularQueue<T>::pop() {
    if (!isEmpty()) {
        {
            std::lock_guard<std::mutex> lg(readMutex);
            readIndex = incrementWrap(readIndex);
        }
    }
}

// Prende un nuovo buffer pulito su cui scrivere; se non è disponibile uno,
// attende finchè qualcuno non si libera
template<typename T>
T *CircularQueue<T>::getNewBuffer() {
    return waitIfFull() ? &queue[writeIndex] : nullptr;
}

// Dice che il buffer preso con getNew è stato completamente scritto e quindi
// può venir letto dal consumatore
template<typename T>
void CircularQueue<T>::commit() {
    if (!isFull()) {
        {
            std::lock_guard<std::mutex> lg(writeMutex);
            writeIndex = incrementWrap(writeIndex);
        }
    }
}

#undef WAIT_TIMEOUT_MILLISEC
#endif
