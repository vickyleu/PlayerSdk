# documentation: https://d.android.com/studio/projects/add-native-code.html
cmake_minimum_required(VERSION 3.4.1)

#add_subdirectory(jni/ndkbitmap) //编译需要AOSP源码,太麻烦不玩了

add_library(native-lib   SHARED   src/main/cpp/native-lib.cpp )

find_library(log-lib   log )

target_link_libraries(native-lib    ${log-lib} )