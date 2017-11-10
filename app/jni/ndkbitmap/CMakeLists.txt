cmake_minimum_required(VERSION 3.4.1)
set(CMAKE_BUILD_TYPE Debug)

aux_source_directory(./ DIR_SRCS)

#增加Cpp动态共享库
add_library(ndkbitmap STATIC ${DIR_SRCS})

#target_include_directories(ndkbitmap PRIVATE
#                    ./AndroidSource/external/skia/include/core \
#					./AndroidSource/frameworks/base/native/include \
#					./AndroidSource/frameworks/base/core/jni/android/graphics \
#					./AndroidSource/frameworks/native/include \
#					./AndroidSource/external/skia/include/images \
#					./AndroidSource/external/skia/include/lazy \
#					./AndroidSource/frameworks/base/include \
#					./AndroidSource/system/core/include)
