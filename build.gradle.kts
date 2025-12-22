import io.github.danielliu1123.deployer.PublishingType
import pl.allegro.tech.build.axion.release.domain.PredefinedVersionCreator

plugins {
    `maven-publish`
    alias(libs.plugins.axion)
    alias(libs.plugins.mavenDeployer)
    id("tel.schich.javacan.convention.root")
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}

description = "JavaCAN is a binding to Linux' socketcan subsystem that feels native to Java developers."

private fun Project.getSecret(name: String): Provider<String> = provider {
    val env = System.getenv(name)
        ?.ifBlank { null }
    if (env != null) {
        return@provider env
    }

    val propName = name.split("_")
        .map { it.lowercase() }
        .joinToString(separator = "") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
        .replaceFirstChar { it.lowercase() }

    property(propName) as String
}

deploy {
    // dirs to upload, they will all be packaged into one bundle
    dirs = provider {
        allprojects
            .map { it.layout.buildDirectory.dir("repo").get().asFile }
            .filter { it.exists() }
            .toList()
    }
    username = project.getSecret("MAVEN_CENTRAL_PORTAL_USERNAME")
    password = project.getSecret("MAVEN_CENTRAL_PORTAL_PASSWORD")
    publishingType = if (Constants.CI) {
        PublishingType.WAIT_FOR_PUBLISHED
    } else {
        PublishingType.USER_MANAGED
    }
}

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
val isSnapshot = project.version.toString().endsWith("-SNAPSHOT")

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

    val repo = if (isSnapshot) {
        Constants.SNAPSHOTS_REPO
    } else {
        dependsOn(tasks.deploy)
        Constants.RELEASES_REPO
    }
    for (project in allprojects) {
        val publishTasks = project.tasks
            .withType<PublishToMavenRepository>()
            .matching { it.repository.name == repo }
        dependsOn(publishTasks)
    }

    doFirst {
        if (isSnapshot) {
            logger.lifecycle("Snapshot deployment!")
        } else {
            logger.lifecycle("Release deployment!")
        }
    }
}
