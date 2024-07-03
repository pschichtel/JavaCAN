plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    api(plugin("tel.schich.dockcross", version = "0.1.1"))
}

fun plugin(id: String, version: String) = "$id:$id.gradle.plugin:$version"
