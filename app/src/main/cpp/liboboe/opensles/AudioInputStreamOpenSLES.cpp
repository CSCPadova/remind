/*
 * Copyright 2017 The Android Open Source Project
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

#include <cassert>

#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <common/OboeDebug.h>

#include "AudioStreamBuilder.h"
#include "AudioInputStreamOpenSLES.h"
#include "AudioStreamOpenSLES.h"
#include "OpenSLESUtilities.h"
#include "EngineOpenSLES.h"

using namespace oboe;

static SLuint32 OpenSLES_convertInputPreset(InputPreset oboePreset) {
    SLuint32 openslPreset = SL_ANDROID_RECORDING_PRESET_NONE;
    switch(oboePreset) {
        case InputPreset::Generic:
            openslPreset =  SL_ANDROID_RECORDING_PRESET_GENERIC;
            break;
        case InputPreset::Camcorder:
            openslPreset =  SL_ANDROID_RECORDING_PRESET_CAMCORDER;
            break;
        case InputPreset::VoiceRecognition:
            openslPreset =  SL_ANDROID_RECORDING_PRESET_VOICE_RECOGNITION;
            break;
        case InputPreset::VoiceCommunication:
            openslPreset =  SL_ANDROID_RECORDING_PRESET_VOICE_COMMUNICATION;
            break;
        case InputPreset::Unprocessed:
            openslPreset =  SL_ANDROID_RECORDING_PRESET_UNPROCESSED;
            break;
        default:
            break;
    }
    return openslPreset;
}

AudioInputStreamOpenSLES::AudioInputStreamOpenSLES(const AudioStreamBuilder &builder)
        : AudioStreamOpenSLES(builder) {
}

AudioInputStreamOpenSLES::~AudioInputStreamOpenSLES() {
}

// Calculate masks specific to INPUT streams.
SLuint32 AudioInputStreamOpenSLES::channelCountToChannelMask(int channelCount) {
    // Derived from internal sles_channel_in_mask_from_count(chanCount);
    // in "frameworks/wilhelm/src/android/channels.cpp".
    // Yes, it seems strange to use SPEAKER constants to describe inputs.
    // But that is how OpenSL ES does it internally.
    switch (channelCount) {
        case 1:
            return SL_SPEAKER_FRONT_LEFT;
        case 2:
            return SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT;
        default:
            return channelCountToChannelMaskDefault(channelCount);
    }
}

Result AudioInputStreamOpenSLES::open() {

    Result oboeResult = AudioStreamOpenSLES::open();
    if (Result::OK != oboeResult) return oboeResult;

    SLuint32 bitsPerSample = getBytesPerSample() * kBitsPerByte;

    // configure audio sink
    SLDataLocator_AndroidSimpleBufferQueue loc_bufq = {
            SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE,    // locatorType
            static_cast<SLuint32>(kBufferQueueLength)};   // numBuffers

    // Define the audio data format.
    SLDataFormat_PCM format_pcm = {
            SL_DATAFORMAT_PCM,       // formatType
            static_cast<SLuint32>(mChannelCount),           // numChannels
            static_cast<SLuint32>(mSampleRate * kMillisPerSecond), // milliSamplesPerSec
            bitsPerSample,                      // bitsPerSample
            bitsPerSample,                      // containerSize;
            channelCountToChannelMask(mChannelCount), // channelMask
            getDefaultByteOrder(),
    };

    SLDataSink audioSink = {&loc_bufq, &format_pcm};

    /**
     * API 21 (Lollipop) introduced support for floating-point data representation and an extended
     * data format type: SLAndroidDataFormat_PCM_EX. If running on API 21+ use this newer format
     * type, creating it from our original format.
     */
#if __ANDROID_API__ >= __ANDROID_API_L__
    SLAndroidDataFormat_PCM_EX format_pcm_ex;
    if (getSdkVersion() >= __ANDROID_API_L__) {
        SLuint32 representation = OpenSLES_ConvertFormatToRepresentation(getFormat());
        // Fill in the format structure.
        format_pcm_ex = OpenSLES_createExtendedFormat(format_pcm, representation);
        // Use in place of the previous format.
        audioSink.pFormat = &format_pcm_ex;
    }
