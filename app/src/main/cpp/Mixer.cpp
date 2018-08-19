#include "Mixer.h"

#include "log.h"

Mixer::Mixer(SongType songType, int samplingFrequency) {
    this->samplingFrequency = samplingFrequency;
    switch (songType) {
        case SONG_TYPE_1M:

            this->trackEnabled[0] = true;

            processor = new MixerProcessor1M(this);
            break;

        case SONG_TYPE_1S: // !!!
        case SONG_TYPE_2M:

            this->trackMap[0] = 0;
            this->trackMap[1] = 1;

            this->trackEnabled[0] = true;
            this->trackEnabled[1] = true;

            processor = new MixerProcessor2M(this);
            break;

        case SONG_TYPE_4M:

            this->trackMap[0] = 0;
            this->trackMap[1] = 1;
            this->trackMap[2] = 2;
            this->trackMap[3] = 3;

            this->trackEnabled[0] = true;
            this->trackEnabled[1] = true;
            this->trackEnabled[2] = true;
            this->trackEnabled[3] = true;
            processor = new MixerProcessor4M(this);
            break;
    }
    track1L = 0.5;
    track1R = 0.5;
    track2L = 0.5;
    track2R = 0.5;
    track3L = 0.5;
    track3R = 0.5;
    track4L = 0.5;
    track4R = 0.5;

    this->songType = songType;
}

Mixer::~Mixer() {
    delete this->processor;
}

void Mixer::setTrackVolume(int trackNumber, float volumeL, float volumeR) {
    switch (trackNumber) {
        case 1:
            track1L = volumeL;
            track1R = volumeR;
            break;
        case 2:
            track2L = volumeL;
            track2R = volumeR;
            break;
        case 3:
            track3L = volumeL;
            track3R = volumeR;
            break;
        case 4:
            track4L = volumeL;
            track4R = volumeR;
            break;
        default:
            int nothing=0;
    }
}

void Mixer::setChannelEnabled(int trackNumber, bool enabled) {
    this->trackEnabled[trackNumber - 1] = enabled;
}

float Mixer::getTrackVolumeL(int trackNumber) {
    switch (trackNumber) {
        case 1:
            return track1L;
        case 2:
            return track2L;
        case 3:
            return track3L;
        case 4:
            return track4L;
        default:
            return -1;
    }
}

float Mixer::getTrackVolumeR(int trackNumber) {
    switch (trackNumber) {
        case 1:
            return track1R;
        case 2:
            return track2R;
        case 3:
            return track3R;
        case 4:
            return track4R;
        default:
            return -1;
    }
}

int Mixer::getChannelEnabled(int channel) {
    return this->trackEnabled[channel - 1];
}

bool Mixer::loop() {
    audio::AudioBuffer temp[4], outLeft, outRight;

    // --- Legge il numero di input adeguato ---
    int chansToPull = 0;
    switch (this->songType) {
        case SONG_TYPE_4M:
            chansToPull = 4;
            break;
        case SONG_TYPE_2M:
            chansToPull = 2;
            break;
        case SONG_TYPE_1S:
            chansToPull = 2;
            break;
        case SONG_TYPE_1M:
            chansToPull = 1;
            break;
    }

    audio::Status pullStatus;
    for (int i = 0; i < chansToPull; i++) {
        pullStatus = inputs[i].waitIfEmpty();

        if (pullStatus ==
            audio::Status::ERROR) {        // se un pullData è fallito per qualche motivo
            LOGD("pull mixer errore");
            return false;        // ritorna false così si interrompe l'elaborazione
        }
    }

    for (int i = 0; i < chansToPull; i++)
        inputs[i].pullData(temp[i]);

    // --- Esegue l'elaborazione ---
    this->processor->process(temp, outLeft, outRight);

    // --- Scrive i risultati ---
    auto pushStatusL = outputLeft.pushData(outLeft);
    auto pushStatusR = outputRight.pushData(outRight);
    if (pushStatusL == audio::Status::ERROR || pushStatusR == audio::Status::ERROR) {
        LOGD("push mixer non ok");
        return false;
    }

    return true;
}

