plugins {
    `java-library`
    `java-test-fixtures`
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

    testImplementation("ch.qos.logback:logback-classic:1.3.14")

    val junitMinor = "10.2"
    val junitVersion = "5.$junitMinor"
//    val junitRunnerVersion = "1.$junitMinor"
    "org.junit.jupiter:junit-jupiter-engine:$junitVersion".also {
        testFixturesImplementation(it)
        testImplementation(it)
    }
//    testImplementation("org.junit.platform:junit-platform-launcher:$junitRunnerVersion")
//    testImplementation("org.junit.platform:junit-platform-runner:$junitRunnerVersion")
}

