rootProject.name = "javacan"

pluginManagement {
    includeBuild("conventions")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

include("core")
include("core-arch-detect")

include("epoll")
include("epoll-arch-detect")

include("tools")
