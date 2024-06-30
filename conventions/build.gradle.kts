plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    api("tel.schich.dockcross:dockcross")
}

fun plugin(id: String, version: String) = "$id:$id.gradle.plugin:$version"
