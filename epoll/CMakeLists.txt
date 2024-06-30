cmake_minimum_required(VERSION 3.17)
project(javacan-epoll C)

include(../common.cmake)

add_library(javacan-epoll SHARED
        build/jni/epoll/jni-c-to-java.c
        src/main/c/javacan_epoll.c)