#endif // __ANDROID_API_L__

    // configure audio source
    SLDataLocator_IODevice loc_dev = {SL_DATALOCATOR_IODEVICE,
                                      SL_IODEVICE_AUDIOINPUT,
                                      SL_DEFAULTDEVICEID_AUDIOINPUT,
                                      NULL};
    SLDataSource audioSrc = {&loc_dev, NULL};

    SLresult result = EngineOpenSLES::getInstance().createAudioRecorder(&mObjectInterface,
                                                                        &audioSrc,
                                                                        &audioSink);
    if (SL_RESULT_SUCCESS != result) {
        LOGE("createAudioRecorder() result:%s", getSLErrStr(result));
        goto error;
    }

    // Configure the stream.
    SLAndroidConfigurationItf configItf;
    result = (*mObjectInterface)->GetInterface(mObjectInterface,
                                            SL_IID_ANDROIDCONFIGURATION,
                                            &configItf);
    if (SL_RESULT_SUCCESS == result) {
        SLuint32 presetValue = OpenSLES_convertInputPreset(getInputPreset());
        result = (*configItf)->SetConfiguration(configItf,
                                         SL_ANDROID_KEY_RECORDING_PRESET,
                                         &presetValue,
                                         sizeof(SLuint32));
        if (SL_RESULT_SUCCESS != result
            && presetValue != SL_ANDROID_RECORDING_PRESET_VOICE_RECOGNITION) {
            presetValue = SL_ANDROID_RECORDING_PRESET_VOICE_RECOGNITION;
            mInputPreset = InputPreset::VoiceRecognition;
            (*configItf)->SetConfiguration(configItf,
                                             SL_ANDROID_KEY_RECORDING_PRESET,
                                             &presetValue,
                                             sizeof(SLuint32));
        }

        result = configurePerformanceMode(configItf);
        if (SL_RESULT_SUCCESS != result) {
            goto error;
        }
    }

    result = (*mObjectInterface)->Realize(mObjectInterface, SL_BOOLEAN_FALSE);
    if (SL_RESULT_SUCCESS != result) {
        LOGE("Realize recorder object result:%s", getSLErrStr(result));
        goto error;
    }

    result = (*mObjectInterface)->GetInterface(mObjectInterface, SL_IID_RECORD, &mRecordInterface);
    if (SL_RESULT_SUCCESS != result) {
        LOGE("GetInterface RECORD result:%s", getSLErrStr(result));
        goto error;
    }

    result = AudioStreamOpenSLES::registerBufferQueueCallback();
    if (SL_RESULT_SUCCESS != result) {
        goto error;
    }

    allocateFifo();

    setState(StreamState::Open);
    return Result::OK;

    error:
    return Result::ErrorInternal; // TODO convert error from SLES to OBOE
}

Result AudioInputStreamOpenSLES::close() {

    if (mState == StreamState::Closed){
        return Result::ErrorClosed;
    } else {
        requestStop();
        mRecordInterface = NULL;
        return AudioStreamOpenSLES::close();
    }
}

Result AudioInputStreamOpenSLES::setRecordState(SLuint32 newState) {

    LOGD("AudioInputStreamOpenSLES::setRecordState(%d)", newState);
    Result result = Result::OK;

    if (mRecordInterface == nullptr) {
        LOGE("AudioInputStreamOpenSLES::SetRecordState() mRecordInterface is null");
        return Result::ErrorInvalidState;
    }
    SLresult slResult = (*mRecordInterface)->SetRecordState(mRecordInterface, newState);
    if (SL_RESULT_SUCCESS != slResult) {
        LOGE("AudioInputStreamOpenSLES::SetRecordState() returned %s", getSLErrStr(slResult));
        result = Result::ErrorInternal; // TODO review
    }
    return result;
}

Result AudioInputStreamOpenSLES::requestStart() {

    LOGD("AudioInputStreamOpenSLES::requestStart()");
    StreamState initialState = getState();
    if (initialState == StreamState::Closed) return Result::ErrorClosed;

    setState(StreamState::Starting);
    Result result = setRecordState(SL_RECORDSTATE_RECORDING);
    if (result == Result::OK) {
        // Enqueue the first buffer so that we have data ready in the callback.
        setState(StreamState::Started);
        enqueueCallbackBuffer(mSimpleBufferQueueInterface);
    } else {
        setState(initialState);
    }
    return result;
}


Result AudioInputStreamOpenSLES::requestPause() {

    LOGD("AudioInputStreamOpenSLES::requestPause()");
    StreamState initialState = getState();
    if (initialState == StreamState::Closed) return Result::ErrorClosed;

    setState(StreamState::Pausing);
    Result result = setRecordState(SL_RECORDSTATE_PAUSED);
    if (result == Result::OK) {
        mPositionMillis.reset32(); // OpenSL ES resets its millisecond position when paused.
        setState(StreamState::Paused);
    } else {
        setState(initialState);
    }
    return result;
}

Result AudioInputStreamOpenSLES::requestFlush() {
    return Result::ErrorUnimplemented; // TODO
}

Result AudioInputStreamOpenSLES::requestStop() {

    LOGD("AudioInputStreamOpenSLES::requestStop()");
    StreamState initialState = getState();
    if (initialState == StreamState::Closed) return Result::ErrorClosed;

    setState(StreamState::Stopping);

    Result result = setRecordState(SL_RECORDSTATE_STOPPED);
    if (result == Result::OK) {
        mPositionMillis.reset32(); // OpenSL ES resets its millisecond position when stopped.
        setState(StreamState::Stopped);
    } else {
        setState(initialState);
    }
    return result;

}

int64_t AudioInputStreamOpenSLES::getFramesWritten() {
    return getFramesProcessedByServer();
}

Result AudioInputStreamOpenSLES::updateServiceFrameCounter() {
    if (mRecordInterface == NULL) {
        return Result::ErrorNull;
    }
    SLmillisecond msec = 0;
    SLresult slResult = (*mRecordInterface)->GetPosition(mRecordInterface, &msec);
    Result result = Result::OK;
    if(SL_RESULT_SUCCESS != slResult) {
        LOGD("%s(): GetPosition() returned %s", __func__, getSLErrStr(slResult));
        // set result based on SLresult
        result = Result::ErrorInternal;
    } else {
        mPositionMillis.update32(msec);
    }
    return result;
}
