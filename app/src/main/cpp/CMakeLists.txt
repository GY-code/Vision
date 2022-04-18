# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# 包含opencv的头文件
include_directories(../../../opencv/jni/include)

# 静态方式导入库
#add_library(opencv_calib3d STATIC IMPORTED)
#add_library(opencv_core STATIC IMPORTED)
#add_library(opencv_features2d STATIC IMPORTED)
#add_library(opencv_flann STATIC IMPORTED)
#add_library(opencv_imgcodecs STATIC IMPORTED)
#add_library(opencv_imgproc STATIC IMPORTED)
#add_library(opencv_stitching STATIC IMPORTED)
#
#add_library(IlmImf STATIC IMPORTED)
#add_library(libjasper STATIC IMPORTED)
#add_library(libjpeg STATIC IMPORTED)
#add_library(libpng STATIC IMPORTED)
##add_library(libprotobuf STATIC IMPORTED)
#add_library(libtiff STATIC IMPORTED)
#add_library(libwebp STATIC IMPORTED)
#add_library(tbb STATIC IMPORTED)
#add_library(tegra_hal STATIC IMPORTED)
#
## 设置库路径
#set_target_properties(opencv_calib3d PROPERTIES IMPORTED_LOCATION ../../../libs/armeabi-v7a/libopencv_calib3d.a)
#set_target_properties(opencv_core PROPERTIES IMPORTED_LOCATION ../../../libs/armeabi-v7a/libopencv_core.a)
#set_target_properties(opencv_features2d PROPERTIES IMPORTED_LOCATION ../../../libs/armeabi-v7a/libopencv_features2d.a)
#set_target_properties(opencv_flann PROPERTIES IMPORTED_LOCATION ../../../libs/armeabi-v7a/libopencv_flann.a)
#set_target_properties(opencv_imgcodecs PROPERTIES IMPORTED_LOCATION ../../../libs/armeabi-v7a/libopencv_imgcodecs.a)
#set_target_properties(opencv_imgproc PROPERTIES IMPORTED_LOCATION ../../../libs/armeabi-v7a/libopencv_imgproc.a)
#set_target_properties(opencv_stitching PROPERTIES IMPORTED_LOCATION ../../../libs/armeabi-v7a/libopencv_stitching.a)
#
#
#set_target_properties(IlmImf PROPERTIES IMPORTED_LOCATION ../../../opencv/3rdparty/libs/armeabi-v7a/libIlmImf.a)
#set_target_properties(libjasper PROPERTIES IMPORTED_LOCATION ../../../opencv/3rdparty/libs/armeabi-v7a/liblibjasper.a)
#set_target_properties(libjpeg PROPERTIES IMPORTED_LOCATION ../../../opencv/3rdparty/libs/armeabi-v7a/liblibjpeg.a)
#set_target_properties(libpng PROPERTIES IMPORTED_LOCATION ../../../opencv/3rdparty/libs/armeabi-v7a/liblibpng.a)
##set_target_properties(libprotobuf PROPERTIES IMPORTED_LOCATION ${PROJECT_SOURCE_DIR}/opencv/3rdparty/libs/${ANDROID_ABI}/liblibprotobuf.a)
#set_target_properties(libtiff PROPERTIES IMPORTED_LOCATION ../../../opencv/3rdparty/libs/armeabi-v7a/liblibtiff.a)
#set_target_properties(libwebp PROPERTIES IMPORTED_LOCATION ../../../opencv/3rdparty/libs/armeabi-v7a/liblibwebp.a)
#set_target_properties(tbb PROPERTIES IMPORTED_LOCATION ../../../opencv/3rdparty/libs/armeabi-v7a/libtbb.a)
#set_target_properties(tegra_hal PROPERTIES IMPORTED_LOCATION ../../../opencv/3rdparty/libs/armeabi-v7a/libtegra_hal.a)
#
#set(SRC_DIR ../../../src/main/cpp)
#
#file(GLOB_RECURSE CPP_SRCS "${SRC_DIR}/*.cpp")  #指定当前目录下的所有.cpp文件（包括子目录）


# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
        native-lib

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        native-lib.cpp)


add_library(
        opencvc7 #最终在build中生成的so名字
        SHARED
        IMPORTED)
set_target_properties(
        opencvc7 #最终在build中生成的so名字
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}../../../../libs/${ANDROID_ABI}/libopencv_java3.so)
# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        native-lib
        opencvc7

#        opencv_stitching
#        opencv_features2d
#        opencv_flann
#        opencv_imgcodecs
#        opencv_imgproc
#        opencv_core
#        opencv_calib3d
#
#        IlmImf
#        libjasper
#        libjpeg
#        libpng
#        #libprotobuf
#        libtiff
#        libwebp
#        tbb
#        tegra_hal

        # Links the target library to the log library
        # included in the NDK.
        jnigraphics
        ${log-lib})