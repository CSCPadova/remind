#include "RateConverter.h"

RateConverter::RateConverter(SongSpeed songSpeed, int nStreams) {
	int error;
	this->state = src_new(SRC_SINC_FASTEST, nStreams, &error);

	if (state == NULL) {
		LOGE("RateConverter: Errore: %s", src_strerror(error));
	}

	this->nStreams = nStreams;
	LOGD("tracce rate converter: %d", nStreams);
	originalSongSpeed = songSpeed;
	currentSpeed = originalSongSpeed;
	src_set_ratio(state, ratio);
}

RateConverter::~RateConverter() {
	src_delete(this->state);
	scheduleStop();
	join();
}

void RateConverter::run() {
	FilterBase::run();
}

// Attende che il thread di esecuzione si fermi (deve essere stato già chiamato scheduleStop())
void RateConverter::join() {
	FilterBase::join();
}

void RateConverter::execute() {
	FilterBase::execute();
}

/*
 * TODO Mancano tutti i controlli di validità sui push e pull
 * 	-Marco
 */
bool RateConverter::loop()
{
	//audio::Status pullStatus;		<-- LUCA! Queste cose servono a qualcosa sai? Si devono CONTROLLARE

	int i;
	for (i = 0; i < nStreams; i++)
		outStreams[i].waitIfFull();

	data.src_ratio = ratio;

	while (offP >= audio::AudioBufferSize)
	{
		for (int k = 0; k < nStreams; k++) {
			for (i = 0; i < audio::AudioBufferSize; i++)
				buffersOut[k].data()[i] = prodotti[i * nStreams + k];

			outStreams[k].pushData(buffersOut[k]);
		}

		for (i = 0; i < offP - audio::AudioBufferSize; i++) {
			for (int k = 0; k < nStreams; k++)
				prodotti[i*nStreams + k] = prodotti[(audio::AudioBufferSize+i) * nStreams + k];
		}
		offP -= audio::AudioBufferSize;
	}

	if (offU < audio::AudioBufferSize)
	{
		for (int k = 0; k < nStreams; k++) {
			if (!inStreams[k].hasData()) {
				return true;
			}
		}
		for (int k = 0; k < nStreams; k++) {
			/*pullStatus =*/ inStreams[k].pullData(bufferIn);	// Commentato per silenziare un warning
			for (i = 0; i < audio::AudioBufferSize; i++)
				usabili[(offU + i) * nStreams + k] = bufferIn.data()[i];
		}
		offU += audio::AudioBufferSize;
	}

	data.data_in = usabili;
	data.data_out = prodotti + offP * nStreams;
	data.input_frames = offU;
	data.output_frames = (20000 - offP * nStreams)/nStreams;
	data.end_of_input = 0;
	src_process(state, &data);

	for (i = 0; i < offU - data.input_frames_used; i++){
		for (int k = 0; k < nStreams; k++)
			usabili[i*nStreams + k] = usabili[(data.input_frames_used+i)*nStreams + k];
	}

	offU -= data.input_frames_used;
	offP += data.output_frames_gen;

	return true;
}

void RateConverter::setSpeed(SongSpeed newSpeed) {
	currentSpeed = newSpeed;
	ratio = pow(2, originalSongSpeed - currentSpeed);

	src_set_ratio(state, ratio);
}

SongSpeed RateConverter::getOriginalSongSpeed() {
	return originalSongSpeed;
}

double RateConverter::getRatio() {
	return ratio;
}

void RateConverter::flush() {
	for (int k = 0; k < nStreams; k++)
		outStreams[k].flush();

	offU = offP = 0;
	src_reset(this->state);
}
