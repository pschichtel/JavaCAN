plugins {
    `java-library`
    `java-test-fixtures`
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

    testFixturesImplementation(libs.junitJupiter)
    testImplementation(libs.junitJupiter)
}

