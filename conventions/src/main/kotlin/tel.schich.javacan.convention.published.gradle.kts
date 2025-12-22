plugins {
    id("tel.schich.javacan.convention.base")
    signing
    `maven-publish`
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    repositories {
        maven {
            name = Constants.SNAPSHOTS_REPO
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
            credentials(PasswordCredentials::class)
        }
        maven {
            name = Constants.RELEASES_REPO
            url = layout.buildDirectory.dir("repo").get().asFile.toURI()
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
    !Constants.CI -> {
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
