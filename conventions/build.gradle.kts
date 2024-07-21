plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    api(plugin("tel.schich.dockcross", version = "0.2.3"))
}

fun plugin(id: String, version: String) = "$id:$id.gradle.plugin:$version"
