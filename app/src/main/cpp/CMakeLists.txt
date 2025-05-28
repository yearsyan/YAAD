cmake_minimum_required(VERSION 3.10)

project(media LANGUAGES C CXX)

message(STATUS "Android ABI: ${CMAKE_ANDROID_ARCH_ABI}")

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

#add_compile_options(
#    -fno-exceptions
#    -fno-rtti
#    -fvisibility=hidden
#    -ffunction-sections
#    -fdata-sections
#)
#
#add_link_options(
#    -Wl,--gc-sections
#    -flto
#)

include_directories(${CMAKE_SOURCE_DIR}/include/ext/${CMAKE_ANDROID_ARCH_ABI})

add_library(media SHARED src/library.cpp)

target_link_directories(media PRIVATE ${CMAKE_SOURCE_DIR}/../jniLibs/${CMAKE_ANDROID_ARCH_ABI})
target_link_libraries(media ffmpeg log)

set_target_properties(media PROPERTIES
    CXX_VISIBILITY_PRESET hidden
    VISIBILITY_INLINES_HIDDEN ON)
