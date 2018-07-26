#ifndef DELAY_BUFFER_H
#define DELAY_BUFFER_H

#include <vector>

class DelayBuffer {
public:
	DelayBuffer(int sampleRate);
	~DelayBuffer();

	float read();
	void write(float element);
	void setDelay(int percentage);
	void printDelay();
private:
	//float * buffer;
	//int size;
	std::vector<float> buffer;
	int i;
	int f;

};
#endif
