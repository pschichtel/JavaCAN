plugins {
    id("tel.schich.javacan.convention.base")
    signing
    `maven-publish`
}

val ci = System.getenv("CI") != null

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
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

if (!ci) {
    signing {
        useGpgCmd()
        sign(publishing.publications)
    }
}
