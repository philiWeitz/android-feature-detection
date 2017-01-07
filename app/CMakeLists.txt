# Sets the minimum version of CMake required to build the native
# library. You should either keep the default value or only pass a
# value of 3.4.0 or lower.

cmake_minimum_required(VERSION 3.4.1)


##### set general env variables
set(SRC_DIR ${CMAKE_SOURCE_DIR}/src/main/cpp/)
set(DISTRIBUTION_DIR ${CMAKE_SOURCE_DIR}/../distribution/)


##### add opencv libraries
add_library(libopencv_core STATIC IMPORTED)
set_target_properties(libopencv_core PROPERTIES IMPORTED_LOCATION
    ${DISTRIBUTION_DIR}/opencv/libs/${ANDROID_ABI}/libopencv_core.a)

add_library(libopencv_imgcodecs STATIC IMPORTED)
set_target_properties(libopencv_imgcodecs PROPERTIES IMPORTED_LOCATION
    ${DISTRIBUTION_DIR}/opencv/libs/${ANDROID_ABI}/libopencv_imgcodecs.a)

add_library(libopencv_imgproc STATIC IMPORTED)
set_target_properties(libopencv_imgproc PROPERTIES IMPORTED_LOCATION
    ${DISTRIBUTION_DIR}/opencv/libs/${ANDROID_ABI}/libopencv_imgproc.a)

add_library(libopencv_features2d STATIC IMPORTED)
set_target_properties(libopencv_features2d PROPERTIES IMPORTED_LOCATION
    ${DISTRIBUTION_DIR}/opencv/libs/${ANDROID_ABI}/libopencv_features2d.a)

add_library(libopencv_flann STATIC IMPORTED)
set_target_properties(libopencv_flann PROPERTIES IMPORTED_LOCATION
    ${DISTRIBUTION_DIR}/opencv/libs/${ANDROID_ABI}/libopencv_flann.a)

add_library(libopencv_calib3d STATIC IMPORTED)
set_target_properties(libopencv_calib3d PROPERTIES IMPORTED_LOCATION
    ${DISTRIBUTION_DIR}/opencv/libs/${ANDROID_ABI}/libopencv_calib3d.a)

add_library(libtbb STATIC IMPORTED)
set_target_properties(libtbb PROPERTIES IMPORTED_LOCATION
    ${DISTRIBUTION_DIR}/3rdparty/libs/${ANDROID_ABI}/libtbb.a)

add_library(liblibwebp STATIC IMPORTED)
set_target_properties(liblibwebp PROPERTIES IMPORTED_LOCATION
    ${DISTRIBUTION_DIR}/3rdparty/libs/${ANDROID_ABI}/liblibwebp.a)

add_library(liblibtiff STATIC IMPORTED)
set_target_properties(liblibtiff PROPERTIES IMPORTED_LOCATION
    ${DISTRIBUTION_DIR}/3rdparty/libs/${ANDROID_ABI}/liblibtiff.a)

add_library(liblibjasper STATIC IMPORTED)
set_target_properties(liblibjasper PROPERTIES IMPORTED_LOCATION
    ${DISTRIBUTION_DIR}/3rdparty/libs/${ANDROID_ABI}/liblibjasper.a)

add_library(liblibjpeg STATIC IMPORTED)
set_target_properties(liblibjpeg PROPERTIES IMPORTED_LOCATION
    ${DISTRIBUTION_DIR}/3rdparty/libs/${ANDROID_ABI}/liblibjpeg.a)

add_library(liblibpng STATIC IMPORTED)
set_target_properties(liblibpng PROPERTIES IMPORTED_LOCATION
    ${DISTRIBUTION_DIR}/3rdparty/libs/${ANDROID_ABI}/liblibpng.a)

add_library(libIlmImf STATIC IMPORTED)
set_target_properties(libIlmImf PROPERTIES IMPORTED_LOCATION
    ${DISTRIBUTION_DIR}/3rdparty/libs/${ANDROID_ABI}/libIlmImf.a)


##### builds the final native library
add_library(native-lib SHARED
            ${SRC_DIR}/native-lib.cpp)


##### include opencv header files
target_include_directories(native-lib PRIVATE
    ${DISTRIBUTION_DIR}/opencv/include)


##### links ndk libraries
find_library(z-lib z)
find_library(log-lib log)


##### combine everything
target_link_libraries(native-lib
                      libopencv_calib3d
                      libopencv_flann
                      libopencv_features2d
                      libopencv_imgcodecs
                      libopencv_imgproc
                      libopencv_core
                      liblibjasper
                      liblibpng
                      liblibjpeg
                      liblibwebp
                      liblibtiff
                      libIlmImf
                      libtbb
                      ${z-lib}
                      ${log-lib})
