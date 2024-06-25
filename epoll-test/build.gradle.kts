plugins {
    id("tel.schich.javacan.convention.test")
}

dependencies {
    implementation(project(":epoll"))
    implementation(project(":core-test"))
    // pull native stuff
}
