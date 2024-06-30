plugins {
    id("tel.schich.javacan.convention.arch-detect")
}

val nativeLibs = configurations.named("nativeLibs")

dependencies {
    api(project(":epoll"))
    nativeLibs(project(mapOf("path" to ":epoll", "configuration" to "archDetectConfiguration")))
}
