import org.gradle.configurationcache.extensions.capitalized
import tel.schich.dockcross.tasks.DockcrossRunTask

plugins {
    id("tel.schich.javacan.convention.native")
    id("tel.schich.dockcross")
}

enum class NativeLinkMode {
    DYNAMIC,
    STATIC,
}

data class BuildTarget(
    val dockcrossArch: String,
    val classifier: String,
    val mode: NativeLinkMode = NativeLinkMode.DYNAMIC,
)

val nativeGroup = "native"
val targets = listOf(
    BuildTarget("x64", "x86_64"),
    BuildTarget("x86", "x86_32"),
    BuildTarget("armv5", "armv5"),
    BuildTarget("armv6", "armv6"),
    BuildTarget("armv7", "armv7"),
    BuildTarget("armv7a", "armv7a"),
    BuildTarget("armv7l-musl", "armv7l", mode = NativeLinkMode.STATIC),
    BuildTarget("arm64", "aarch64"),
    BuildTarget("riscv32", "riscv32"),
    BuildTarget("riscv64", "riscv64"),
    BuildTarget("android-arm", "android-arm"),
    BuildTarget("android-arm64", "android-arm64"),
    BuildTarget("android-x86_64", "android-x86_64"),
    BuildTarget("android-x86", "android-x86_32"),
)

val compileNativeAll by tasks.registering(DefaultTask::class) {
    group = nativeGroup
}

val compileNativeAllExceptAndroid by tasks.registering(DefaultTask::class) {
    group = nativeGroup
}

val isRelease = !project.version.toString().endsWith("-SNAPSHOT")

for ((dockcrossArch, classifier, linkMode) in targets) {
    val buildOutputDir = project.layout.buildDirectory.dir("dockcross/$dockcrossArch")

    val compileNative = tasks.register("compileNativeFor${dockcrossArch.capitalized()}", DockcrossRunTask::class) {
        group = nativeGroup
        inputs.dir(project.layout.projectDirectory.dir("src/include"))
        inputs.dir(project.layout.projectDirectory.dir("src/main/c"))

        dependsOn(tasks.compileJava)

        val toolchainHome = javaToolchains.launcherFor(java.toolchain).map { it.metadata.installationPath }
        mountSource = project.rootProject.layout.projectDirectory.asFile
        javaHome = toolchainHome
        dockcrossTag = "20240418-88c04a4"
        architecture = dockcrossArch
        output = buildOutputDir.map { it.dir("native") }

        val projectVersionOption = "-DPROJECT_VERSION=${project.version}"
        val releaseOption = "-DIS_RELEASE=${if (isRelease) "1" else "0"}"
        val linkStaticallyOption = "-DLINK_STATICALLY=${if (linkMode == NativeLinkMode.STATIC) "1" else "0"}"
        script = listOf(
            listOf("cmake", "../../../..", projectVersionOption, releaseOption, linkStaticallyOption),
            listOf("make", "-j${project.gradle.startParameter.maxWorkerCount}"),
        )
    }

    val packageNative = tasks.register("packageNativeFor${dockcrossArch.capitalized()}", Jar::class) {
        group = nativeGroup

        dependsOn(compileNative)

        from(buildOutputDir) {
            include("native/*.so")
        }


        archiveClassifier = classifier
    }

    compileNativeAll.configure {
        dependsOn(packageNative)
    }

    if (!classifier.startsWith("android-")) {
        compileNativeAllExceptAndroid.configure {
            dependsOn(packageNative)
        }
    }
}

