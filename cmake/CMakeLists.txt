cmake_minimum_required(VERSION 3.17)
project(javacan-jni C)

set(CMAKE_C_STANDARD 99)

include_directories(../core/src/include)
include_directories(../core/src/include/linux)
include_directories(../core/src/include/linux/can)
include_directories(../core/src/main/c)
include_directories(../core/target/java-jni-headers)
include_directories(../core/target/java-jni-headers/linux)
include_directories(../core/target/jni)
include_directories(../epoll/target/jni)

add_library(javacan-core
        ../core/src/main/c/common.c
        ../core/src/main/c/common.h
        ../core/src/main/c/javacan_bcmmessage.c
        ../core/src/main/c/javacan_bufferhelper.c
        ../core/src/main/c/javacan_networkdevice.c
        ../core/src/main/c/javacan_socketcan.c)

add_library(javacan-epoll
        ../epoll/src/main/c/javacan_epoll.c)
