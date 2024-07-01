plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    api("tel.schich.dockcross:dockcross")
    api(plugin("com.diffplug.spotless", version = "7.0.0.BETA1"))
}

fun plugin(id: String, version: String) = "$id:$id.gradle.plugin:$version"
