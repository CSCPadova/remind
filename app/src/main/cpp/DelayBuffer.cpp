#include "DelayBuffer.h"
#include "log.h"

DelayBuffer::DelayBuffer(int size) :
		buffer(size, 0.0f)
{
	this->i = 0;
	this->f = buffer.size() - 2;
	this->printDelay();
}

DelayBuffer::~DelayBuffer() {
}

void DelayBuffer::write(float element) {
	this->buffer[this->f] = element;
	this->f = (this->f + 1) % this->buffer.size();
	//this->printDelay();
}

void DelayBuffer::printDelay() {
	if (this->f >= this->i)
		LOGD("delay: %d size: %d", (this->f - this->i),this->buffer.size());
	else
		LOGD("delay: %d size: %d", (this->f + this->buffer.size() - this->i),this->buffer.size());
}

void DelayBuffer::setDelay(int percentage) {
	if (percentage < 0 || percentage > 100)
		return;
	float coeff = (float) percentage / 100.0f;
	int inc = (int) ((float)(this->buffer.size() - 2) *  coeff);
	this->f = (this->i + inc) % this->buffer.size();
	//this->printDelay();
}

float DelayBuffer::read() {
	float element = this->buffer[this->i];
	this->i = (this->i + 1) % this->buffer.size();
	return element;
}
