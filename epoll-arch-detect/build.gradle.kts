plugins {
    id("tel.schich.javacan.convention.arch-detect")
}

val nativeLibs by configurations.getting

dependencies {
    api(project(":epoll"))
    nativeLibs(project(mapOf("path" to ":epoll", "configuration" to Constants.ARCH_DETECT_CONFIG)))
}

publishing.publications.withType<MavenPublication>().configureEach {
    pom {
        description = "${rootProject.description} The ${project.name} module bundles all architectures and allows runtime architecture detection."
    }
}
    
