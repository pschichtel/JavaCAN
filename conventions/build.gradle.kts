plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    api(plugin("io.github.gradle-nexus.publish-plugin", "1.3.0"))
}

fun plugin(id: String, version: String) = "$id:$id.gradle.plugin:$version"
