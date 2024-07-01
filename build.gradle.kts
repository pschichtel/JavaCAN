plugins {
    `maven-publish`
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

if (!JavaVersion.current().isJava11Compatible) {
    throw GradleException("This build must be run with Java 11 or newer. Don't worry, it still targets Java 8!")
}

allprojects {
    group = "tel.schich"
    version = "3.5.0-SNAPSHOT"
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

val licenseHeaders by tasks.registering(DefaultTask::class) {
    group = "verification"

    project.subprojects
        .flatMap { it.tasks }
        .filter { it.name == "spotlessJavaApply" }
        .forEach {
            this@registering.dependsOn(it)
        }
}
