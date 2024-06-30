plugins {
    id("tel.schich.javacan.convention.arch-detect")
}

val nativeLibs = configurations.named("nativeLibs")

dependencies {
    api(project(":core"))
    nativeLibs(project(mapOf("path" to ":core", "configuration" to "archDetectConfiguration")))
}
