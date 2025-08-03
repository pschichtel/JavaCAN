plugins {
    id("tel.schich.javacan.convention.arch-detect")
}

val nativeLibs by configurations.getting

dependencies {
    api(project(":core"))
    nativeLibs(project(mapOf("path" to ":core", "configuration" to ARCH_DETECT_CONFIGURATION_NAME)))
}

publishing.publications.withType<MavenPublication>().configureEach {
    pom {
        description = "${rootProject.description} The ${project.name} module bundles all architectures and allows runtime architecture detection."
    }
}
