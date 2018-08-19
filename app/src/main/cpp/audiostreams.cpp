#include "audiostreams.h"

#include <mutex>
#include <algorithm>

#include <circularqueue_impl.h>

#include <iostream>

template class CircularQueue<audio::AudioBuffer>;	// Esplicito istanziamento della coda

using namespace audio;

void audio::connect(InputStream &i, OutputStream &o)
{
	i.removeOutput();
	o.removeInput();
	i.attachOutput(o);
	o.attachInput(i);
}

// ####################################################################################################################

InputStream::InputStream(){}
InputStream::~InputStream() {
	removeOutput();
}

bool InputStream::attachOutput(OutputStream &out) {
	if(connectedOutput != nullptr)
		return false;
	connectedOutput = &out;
	return true;
}

// Rimuove l'output associato, e automaticamente avvisa l'input della disconnessione
// withoutNotify a true dice di non avvisare il relativo OutputStream della disconnessione
void InputStream::removeOutput(bool withoutNotify) {
	if(connectedOutput && !withoutNotify)
		connectedOutput->removeInput(true);
	connectedOutput = nullptr;
}

// ritorna true se ci sono dati in attesa di esser ricevuti
bool InputStream::hasData() {
	return connectedOutput ? !connectedOutput->bufferQueue.isEmpty() : false;
}

// Attende finchè non si presentano elementi nello stream.
Status InputStream::waitIfEmpty()
{
	if(!connectedOutput)
		return Status::ERROR;
	return connectedOutput->bufferQueue.waitIfEmpty() ? Status::OK : Status::TIMEOUT;
}

// ####################################################################################################################

OutputStream::OutputStream()
	: bufferQueue(4) {}
OutputStream::~OutputStream() {
	removeInput();
}

bool OutputStream::attachInput(InputStream &in) {
	if(connectedInput != nullptr)
		return false;
	connectedInput = &in;
	bufferQueue.erase();
	return true;
}

// Rimuove l'input associato, e automaticamente avvisa l'input della disconnessione
// withoutNotify a true dice di non avvisare il relativo InputStream della disconnessione
void OutputStream::removeInput(bool withoutNotify) {
	if(connectedInput && !withoutNotify)
		connectedInput->removeOutput(true);
	connectedInput = nullptr;
	bufferQueue.erase();
}

// Ritorna true se la coda è piena
bool OutputStream::isFull() {
	return bufferQueue.isFull();
}

// Attende finchè non si liberano posizioni nello stream.
Status OutputStream::waitIfFull() {
	return bufferQueue.waitIfFull() ? Status::OK : Status::TIMEOUT;
}

void OutputStream::flush(){
	bufferQueue.erase();
}

// ################################################################################################
// ################################################################################################
// ################################################################################################
// ################################################################################################


// Copia un buffer di dati dalla coda, torna false in caso di errore altrimenti true
Status InputStream::pullData(AudioBuffer &destBuffer)
{
	if(!connectedOutput)
		return Status::ERROR;

	const auto* srcBuffer = connectedOutput->bufferQueue.top();

	if(!srcBuffer) {
		std::cout << "[InpStream] Timeout PULLdata" << std::endl;
		return Status::TIMEOUT;
	}

	std::copy(srcBuffer->begin(), srcBuffer->end(), destBuffer.begin());

	connectedOutput->bufferQueue.pop();
	return Status::OK;
}

// inserisce tot dati nello stream, ritorna il numero di dati inviati
Status OutputStream::pushData(const AudioBuffer &srcBuffer)
{
	if(!connectedInput)
		return Status::ERROR;

	auto* destBuffer = bufferQueue.getNewBuffer();

	if(!destBuffer) {
		std::cout << "[OutStream] Timeout PUSHdata" << std::endl;
		return Status::TIMEOUT;
	}

	std::copy(srcBuffer.begin(), srcBuffer.end(), destBuffer->begin());

	bufferQueue.commit();
	return Status::OK;
}
