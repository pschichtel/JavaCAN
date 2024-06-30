cmake_minimum_required(VERSION 3.17)
project(javacan-core C)

include(../common.cmake)

add_library(javacan-core SHARED
        build/jni/core/jni-c-to-java.c
        src/main/c/common.c
        src/main/c/common.h
        src/main/c/javacan_bcm.c
        src/main/c/javacan_bufferhelper.c
        src/main/c/javacan_networkdevice.c
        src/main/c/javacan_socketcan.c
        src/main/c/javacan_j1939.c
        src/main/c/javacan_isotp.c
        src/main/c/javacan_raw.c)
