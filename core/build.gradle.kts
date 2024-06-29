import org.gradle.configurationcache.extensions.capitalized
import tel.schich.dockcross.tasks.DockcrossRunTask

plugins {
    id("tel.schich.javacan.convention.native")
    id("tel.schich.dockcross")
}

val nativeGroup = "native"
val architectures = listOf("x64", "x86")

val compileNativeAll by tasks.registering(DefaultTask::class) {
    group = nativeGroup
}

for (arch in architectures) {
    val compileNative = tasks.register("compileNativeFor${arch.capitalized()}", DockcrossRunTask::class) {
        group = nativeGroup

        dependsOn(tasks.compileJava)

        val toolchainHome = javaToolchains.launcherFor(java.toolchain).map { it.metadata.installationPath }
        mountSource = project.rootProject.layout.projectDirectory
        javaHome = toolchainHome
        dockcrossTag = "20240418-88c04a4"
        architecture = arch
        output = project.layout.buildDirectory.dir("native/$arch")
        script = listOf(
            listOf("cmake", "../../..", "-DPROJECT_VERSION=${project.version}"),
            listOf("make", "-j8"),
        )
    }

    val packageNative = tasks.register("packageNativeFor${arch.capitalized()}", Jar::class) {
        group = nativeGroup

        dependsOn(compileNative)

        archiveClassifier = arch
    }

    compileNativeAll.configure {
        dependsOn(packageNative)
    }
}

