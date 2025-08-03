rootProject.name = "javacan"

includeBuild("conventions")
pluginManagement {
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include("core")
include("core-arch-detect")

include("epoll")
include("epoll-arch-detect")

include("tools")
