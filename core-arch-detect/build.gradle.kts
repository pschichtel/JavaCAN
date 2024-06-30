plugins {
    id("tel.schich.javacan.convention.arch-detect")
}

dependencies {
    api(project(":core"))
    configurations["nativeLibs"](project(mapOf("path" to ":core", "configuration" to "archDetectConfiguration")))
}
