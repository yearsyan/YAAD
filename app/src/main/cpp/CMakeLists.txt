cmake_minimum_required(VERSION 3.10)

project(media LANGUAGES C CXX)
message(STATUS "Android ABI: ${CMAKE_ANDROID_ARCH_ABI}")
set(CMAKE_C_STANDARD 11)

set(CMAKE_BUILD_TYPE Release)
find_package(ffmpeg REQUIRED CONFIG)
add_library(media SHARED library.c)
target_link_libraries(media ffmpeg::ffmpeg log)
