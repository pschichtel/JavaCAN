plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    api(plugin("io.github.gradle-nexus.publish-plugin", version = "1.3.0"))
    api("tel.schich.dockcross:dockcross")
}

fun plugin(id: String, version: String) = "$id:$id.gradle.plugin:$version"
