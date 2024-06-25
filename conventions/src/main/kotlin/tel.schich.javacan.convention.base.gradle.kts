plugins {
    `java-library`
}

java {
    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(8)
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
}

