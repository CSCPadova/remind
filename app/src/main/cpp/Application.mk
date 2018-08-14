#NDK_TOOLCHAIN_VERSION=4.8
#NDK_TOOLCHAIN_VERSION=clang
APP_STL := stlport_static
#APP_STL := gnustl_static deprecato
APP_STL := c++_static

APP_USE_CPP0X := true
APP_CPPFLAGS := -std=c++11 -frtti -fexceptions -Wall -Ofast
APP_PLATFORM := android-28
#APP_CPPFLAGS += -std=c++11 -Wall
#APP_ABI := armeabi-v7a
APP_ABI := all
APP_OPTIM := release