MixerProcessor::MixerProcessor(Mixer *mixer) {
    this->mixer = mixer;
    for (unsigned int i = 0; i < silence.size(); i++)
        silence[i] = 0.0f;
}

void MixerProcessor1M::process(audio::AudioBuffer (&buffers)[4], audio::AudioBuffer &outLeft,
                               audio::AudioBuffer &outRight) {
    auto &inputBuffer = mixer->trackEnabled[0] ? buffers[0] : silence;
    for (unsigned int i = 0; i < audio::AudioBufferSize; i++) {
        outLeft[i] = inputBuffer[i] * this->mixer->track1L;
        outRight[i] = inputBuffer[i] * this->mixer->track1R;
    }

}

void MixerProcessor1S::process(audio::AudioBuffer (&buffers)[4], audio::AudioBuffer &outLeft,
                               audio::AudioBuffer &outRight) {
    auto &inputBuffer_1 = mixer->trackEnabled[0] ? buffers[0] : silence;
    auto &inputBuffer_2 = mixer->trackEnabled[1] ? buffers[1] : silence;

    for (unsigned int i = 0; i < audio::AudioBufferSize; i++) {
        outLeft[i] = (inputBuffer_1[i] * this->mixer->track1L) +
                     (inputBuffer_2[i] * this->mixer->track2L);
        outRight[i] = (inputBuffer_1[i] * this->mixer->track1R) +
                      (inputBuffer_2[i] * this->mixer->track2R);
    }
}

void MixerProcessor2M::process(audio::AudioBuffer (&buffers)[4], audio::AudioBuffer &outLeft,
                               audio::AudioBuffer &outRight) {

    auto &inputBuffer_1 = mixer->trackEnabled[0] ? buffers[0] : silence;
    auto &inputBuffer_2 = mixer->trackEnabled[1] ? buffers[1] : silence;

    for (unsigned int i = 0; i < audio::AudioBufferSize; i++) {
        outLeft[i] = (inputBuffer_1[i] * this->mixer->track1L) +
                     (inputBuffer_2[i] * this->mixer->track2L);
        outRight[i] = (inputBuffer_1[i] * this->mixer->track1R) +
                      (inputBuffer_2[i] * this->mixer->track2R);
    }
}

void MixerProcessor4M::process(audio::AudioBuffer (&buffers)[4], audio::AudioBuffer &outLeft,
                               audio::AudioBuffer &outRight) {

    auto &inputBuffer_1 = mixer->trackEnabled[0] ? buffers[mixer->trackMap[0]] : silence;
    auto &inputBuffer_2 = mixer->trackEnabled[1] ? buffers[mixer->trackMap[1]] : silence;
    auto &inputBuffer_3 = mixer->trackEnabled[2] ? buffers[mixer->trackMap[2]] : silence;
    auto &inputBuffer_4 = mixer->trackEnabled[3] ? buffers[mixer->trackMap[3]] : silence;

    for (unsigned int i = 0; i < audio::AudioBufferSize; i++) {

        outLeft[i] = (inputBuffer_1[i] * this->mixer->track1L) +
                     (inputBuffer_2[i] * this->mixer->track2L) +
                     (inputBuffer_3[i] * this->mixer->track3L) +
                     (inputBuffer_4[i] * this->mixer->track4L);
        outRight[i] = (inputBuffer_1[i] * this->mixer->track1R) +
                      (inputBuffer_2[i] * this->mixer->track2R) +
                      (inputBuffer_3[i] * this->mixer->track3R) +
                      (inputBuffer_4[i] * this->mixer->track4R);
    }
}

void Mixer::flush() {
    outputLeft.flush();
    outputRight.flush();
}
