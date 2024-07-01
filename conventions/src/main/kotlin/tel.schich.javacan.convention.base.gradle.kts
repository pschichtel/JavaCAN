import com.diffplug.gradle.spotless.SpotlessTask

plugins {
    `java-library`
    `java-test-fixtures`
    id("com.diffplug.spotless")
}

val javaComponent = components["java"] as AdhocComponentWithVariants
javaComponent.withVariantsFromConfiguration(configurations["testFixturesApiElements"]) { skip() }
javaComponent.withVariantsFromConfiguration(configurations["testFixturesRuntimeElements"]) { skip() }

java {
    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(8)
    }
}

spotless {
    java {
        licenseHeaderFile(project.rootProject.layout.projectDirectory.file("MIT-header.txt"), "(package|import)")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(
        listOf(
            "-Xlint:deprecation",
            "-Xlint:unchecked",
        )
    )
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.eclipse.jdt:org.eclipse.jdt.annotation:2.3.0")
    implementation("org.slf4j:slf4j-api:2.0.13")

    testImplementation("ch.qos.logback:logback-classic:1.3.14")

    "org.junit.jupiter:junit-jupiter-engine:5.10.2".also {
        testFixturesImplementation(it)
        testImplementation(it)
    }
}

