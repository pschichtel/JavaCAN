plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "tel.schich.dockcross"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
}

gradlePlugin {
    plugins {
        create("dockcrossPlugin") {
            id = "tel.schich.dockcross"
            implementationClass = "tel.schich.dockcross.DockcrossPlugin"
        }
    }
}
