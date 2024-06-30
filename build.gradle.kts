plugins {
    `maven-publish`
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
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
        .filter { it.name == "publishToMavenLocal" }
        .forEach {
            this@registering.dependsOn(it)
        }
}

val testAll by tasks.registering(DefaultTask::class) {
    group = "verification"

    project.subprojects
        .flatMap { it.tasks .withType<Test>() }
        .forEach {
            this@registering.dependsOn(it)
        }
}
