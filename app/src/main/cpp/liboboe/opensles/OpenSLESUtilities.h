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

#ifndef OBOE_OPENSLES_OPENSLESUTILITIES_H
#define OBOE_OPENSLES_OPENSLESUTILITIES_H

#include <SLES/OpenSLES_Android.h>
#include "Oboe.h"

namespace oboe {

const char *getSLErrStr(SLresult code);

#if __ANDROID_API__ >= __ANDROID_API_L__
/**
 * Creates an extended PCM format from the supplied format and data representation. This method
 * should only be called on Android devices with API level 21+. API 21 introduced the
 * SLAndroidDataFormat_PCM_EX object which allows audio samples to be represented using
 * single precision floating-point.
 *
 * @param format
 * @param representation
 * @return the extended PCM format
 */
SLAndroidDataFormat_PCM_EX OpenSLES_createExtendedFormat(SLDataFormat_PCM format,
                                                         SLuint32 representation);

SLuint32 OpenSLES_ConvertFormatToRepresentation(AudioFormat format);
#endif // __ANDROID_API_L__

} // namespace oboe

#endif //OBOE_OPENSLES_OPENSLESUTILITIES_H
