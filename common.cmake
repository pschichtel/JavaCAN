set(CMAKE_C_STANDARD 99)

option(PROJECT_VERSION "The version of the maven project" "unspecified")
option(IS_RELEASE "Whether this is a release build" OFF)
option(LINK_STATICALLY "Whether this is a release build" OFF)

include_directories(
        src/include
        "$ENV{JAVA_HOME}/include"
        "$ENV{JAVA_HOME}/include/linux"
        build/jni)

add_compile_options(-Werror -fPIC -D "PROJECT_VERSION=${PROJECT_VERSION}")
if(IS_RELEASE)
    add_compile_options(-O2 -flto)
else()
    add_compile_options(-g3 -Og)
endif()

add_link_options(-z noexecstack -fvisibility=hidden)
if(LINK_STATICALLY)
    add_link_options(-static)
endif()
