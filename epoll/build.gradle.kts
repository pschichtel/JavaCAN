plugins {
    id("tel.schich.javacan.convention.native")
}

dependencies {
    api(project(":core"))
    testImplementation(testFixtures(project(":core")))
}

publishing.publications.withType<MavenPublication>().configureEach {
    pom {
        description = "${rootProject.description} The ${project.name} module provides facilities for reactive IO using Linux' epoll subsystem."
    }
}
