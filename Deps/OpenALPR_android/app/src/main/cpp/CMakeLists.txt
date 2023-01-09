# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

#cmake_minimum_required(VERSION 3.10.2)
cmake_minimum_required(VERSION 3.4.1)

# Declares and names the project.

project("openalpr_android")

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
#             native-lib.cpp )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.
#
#find_library( # Sets the name of the path variable.
#              log-lib
#
#              # Specifies the name of the NDK library that
#              # you want CMake to locate.
#              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

#target_link_libraries( # Specifies the target library.
#                       native-lib
#
#                       # Links the target library to the log library
#                       # included in the NDK.
#                       ${log-lib} )



# Tesseract requires newer C++ standard, so let's enable it for all libraries
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Adds the CMakeLists.txt file located in the specified directory as a build dependency.
include(ndk-stl-config.cmake)









# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
# Set the OpenALPR version in cmake, and also add it as a DEFINE for the code to access
SET(OPENALPR_MAJOR_VERSION "2")
SET(OPENALPR_MINOR_VERSION "3")
SET(OPENALPR_PATCH_VERSION "0")
SET(OPENALPR_VERSION ${OPENALPR_MAJOR_VERSION}.${OPENALPR_MINOR_VERSION}.${OPENALPR_PATCH_VERSION})

add_definitions( -DOPENALPR_MAJOR_VERSION=${OPENALPR_MAJOR_VERSION})
add_definitions( -DOPENALPR_MINOR_VERSION=${OPENALPR_MINOR_VERSION})
add_definitions( -DOPENALPR_PATCH_VERSION=${OPENALPR_PATCH_VERSION})


set(OPENALPR_PATH "openalpr/src/openalpr/")
set(TESS_PATH "tesseract/") # incluye leptonica y otros
set(OPENCV_PATH "opencv/")
set(OPENCV_INCLUDE_PATH "./opencv/include/")
set(TESS_INCLUDE_PATH "./tesseract/include") # incluye leptonica y otros
set(OPENCV_LIB_PATH "${PROJECT_SOURCE_DIR}/opencv/${ANDROID_ABI}/libopencv_java4.so")
#set(OPENCV_LIB_PATH "${PROJECT_SOURCE_DIR}/opencv/${ANDROID_ABI}/libopencv_java3.so")
set(TESS_LIB_PATH "${PROJECT_SOURCE_DIR}/tesseract/${ANDROID_ABI}/libtesseract.so")
set(LEPT_LIB_PATH "${PROJECT_SOURCE_DIR}/tesseract/${ANDROID_ABI}/libleptonica.so")


file(GLOB openalpr_src
        ${OPENALPR_PATH}alpr.cpp
        ${OPENALPR_PATH}alpr_impl.cpp
        ${OPENALPR_PATH}config.cpp
        ${OPENALPR_PATH}config_helper.cpp
        ${OPENALPR_PATH}detection/detector.cpp
        ${OPENALPR_PATH}detection/detectorcpu.cpp
        ${OPENALPR_PATH}detection/detectorcuda.cpp
        ${OPENALPR_PATH}detection/detectorocl.cpp
        ${OPENALPR_PATH}detection/detectorfactory.cpp
        ${OPENALPR_PATH}detection/detectormorph.cpp
        ${OPENALPR_PATH}detection/detectormask.cpp
        ${OPENALPR_PATH}licenseplatecandidate.cpp
        ${OPENALPR_PATH}utility.cpp
        ${OPENALPR_PATH}ocr/tesseract_ocr.cpp
        ${OPENALPR_PATH}ocr/ocr.cpp
        ${OPENALPR_PATH}ocr/ocrfactory.cpp
        ${OPENALPR_PATH}postprocess/postprocess.cpp
        ${OPENALPR_PATH}postprocess/regexrule.cpp
        ${OPENALPR_PATH}binarize_wolf.cpp
        ${OPENALPR_PATH}ocr/segmentation/charactersegmenter.cpp
        ${OPENALPR_PATH}ocr/segmentation/histogram.cpp
        ${OPENALPR_PATH}ocr/segmentation/histogramvertical.cpp
        ${OPENALPR_PATH}ocr/segmentation/histogramhorizontal.cpp
        ${OPENALPR_PATH}edges/edgefinder.cpp
        ${OPENALPR_PATH}edges/platecorners.cpp
        ${OPENALPR_PATH}edges/platelines.cpp
        ${OPENALPR_PATH}edges/textlinecollection.cpp
        ${OPENALPR_PATH}edges/scorekeeper.cpp
        ${OPENALPR_PATH}colorfilter.cpp
        ${OPENALPR_PATH}prewarp.cpp
        ${OPENALPR_PATH}transformation.cpp
        ${OPENALPR_PATH}textdetection/characteranalysis.cpp
        ${OPENALPR_PATH}textdetection/platemask.cpp
        ${OPENALPR_PATH}textdetection/textcontours.cpp
        ${OPENALPR_PATH}textdetection/textline.cpp
        ${OPENALPR_PATH}textdetection/linefinder.cpp
        ${OPENALPR_PATH}pipeline_data.cpp
        ${OPENALPR_PATH}cjson.c
        ${OPENALPR_PATH}motiondetector.cpp
        ${OPENALPR_PATH}result_aggregator.cpp
        )


