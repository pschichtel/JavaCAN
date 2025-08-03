plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    api(plugin("tel.schich.dockcross", version = "0.2.3"))
    implementation("io.github.zenhelix.maven-central-publish:io.github.zenhelix.maven-central-publish.gradle.plugin:0.8.0")
}

fun plugin(id: String, version: String) = "$id:$id.gradle.plugin:$version"
