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
    track1LGUI = 0.5f;
    track1RGUI = 0.5f;
    track2LGUI = 0.5f;
    track2RGUI = 0.5f;
    track3LGUI = 0.5f;
    track3RGUI = 0.5f;
    track4LGUI = 0.5f;
    track4RGUI = 0.5f;
    setMixingVolumes();

    this->songType = songType;
}

Mixer::~Mixer() {
    delete this->processor;
}

void Mixer::setMixingVolumes()
{
    float sum=track1LGUI + track1RGUI + track2LGUI + track2RGUI + track3LGUI + track3RGUI +
              track4LGUI + track4RGUI;
    track1LMIX = track1LGUI/sum;
    track1RMIX = track1RGUI/sum;
    track2LMIX = track2LGUI/sum;
    track2RMIX = track2RGUI/sum;
    track3LMIX = track3LGUI/sum;
    track3RMIX = track3RGUI/sum;
    track4LMIX = track4LGUI/sum;
    track4RMIX = track4RGUI/sum;
}

void Mixer::setTrackVolume(int trackNumber, float volumeL, float volumeR) {
    switch (trackNumber) {
        case 1:
            track1LGUI = volumeL;
            track1RGUI = volumeR;
            break;
        case 2:
            track2LGUI= volumeL;
            track2RGUI= volumeR;
            break;
        case 3:
            track3LGUI = volumeL;
            track3RGUI = volumeR;
            break;
        case 4:
            track4LGUI = volumeL;
            track4RGUI = volumeR;
            break;
        default:
            break;
    }
    setMixingVolumes();
}

void Mixer::setChannelEnabled(int trackNumber, bool enabled) {
    this->trackEnabled[trackNumber - 1] = enabled;
}

float Mixer::getTrackVolumeL(int trackNumber) {
    switch (trackNumber) {
        case 1:
            return track1LGUI;
        case 2:
            return track2LGUI;
        case 3:
            return track3LGUI;
        case 4:
            return track4LGUI;
        default:
            return -1;
    }
}

float Mixer::getTrackVolumeR(int trackNumber) {
    switch (trackNumber) {
        case 1:
            return track1RGUI;
        case 2:
            return track2RGUI;
        case 3:
            return track3RGUI;
        case 4:
            return track4RGUI;
        default:
            return -1;
    }
}

int Mixer::getChannelEnabled(int channel) {
    return this->trackEnabled[channel - 1];
}

bool Mixer::loop() {
    audio::AudioBuffer temp[4], outLeft, outRight;

    auto stat = outputLeft.waitIfFull();
    if (stat == audio::Status::ERROR)
        return false;
    if (stat == audio::Status::TIMEOUT)
        return true;

    stat = outputRight.waitIfFull();
    if (stat == audio::Status::ERROR)
        return false;
    if (stat == audio::Status::TIMEOUT)
        return true;

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
        if (pullStatus == audio::Status::TIMEOUT)
            return true;
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
        outLeft[i] = inputBuffer[i] * this->mixer->track1LMIX;
        outRight[i] = inputBuffer[i] * this->mixer->track1RMIX;
    }

}

void MixerProcessor1S::process(audio::AudioBuffer (&buffers)[4], audio::AudioBuffer &outLeft,
                               audio::AudioBuffer &outRight) {
    auto &inputBuffer_1 = mixer->trackEnabled[0] ? buffers[0] : silence;
    auto &inputBuffer_2 = mixer->trackEnabled[1] ? buffers[1] : silence;

    for (unsigned int i = 0; i < audio::AudioBufferSize; i++) {
        outLeft[i] = (inputBuffer_1[i] * this->mixer->track1LMIX) +
                     (inputBuffer_2[i] * this->mixer->track2LMIX);
        outRight[i] = (inputBuffer_1[i] * this->mixer->track1RMIX) +
                      (inputBuffer_2[i] * this->mixer->track2RMIX);
    }
}

void MixerProcessor2M::process(audio::AudioBuffer (&buffers)[4], audio::AudioBuffer &outLeft,
                               audio::AudioBuffer &outRight) {

    auto &inputBuffer_1 = mixer->trackEnabled[0] ? buffers[0] : silence;
    auto &inputBuffer_2 = mixer->trackEnabled[1] ? buffers[1] : silence;

    for (unsigned int i = 0; i < audio::AudioBufferSize; i++) {
        outLeft[i] = (inputBuffer_1[i] * this->mixer->track1LMIX) +
                     (inputBuffer_2[i] * this->mixer->track2LMIX);
        outRight[i] = (inputBuffer_1[i] * this->mixer->track1RMIX) +
                      (inputBuffer_2[i] * this->mixer->track2RMIX);
    }
}

void MixerProcessor4M::process(audio::AudioBuffer (&buffers)[4], audio::AudioBuffer &outLeft,
                               audio::AudioBuffer &outRight) {

    auto &inputBuffer_1 = mixer->trackEnabled[0] ? buffers[mixer->trackMap[0]] : silence;
    auto &inputBuffer_2 = mixer->trackEnabled[1] ? buffers[mixer->trackMap[1]] : silence;
    auto &inputBuffer_3 = mixer->trackEnabled[2] ? buffers[mixer->trackMap[2]] : silence;
    auto &inputBuffer_4 = mixer->trackEnabled[3] ? buffers[mixer->trackMap[3]] : silence;

    for (unsigned int i = 0; i < audio::AudioBufferSize; i++) {

        outLeft[i] = (inputBuffer_1[i] * this->mixer->track1LMIX) +
                     (inputBuffer_2[i] * this->mixer->track2LMIX) +
                     (inputBuffer_3[i] * this->mixer->track3LMIX) +
                     (inputBuffer_4[i] * this->mixer->track4LMIX);
        outRight[i] = (inputBuffer_1[i] * this->mixer->track1RMIX) +
                      (inputBuffer_2[i] * this->mixer->track2RMIX) +
                      (inputBuffer_3[i] * this->mixer->track3RMIX) +
                      (inputBuffer_4[i] * this->mixer->track4RMIX);
    }
}

void Mixer::flush() {
    outputLeft.flush();
    outputRight.flush();
}
