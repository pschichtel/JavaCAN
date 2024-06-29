plugins {
    id("tel.schich.javacan.convention.base")
}

dependencies {
    "tel.schich:jni-access-generator:1.1.2".let {
        annotationProcessor(it)
        compileOnly(it)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Agenerate.jni.headers=true"))
    options.headerOutputDirectory = project.layout.buildDirectory.dir("jni")
        .map { it.dir(project.name) }
}
