import io.github.zenhelix.gradle.plugin.task.PublishBundleMavenCentralTask
import pl.allegro.tech.build.axion.release.domain.PredefinedVersionCreator

plugins {
    `maven-publish`
    alias(libs.plugins.axion)
    alias(libs.plugins.mavenCentralPublish) apply false
}

description = "JavaCAN is a binding to Linux' socketcan subsystem that feels native to Java developers."

scmVersion {
    tag {
        prefix = "javacan-"
    }
    nextVersion {
        suffix = "SNAPSHOT"
        separator = "-"
    }
    versionCreator = PredefinedVersionCreator.SIMPLE.versionCreator
}

val gitVersion: String = scmVersion.version

allprojects {
    group = "tel.schich"
    version = gitVersion
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

val mavenCentralDeploy by tasks.registering(DefaultTask::class) {
    group = "publishing"
    val isSnapshot = project.version.toString().endsWith("-SNAPSHOT")

    if (isSnapshot) {
        logger.lifecycle("Snapshot deployment!")
        for (project in allprojects) {
            val tasks = project.tasks
                .withType<PublishToMavenRepository>()
                .matching { it.repository.name == "mavenCentralSnapshots" }
            dependsOn(tasks)
        }
    } else {
        logger.lifecycle("Release deployment!")
        for (project in allprojects) {
            val tasks = project.tasks
                .withType<PublishBundleMavenCentralTask>()
            dependsOn(tasks)
        }
    }
}
