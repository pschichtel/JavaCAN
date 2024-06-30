plugins {
    id("tel.schich.javacan.convention.arch-detect")
}

dependencies {
    api(project(":epoll"))
    configurations["nativeLibs"](project(mapOf("path" to ":epoll", "configuration" to "archDetectConfiguration")))
}
