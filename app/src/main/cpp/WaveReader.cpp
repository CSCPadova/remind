#include <liboboe/common/OboeDebug.h>
#include "WaveReader.h"
#include "log.h"

WaveReader::WaveReader(const char *paths[], int nPaths) {
    int i = 0;
    for (; i < nPaths; i++) {
        if (paths[i] != nullptr) {
            int fd = open(paths[i], O_RDONLY);
            fileAudio[i] = sf_open_fd(fd, SFM_READ, &info, 1);

            if (fileAudio[i] == nullptr) {
                LOGE("WaveReader: Errore apertura file: %s", sf_strerror(fileAudio[i]));
                return;
            } else {
                LOGD("frames: %lld", info.frames);
                LOGD("Samplerate: %d", info.samplerate);
                LOGD("channels: %d", info.channels);
                LOGD("format : %x", (info.format & 0xFFFF0000));
                LOGD("bit per sample: %x", (info.format & 0x0000FFFF));
                LOGD("seekable : %d", info.seekable);
            }
        } else {
            LOGE("WaveReader: passato nome file null all'indice %d", i);
            return;
        }
    }

    //for(; i < 4; i++)	// il resto li mette a null
    //	fileAudio[i] = nullptr;

    inTracks = nPaths;
    outTracks = inTracks;

    if (info.channels == 2)
        outTracks = 2;

    if (getAudioFileFormat() != SF_FORMAT_WAV)
        throw FILE_FORMAT_NOT_SUPPORTED_EXCEPTION;

    if (!info.seekable)
        throw FILE_NOT_SEEKABLE_EXCEPTION;

    validReader = true;
}

bool WaveReader::isValid() {
    return validReader;
}

int WaveReader::getAudioFileFormat() {
    return (info.format & 0xFFFF0000);
}

WaveReader::~WaveReader(void) {
    for (int i = 0; i < inTracks; i++)
        sf_close(this->fileAudio[i]);
}

bool WaveReader::isEof() {
    return eof;
}

bool WaveReader::loop() {
    for (int i = 0; i < outTracks; i++) {
        auto s = outStreams[i].waitIfFull();
        if (s == audio::Status::ERROR) {
            __android_log_print(ANDROID_LOG_ERROR, "ERROR", "WaveReader: pushData NOT ok");
            return false;
        } else if (s == audio::Status::TIMEOUT) {
            return true;
        }
    }

    int letti = 0;
    if (info.channels == 2) {
        //traccia stereo
        letti = sf_read_float(fileAudio[0], tempBuffer, audio::AudioBufferSize * 2);
    } else {
        //tracce mono
        for (int i = 0; i < inTracks; i++) {
            letti = sf_read_float(fileAudio[i], buffers[i].data(), audio::AudioBufferSize);
            if (letti == 0)
                break;
        }
    }

    if (letti == 0 || eof) {
        eof = true;
        return false;
    }

    if (info.channels == 2) {
        int i = 0;
        for (; i < letti / 2; i++) {
            buffers[0].data()[i] = tempBuffer[i * 2];
            buffers[1].data()[i] = tempBuffer[i * 2 + 1];
        }
        for (; i < audio::AudioBufferSize; i++) {
            buffers[0].data()[i] = 0.0f;
            buffers[1].data()[i] = 0.0f;
        }
    }

    for (int i = 0; i < outTracks; i++) {
        outStreams[i].pushData(buffers[i]);
    }

    return true;
}

int WaveReader::getChannelCount() {
    return info.channels;
}

int WaveReader::getBitPerSample() {
    return (info.format & 0x0000FFFF);
}

int WaveReader::getSamplerate() {
    return info.samplerate;
}

double WaveReader::getSongDuration() {
    return (double) (info.frames * 100) / (double) getSamplerate();
}

void WaveReader::flush() {
    for (int i = 0; i < outTracks; i++)
        outStreams[i].flush();
}

void WaveReader::seek(double timeCentiSec) {
    if (timeCentiSec >= getSongDuration()) {
        timeCentiSec = getSongDuration();
        eof = true;
        return;
    } else if (timeCentiSec <= 0) {
        timeCentiSec = 0;
    }

    long long int framesFromBegin =
            (long long int) getSamplerate() * (long long int) timeCentiSec / 100;

    int blockDim = info.channels == 2 ? audio::AudioBufferSize * 2 : audio::AudioBufferSize;

    framesFromBegin = framesFromBegin - framesFromBegin % blockDim;
    for (int i = 0; i < inTracks; i++)
        sf_seek(fileAudio[i], framesFromBegin, SEEK_SET);

    eof = false;
}
