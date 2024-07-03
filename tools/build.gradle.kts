plugins {
    id("tel.schich.javacan.convention.published")
}

dependencies {
    implementation(project(":core-arch-detect"))
}

tasks.withType<Test>().configureEach {
    enabled = false
}

publishing.publications.withType<MavenPublication>().configureEach {
    pom {
        description = "${rootProject.description} The ${project.name} module contains tools to work with socketcan and related tooling."
    }
}
