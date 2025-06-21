cmake_minimum_required(VERSION 3.10.2)
project("downloader_core")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_EXTENSIONS OFF)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fno-exceptions")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fno-rtti")
set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -Wl,--gc-sections")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -ffunction-sections -fdata-sections")

find_package(libtorrent REQUIRED CONFIG)
add_library(
        downloader-core
        SHARED
        lib.cpp bt.cpp bt.h
)

find_library(log-lib log)

target_link_libraries(
        downloader-core
        libtorrent::torrent-rasterbar
        ${log-lib}
)
