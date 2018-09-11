#ifndef MIXER_H
#define MIXER_H

#include "QuadraphonicFilter.h"
#include <audiostreams.h>
#include <filterbase.h>
#include <tgmath.h>

//#define FRONT_DISTANCE_QUAD 0.36f
//#define BACK_DISTANCE_QUAD 2.25f
//#define HEAD_WIDTH 0.25f
//#define HALF_HEAD_WIDTH 0.125f
//#define HEAD_WIDTH_QUAD 0.0625f

enum SongType {
    SONG_TYPE_1M, SONG_TYPE_1S, SONG_TYPE_2M, SONG_TYPE_4M
};

//#define MAX_SATELLITE_POSITION 100
//#define MIN_SATELLITE_POSITION 0

class MixerProcessor;

class Mixer : public FilterBase {
public:
    audio::InputStream inputs[4];
    audio::OutputStream outputLeft, outputRight;

    Mixer(SongType songType, int samplingFrequency);

    ~Mixer();

    void setTrackVolume(int channelNumber, float volumeL, float volumeR);

    void setChannelEnabled(int channelNumber, bool enabled);

    int getChannelEnabled(int channel);

    int getSamplingFrequency() {
        return samplingFrequency;
    }

    float getTrackVolumeL(int trackNumber);

    float getTrackVolumeR(int trackNumber);

    void flush();

private:
    friend class MixerProcessor1M;

    friend class MixerProcessor2M;

    friend class MixerProcessor4M;

    SongType songType;
    int samplingFrequency;
    int chansToPull;
    int trackMap[4];
    bool trackEnabled[4];
    MixerProcessor *processor;

    bool loop();    // da FilterBase

    void setMixingVolumes();

    //volume delle tracce
    float track1LGUI;
    float track1RGUI;
    float track2LGUI;
    float track2RGUI;
    float track3LGUI;
    float track3RGUI;
    float track4LGUI;
    float track4RGUI;

    float track1LMIX;
    float track1RMIX;
    float track2LMIX;
    float track2RMIX;
    float track3LMIX;
    float track3RMIX;
    float track4LMIX;
    float track4RMIX;
};

class MixerProcessor {
protected:
    Mixer *mixer;
    audio::AudioBuffer silence;
public:
    MixerProcessor(Mixer *mixer);

    virtual ~MixerProcessor() {
    }

    virtual void process(audio::AudioBuffer (&buffers)[4], audio::AudioBuffer &outLeft,
                         audio::AudioBuffer &outRight) = 0;
};

class MixerProcessor1M : public MixerProcessor {
public:
    MixerProcessor1M(Mixer *mixer)
            : MixerProcessor(mixer) {
    }

    void process(audio::AudioBuffer (&buffers)[4], audio::AudioBuffer &outLeft,
                 audio::AudioBuffer &outRight);
};

class MixerProcessor2M : public MixerProcessor {
private:
public:
    MixerProcessor2M(Mixer *mixer)
            : MixerProcessor(mixer) {
    }

    void process(audio::AudioBuffer (&buffers)[4], audio::AudioBuffer &outLeft,
                 audio::AudioBuffer &outRight);
};

class MixerProcessor4M : public MixerProcessor {
private:
public:

    MixerProcessor4M(Mixer *mixer)
            : MixerProcessor(mixer) {
    }

    ~MixerProcessor4M() {
    }

    void process(audio::AudioBuffer (&buffers)[4], audio::AudioBuffer &outLeft,
                 audio::AudioBuffer &outRight);
};

#endif
