#ifndef CIRCULARQUEUE_H
#define CIRCULARQUEUE_H

#include <vector>
#include <mutex>
#include <condition_variable>

/**
 * CircularQueue: coda circolare per applicazioni multithread Single-Producer, Single-Consumer
 */
template<typename T>
class CircularQueue {
public:

    explicit CircularQueue(int bufferLen);

    ~CircularQueue();

    // True se la coda è vuota (non ci sono elementi da leggere)
    bool isEmpty();

    // True se la coda è piena (non ci sono posizioni in cui scrivere)
    bool isFull();

    // Attende fintanto che la coda è piena, ritorna false in caso di timeout o true se nella coda c'i sono elementi c'è spazio
    bool waitIfFull();

    // Attende fintanto che la coda è quote; ritorna false in caso di timeout o true se nella coda ci sono elementi
    bool waitIfEmpty();

    // Ritorna l'elemento in testa; se non è disponibile uno, attende finchè non
    // ci sono ulteriori dati.
    // Ritorna nullptr se è scaduto il timeout per l'attesa
    const T *top();

    // Libera il buffer di testa e si posiziona sul prossimo
    void pop();

    // Prende un nuovo buffer pulito su cui scrivere; se non è disponibile uno,
    // attende finchè qualcuno non si libera.
    // Ritorna nullptr se è scaduto il timeout per l'attesa
    T *getNewBuffer();

    // Dice che il buffer preso con getNew è stato completamente scritto e quindi
    // può venir letto dal consumatore
    void commit();

    // Elimina tutti gli elementi e svuota la coda
    void erase();

private:
    // Nota sugli indici: readIndex punta all'elemento che deve essere letto,
    // mentre writeIndex punta all'ultimo elemento scritto, e il prossimo elemento
    // vuoto su cui scrivere è writeIndex+1.
    // Praticamente funziona come uno stack: in lettura si legge da readIndex e
    // DOPO si incrementa, mentre in scrittura PRIMA si incrementa e poi si scrive.
    std::vector<T> queue;
    int readIndex = 0;
    std::mutex readMutex;
    int writeIndex = 0;
    std::mutex writeMutex;

    int incrementWrap(int val);
};

#endif // CIRCULARQUEUE_H