# Enable use of NEON on armeabi-v7a
if (${ANDROID_ABI} STREQUAL "armeabi-v7a")
    add_definitions("-DHAVE_NEON")

    #set_source_files_properties(<file.cpp>
    #    PROPERTIES COMPILE_FLAGS -mfpu=neon)

    # Include cpu-features for runtime detection of NEON support
    include(AndroidNdkModules)
    android_ndk_import_module_cpufeatures()
    set(LIBRARY_CPU_FEATURES cpufeatures)
endif()


#file(GLOB openalpr_hdr
#        ${OPENALPR_PATH}*.h
#        ${OPENALPR_PATH}detection/*.h
#        ${OPENALPR_PATH}ocr/*.h
#        ${OPENALPR_PATH}ocr/segmentation/*.h
#        ${OPENALPR_PATH}edges/*.h
#        ${OPENALPR_PATH}postprocess/*.h
#        ${OPENALPR_PATH}textdetection/*.h
#
#        )


#file(GLOB openalpr_hdr
#        ${OPENALPR_PATH}include/*.h
#        ${OPENALPR_PATH}include/detection/*.h
#        ${OPENALPR_PATH}include/ocr/*.h
#        ${OPENALPR_PATH}include/ocr/segmentation/*.h
#        ${OPENALPR_PATH}include/edges/*.h
#        ${OPENALPR_PATH}include/postprocess/*.h
#        ${OPENALPR_PATH}include/textdetection/*.h
#
#
#       )

add_library( # Sets the name of the library.
        openalpr

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        ${openalpr_src}
        ${openalpr_hdr}
        )


add_library( # Sets the name of the library.
        opencv

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        IMPORTED
        )

set_property(TARGET opencv PROPERTY IMPORTED_LOCATION ${OPENCV_LIB_PATH})

add_library( # Sets the name of the library.
        leptonica

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        IMPORTED
        )

set_property(TARGET leptonica PROPERTY IMPORTED_LOCATION ${LEPT_LIB_PATH})

add_library( # Sets the name of the library.
        tesseract

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        IMPORTED
        )

set_property(TARGET tesseract PROPERTY IMPORTED_LOCATION ${TESS_LIB_PATH})

# Specifies a path to native header files.
include_directories(
        ${OPENALPR_PATH}
)


include_directories(
        ${OPENCV_INCLUDE_PATH}
)


include_directories(
        ${TESS_INCLUDE_PATH}
)



# Define preprocessor constants
add_definitions(-DHAVE_CONFIG_H)
add_definitions(-DGRAPHICS_DISABLED)
add_definitions(-DFAST_FLOAT)
add_definitions( -DINSTALL_PREFIX="${CMAKE_INSTALL_PREFIX}")


# Ignore some warnings during build
target_compile_options(openalpr PRIVATE "-Wno-null-dereference")


# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

add_subdirectory(${OPENALPR_PATH}simpleini)
add_subdirectory(${OPENALPR_PATH}support)
#add_subdirectory(${OPENALPR_PATH}/../bindings/java)
add_subdirectory(${OPENALPR_PATH}/../statedetection)


target_link_libraries( # Specifies the target library.
        openalpr

        # Links the target library to the z library included in the NDK.
        simpleini
        support
        statedetection

        atomic
        dl
        log
        ${LIBRARY_CPU_FEATURES}
        jnigraphics
        leptonica
        tesseract
        opencv
        )

##

set(jni_source_files
        ${OPENALPR_PATH}/../bindings/java/openalprjni.cpp
        )


add_library(openalprjni SHARED ${jni_source_files})

TARGET_LINK_LIBRARIES(openalprjni openalpr)

