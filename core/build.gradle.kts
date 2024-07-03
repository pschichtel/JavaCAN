plugins {
    id("tel.schich.javacan.convention.native")
}

publishing.publications.withType<MavenPublication>().configureEach {
    pom {
        description = "${rootProject.description} The ${project.name} module provides the basic socketcan bindings."
    }
}
