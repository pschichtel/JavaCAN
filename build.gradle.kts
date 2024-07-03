plugins {
    `maven-publish`
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("pl.allegro.tech.build.axion-release") version "1.16.1" // this is the last version that supports Java 8
}

scmVersion {
    tag {
        prefix = "javacan-"
    }
    nextVersion {
        suffix = "SNAPSHOT"
        separator = "-"
    }
}

val gitVersion: String = scmVersion.version

allprojects {
    group = "tel.schich"
    version = gitVersion
}

nexusPublishing {
    this.repositories {
        sonatype()
    }
}

val publishAllToMavenLocal by tasks.registering(DefaultTask::class) {
    group = "publishing"

    project.subprojects
        .flatMap { it.tasks }
        .filter { it.enabled }
        .filter { it.name == "publishToMavenLocal" }
        .forEach {
            this@registering.dependsOn(it)
        }
}

val testAll by tasks.registering(DefaultTask::class) {
    group = "verification"

    project.subprojects
        .flatMap { it.tasks .withType<Test>() }
        .filter { it.enabled }
        .forEach {
            this@registering.dependsOn(it)
        }
}
