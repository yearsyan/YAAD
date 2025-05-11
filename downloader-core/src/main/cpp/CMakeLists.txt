cmake_minimum_required(VERSION 3.10.2)
project("downloader_core")

add_library(
        downloader-core
        SHARED
        lib.cpp
)

find_library(log-lib log)

target_link_libraries(
        downloader-core
        ${log-lib}
)
