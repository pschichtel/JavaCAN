plugins {
    `java-library`
    `java-test-fixtures`
    id("tel.schich.javacan.convention.root")
}

val javaComponent = components["java"] as AdhocComponentWithVariants
javaComponent.withVariantsFromConfiguration(configurations["testFixturesApiElements"]) { skip() }
javaComponent.withVariantsFromConfiguration(configurations["testFixturesRuntimeElements"]) { skip() }

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }

    testing {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }
}

tasks.test {
    useJUnitPlatform()
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
    compileOnly(libs.jdtAnnotations)
    implementation(libs.slf4j)

    testImplementation(libs.logbackClassic)

    testRuntimeOnly(libs.junitPlatformLauncher)
    testFixturesImplementation(libs.junitJupiter)
    testImplementation(libs.junitJupiter)
}

