/*
 * Copyright 2015 The Android Open Source Project
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
#include <pthread.h>
#include <AudioStreamBuilder.h>
#include <AudioStream.h>
#include <Oboe.h>
#include "OboeDebug.h"

namespace oboe {

/*
 * AudioStream
 */
AudioStream::AudioStream(const AudioStreamBuilder &builder)
        : AudioStreamBase(builder) {
}

Result AudioStream::open() {
    // TODO validate parameters or let underlying API validate them?
    return Result::OK;
}

DataCallbackResult AudioStream::fireCallback(void *audioData, int32_t numFrames) {
    int scheduler = sched_getscheduler(0) & ~SCHED_RESET_ON_FORK; // for current thread
    if (scheduler != mPreviousScheduler) {
        LOGD("AudioStream::fireCallback() scheduler = %s",
             ((scheduler == SCHED_FIFO) ? "SCHED_FIFO" :
             ((scheduler == SCHED_OTHER) ? "SCHED_OTHER" :
             ((scheduler == SCHED_RR) ? "SCHED_RR" : "UNKNOWN")))
        );
        mPreviousScheduler = scheduler;
    }

    DataCallbackResult result;
    if (mStreamCallback == nullptr) {
        result = onDefaultCallback(audioData, numFrames);
    } else {
        result = mStreamCallback->onAudioReady(this, audioData, numFrames);
        if (getDirection() == Direction::Input) {
            incrementFramesRead(numFrames);
        } else {
            incrementFramesWritten(numFrames);
        }
    }

    return result;
}

Result AudioStream::waitForStateTransition(StreamState startingState,
                                           StreamState endingState,
                                           int64_t timeoutNanoseconds)
{
    StreamState state = getState();
    if (state == StreamState::Closed) {
        return Result::ErrorClosed;
    } else {
        StreamState nextState = state;
        if (state == startingState && state != endingState) {
            Result result = waitForStateChange(state, &nextState, timeoutNanoseconds);
            if (result != Result::OK) {
                return result;
            }
        }
        if (nextState != endingState) {
            return Result::ErrorInvalidState;
        } else {
            return Result::OK;
        }
    }
}

Result AudioStream::start(int64_t timeoutNanoseconds)
{
    Result result = requestStart();
    if (result != Result::OK) return result;
    return waitForStateTransition(StreamState::Starting,
                                  StreamState::Started, timeoutNanoseconds);
}

Result AudioStream::pause(int64_t timeoutNanoseconds)
{
    Result result = requestPause();
    if (result != Result::OK) return result;
    return waitForStateTransition(StreamState::Pausing,
                                  StreamState::Paused, timeoutNanoseconds);
}

Result AudioStream::flush(int64_t timeoutNanoseconds)
{
    Result result = requestFlush();
    if (result != Result::OK) return result;
    return waitForStateTransition(StreamState::Flushing,
                                  StreamState::Flushed, timeoutNanoseconds);
}

Result AudioStream::stop(int64_t timeoutNanoseconds)
{
    Result result = requestStop();
    if (result != Result::OK) return result;
    return waitForStateTransition(StreamState::Stopping,
                                  StreamState::Stopped, timeoutNanoseconds);
}

bool AudioStream::isPlaying() {
    StreamState state = getState();
    return state == StreamState::Starting || state == StreamState::Started;
}

int32_t AudioStream::getBytesPerSample() const {
    return convertFormatToSizeInBytes(mFormat);
}

} // namespace oboe