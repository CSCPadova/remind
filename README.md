
# REMIND

## Description

This repository contains an Android application that simulates a megnetophone, specifically the Studer A810
## Installation

**Windows** (tested) **Mac** (not tested) & **Linux** (tested)

Install Android Studio: <https://developer.android.com/studio/>

Install Git: <https://git-scm.com/download/win>

Run Android Studio and import the project using Git. To do that go to `Check out project from Version Control` and select `Git`.

The URL is `https://gitlab.dei.unipd.it/AccessKit/remind-app-2018.git`, select the desired path, insert the required GitLab credentials and then click `Clone`.

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

The installation and configuration of all these components can be done through Android Studio in `Tools->SDK Manager` or with the packet manager of your distribution (like `apt`, `pacman` and `aurman` if you are using Linux)

`Tools->AVD Manager->Create Virtual Device...` is usefull for the Android Emulator 

## Credits

This project is based on various works from different researchers and students of the Department of Information Engineering at Padova University’s Engineering Faculty.

## References

A lot actually, to many to put them here

## Notes

To copy files in the Android Emulator or in the real devices you can use the `adb` tool (with `adb push` and `adb pull` commands) or the Device File Explorer in Android Studio.

The folder that will contain the file that the app can use is `/storage/self/primary`