# REMIND

## Description

This repository contains an Android application that simulates a tape recorder, specifically the Studer A810.

## Installation

**Windows** (tested, Windows 10, August 2018) **Linux** (tested, Arch Linux, September 2018 ) & **Mac** (not tested)

Install Android Studio: <https://developer.android.com/studio/>

Install Git: <https://git-scm.com/download/win>

Run Android Studio and import the project using Git. To do that go to `Check out project from Version Control` and select `Git`.

The URL is <https://gitlab.dei.unipd.it/AccessKit/remind-app-2018.git>, select the desired path, insert the required GitLab credentials and then click `Clone`.

Open the project and let Android Studio configure the environment.

## Usage

You will need:

*   SDK Platforms with the API 28

*   Android SDK Build-Tools

*   LLDB    (optional)

*   Android Emulator (optional)

*   Android SDK Platforms-Tools 28

*   Android SDK Tools 26.1.1

*   NDK

The installation and configuration of all these components can be done through Android Studio in `Tools->SDK Manager` or with the packet manager of your distribution (like `apt`, `pacman` and `aurman` if you are using Linux).

After cloning the repository and some Android Studio automatic config the application may not be configured to start. To solve this go in `File->Sync Project with Gradle Files`.

`Tools->AVD Manager->Create Virtual Device...` is usefull for the Android Emulator.

The application can be installed using the APK file in folder `Application APK`, simply copy the .apk on the device and launch it, it will install the app.

## Authors

This is the second version of the REMIND app described in [1,2].
Under the supervision of [*Niccolò Pretto*](http://www.dei.unipd.it/~prettoni/)), Carlo Fantozzi and Sergio Canazza, several contributors partecipate to the development of the two version of this app:
* _Marco Ambrico_*
* _Luca Bianconi_*
* _Daniele Colanardi_*
* _Dennis Dosso_*
* _Riccardo Gasparini_*


## Credits

This project is based on various works from different researchers and students of the Department of Information Engineering at Padova University’s Engineering Faculty.
The folllowing libraries are used in the application:

*   FFT and Inverse FFT `Cricket FFT`: <https://www.crickettechnology.com/ckfft>

*   WAVE file reader `libsndfile`: <https://www.mega-nerd.com/libsndfile/>

*   Sample Rate Converter for audio `Secret Rabbit Code (aka libsamplerate)`: <https://www.mega-nerd.com/SRC/>

*   Android audio `Google Oboe`: <https://www.github.com/google/oboe>

*   PDF Viewer `AndroidPdfViewer`: <https://www.github.com/barteksc/AndroidPdfViewer>
   
*   Efficient Image Library `Glide`: <https://github.com/bumptech/glide>

## References

[1] S.Canazza, C.Fantozzi, and N.Pretto. "Accessing tape music documents on mobile devices." *ACM Transactions Multimedia Computing Communications and Applications (TOMM)*, 12 (1s):20:1–20:20, October 2015. ISSN 1551-6857. doi: 10.1145/2808200.

[2] C. Fantozzi, F. Bressan, N. Pretto, and S. Canazza. "Tape music archives: from preservation to access." *International Journal on Digital Libraries*, 18(3):233–249, September 2017. ISSN 1432-1300. doi: 10.1007/s00799-017-0208-8.

## Notes

To copy files in the Android Emulator or in the real devices you can use the `adb` tool (with `adb push` and `adb pull` commands) or the Device File Explorer in Android Studio.

The folder that will contain the file that the app can use is `/storage/self/primary`.

If the release version of the application fails to install going to `Google Play` and disabling `Play Protect` should fix the problem.
