plugins {
    id("tel.schich.javacan.convention.published")
}

dependencies {
    implementation(project(":core-arch-detect"))
}

tasks.withType<Test>().configureEach {
    enabled = false
}
