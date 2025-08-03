import io.github.zenhelix.gradle.plugin.extension.PublishingType

plugins {
    id("tel.schich.javacan.convention.base")
    signing
    `maven-publish`
    id("io.github.zenhelix.maven-central-publish")
}

val ci = System.getenv("CI") != null

java {
    withSourcesJar()
    withJavadocJar()
}

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

mavenCentralPortal {
    credentials {
        username = project.getSecret("MAVEN_CENTRAL_PORTAL_USERNAME")
        password = project.getSecret("MAVEN_CENTRAL_PORTAL_PASSWORD")
    }
    publishingType = PublishingType.AUTOMATIC
}

publishing {
    repositories {
        maven {
            name = "mavenCentralSnapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
            credentials(PasswordCredentials::class)
        }
    }

    publications {
        register<MavenPublication>("maven") {
            artifactId = "javacan-${project.name}"
            from(components["java"])

            pom {
                name = "JavaCAN"
                description = project.description
                url = "https://github.com/pschichtel/JavaCAN"
                licenses {
                    license {
                        name = "MIT"
                        url = "https://opensource.org/licenses/MIT"
                    }
                }
                developers {
                    developer {
                        id.set("pschichtel")
                        name.set("Phillip Schichtel")
                        email.set("phillip@schich.tel")
                    }
                }
                scm {
                    url.set("https://github.com/pschichtel/JavaCAN")
                    connection.set("scm:git:https://github.com/pschichtel/JavaCAN")
                    developerConnection.set("scm:git:git@github.com:pschichtel/JavaCAN")
                }
            }
        }
    }
}

private val signingKey = System.getenv("SIGNING_KEY")?.ifBlank { null }?.trim()
private val signingKeyPassword = System.getenv("SIGNING_KEY_PASSWORD")?.ifBlank { null }?.trim() ?: ""

when {
    signingKey != null -> {
        logger.lifecycle("Received a signing key, using in-memory pgp keys!")
        signing {
            useInMemoryPgpKeys(signingKey, signingKeyPassword)
            sign(publishing.publications)
        }
    }
    !ci -> {
        logger.lifecycle("Not running in CI, using the gpg command!")
        signing {
            useGpgCmd()
            sign(publishing.publications)
        }
    }
    else -> {
        logger.lifecycle("Not signing artifacts!")
    }
}
