rootProject.name = "javacan"

pluginManagement {
    includeBuild("conventions")
    includeBuild("jni-cross")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

include("core")
include("core-arch-detect")
include("core-test")
include("epoll")
include("epoll-arch-detect")
include("epoll-test")
include("tools")
