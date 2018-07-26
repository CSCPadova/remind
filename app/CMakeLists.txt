# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

#add_library( # Sets the name of the library.
#             native-lib
#
#             # Sets the library as a shared library.
#             SHARED
#
#             # Provides a relative path to your source file(s).
#             src/main/cpp/native-lib.cpp )


add_library( native-player

             SHARED

             src/main/cpp/native-player.cpp
             src/main/cpp/Mixer.cpp
             src/main/cpp/Reverber.cpp
             src/main/cpp/QuadraphonicFilter.cpp
             src/main/cpp/audiostreams.cpp
             src/main/cpp/filterbase.cpp
             src/main/cpp/DelayBuffer.cpp
             src/main/cpp/RateConverter.cpp
             src/main/cpp/WaveReader.cpp
             src/main/cpp/fftconvolver.cpp)

add_library( libsamplerate

             SHARED

             src/main/cpp/samplerate.c
             src/main/cpp/src_linear.c
             src/main/cpp/src_sinc.c
             src/main/cpp/src_zoh.c)

add_library( ckfft

             SHARED

             src/main/cpp/ckfft.cpp
             src/main/cpp/context.cpp
             src/main/cpp/debug.cpp
             src/main/cpp/fft.cpp
             src/main/cpp/fft_default.cpp
             src/main/cpp/fft_real.cpp
             src/main/cpp/fft_real_default.cpp

             #qui c'e' un if per otimizzare il codice, vedere se si puo' preservare
             src/main/cpp/fft_neon.cpp
             src/main/cpp/fft_real_neon.cpp)


# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

#find_library( # Sets the name of the path variable.
#              log-lib
#
#              # Specifies the name of the NDK library that
#              # you want CMake to locate.
#              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                      libsamplerate libsndfile ckfft OpenSLES

                      # Links the target library to the log library
                      # included in the NDK.
                      ${log-lib} )