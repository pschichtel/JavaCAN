plugins {
    id("tel.schich.javacan.convention.published")
}

val nativeLibs by configurations.registering

tasks.jar.configure {
    dependsOn(nativeLibs)
    for (jar in nativeLibs.get().resolvedConfiguration.resolvedArtifacts) {
        val classifier = jar.classifier ?: continue
        from(zipTree(jar.file)) {
            include("native/*.so")
            into(classifier)
        }
    }
}

tasks.withType<Test>().configureEach {
    enabled = false
}
