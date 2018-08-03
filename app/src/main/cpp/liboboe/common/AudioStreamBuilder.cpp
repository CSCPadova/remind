/*
 * Copyright 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <sys/types.h>
#include <aaudio/AudioStreamAAudio.h>
#include <Oboe.h>
#include <opensles/AudioInputStreamOpenSLES.h>
#include <opensles/AudioOutputStreamOpenSLES.h>


namespace oboe {

/**
 * The following default values are used when oboe does not have any better way of determining the optimal values
 * for an audio stream. This can happen when:
 *
 * - Client is creating a stream on API < 26 (OpenSLES) but has not supplied the optimal sample
 * rate and/or frames per burst
 * - Client is creating a stream on API 16 (OpenSLES) where AudioManager.PROPERTY_OUTPUT_* values
 * are not available
 */
int32_t DefaultStreamValues::SampleRate = 48000; // Common rate for mobile audio and video
int32_t DefaultStreamValues::FramesPerBurst = 192; // 4 msec at 48000 Hz
int32_t DefaultStreamValues::ChannelCount = 2; // Stereo

bool AudioStreamBuilder::isAAudioSupported() {
    return AudioStreamAAudio::isSupported();
}

bool AudioStreamBuilder::isAAudioRecommended() {
    // See https://github.com/google/oboe/issues/40,
    // AAudio may not be stable on Android O, depending on how it is used.
    // To be safe, use AAUdio on O_MR1 and above.
    return (getSdkVersion() >= __ANDROID_API_O_MR1__);
}

AudioStream *AudioStreamBuilder::build() {
    AudioStream *stream = nullptr;
    if (mAudioApi == AudioApi::AAudio && isAAudioSupported()) {
        stream = new AudioStreamAAudio(*this);

    // If unspecified, only use AAudio if supported and recommended.
    } else if (mAudioApi == AudioApi::Unspecified && isAAudioSupported() && isAAudioRecommended()) {
        stream = new AudioStreamAAudio(*this);
    } else {
        if (getDirection() == oboe::Direction::Output) {
            stream = new AudioOutputStreamOpenSLES(*this);
        } else if (getDirection() == oboe::Direction::Input) {
            stream = new AudioInputStreamOpenSLES(*this);
        }
    }
    return stream;
}

Result AudioStreamBuilder::openStream(AudioStream **streamPP) {
    if (streamPP == nullptr) {
        return Result::ErrorNull;
    }
    *streamPP = nullptr;
    AudioStream *streamP = build();
    if (streamP == nullptr) {
        return Result::ErrorNull;
    }
    Result result = streamP->open(); // TODO review API
    if (result == Result::OK) {
        *streamPP = streamP;
    }
    return result;
}

} // namespace oboe