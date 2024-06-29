cmake_minimum_required(VERSION 3.17)
project(javacan-jni C)

set(CMAKE_C_STANDARD 99)

add_subdirectory(core)
add_subdirectory(epoll)
