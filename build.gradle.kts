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
