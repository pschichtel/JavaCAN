cmake_minimum_required(VERSION 3.17)
project(javacan-jni C)

set(CMAKE_C_STANDARD 99)

option(IS_RELEASE "Whether this is a release build" OFF)
option(MAVEN_VERSION "The version of the maven project")

find_package(Java 1.8 REQUIRED)

include_directories(
        src/include
        "$ENV{JAVA_HOME}/include"
        "$ENV{JAVA_HOME}/include/linux"
        build/jni)


add_compile_options(-Werror -fPIC -D "MVN_VERSION=${MAVEN_VERSION}")
if(IS_RELEASE)
    add_compile_options(-O2 -flto)
else()
    add_compile_options(-g3 -Og)
endif()

add_link_options(-z noexecstack -fvisibility=hidden)

add_library(javacan-core SHARED
        src/main/c/common.c
        src/main/c/common.h
        src/main/c/javacan_bcm.c
        src/main/c/javacan_bufferhelper.c
        src/main/c/javacan_networkdevice.c
        src/main/c/javacan_socketcan.c
        src/main/c/javacan_j1939.c
        src/main/c/javacan_isotp.c
        src/main/c/javacan_raw.c)
