cmake_minimum_required(VERSION 3.17)
project(javacan-jni C)

set(CMAKE_C_STANDARD 99)

include_directories(../core/src/include)
include_directories(../core/target/java-jni-headers)
include_directories(../core/target/java-jni-headers/linux)
include_directories(../core/target/jni)
include_directories(../epoll/target/jni)

add_library(javacan-core
        ../core/src/main/c/common.c
        ../core/src/main/c/common.h
        ../core/src/main/c/javacan_bcm.c
        ../core/src/main/c/javacan_bufferhelper.c
        ../core/src/main/c/javacan_networkdevice.c
        ../core/src/main/c/javacan_socketcan.c
        ../core/src/main/c/javacan_j1939.c
        ../core/src/main/c/javacan_isotp.c
        ../core/src/main/c/javacan_raw.c)

add_library(javacan-epoll
        ../epoll/src/main/c/javacan_epoll.c)
