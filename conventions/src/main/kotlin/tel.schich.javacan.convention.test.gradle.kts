plugins {
    id("tel.schich.javacan.convention.base")
}

dependencies {
    testImplementation("ch.qos.logback:logback-classic:1.3.14")

    val junitMinor = "10.2"
    val junitVersion = "5.$junitMinor"
    val junitRunnerVersion = "1.$junitMinor"
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.junit.platform:junit-platform-launcher:$junitRunnerVersion")
    testImplementation("org.junit.platform:junit-platform-runner:$junitRunnerVersion")
}

testing {
}
