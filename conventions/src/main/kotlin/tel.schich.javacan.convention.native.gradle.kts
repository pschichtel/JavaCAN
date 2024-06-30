import org.gradle.configurationcache.extensions.capitalized
import tel.schich.dockcross.tasks.DockcrossRunTask

plugins {
    id("tel.schich.javacan.convention.published")
    id("tel.schich.dockcross")
}

dependencies {
    "tel.schich:jni-access-generator:1.1.2".let {
        annotationProcessor(it)
        compileOnly(it)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Agenerate.jni.headers=true"))
    options.headerOutputDirectory = project.layout.buildDirectory.get().dir("jni").dir(project.name)
}

enum class NativeLinkMode {
    DYNAMIC,
    STATIC,
}

data class BuildTarget(
    val image: String,
    val classifier: String,
    val mode: NativeLinkMode = NativeLinkMode.DYNAMIC,
)

val nativeGroup = "native"
val targets = listOf(
    BuildTarget(image = "linux-x64", classifier = "x86_64"),
    BuildTarget(image = "linux-x86", classifier = "x86_32"),
    BuildTarget(image = "linux-armv5", classifier = "armv5"),
    BuildTarget(image = "linux-armv6", classifier = "armv6"),
    BuildTarget(image = "linux-armv7", classifier = "armv7"),
    BuildTarget(image = "linux-armv7a", classifier = "armv7a"),
    BuildTarget(image = "linux-armv7l-musl", classifier = "armv7l", mode = NativeLinkMode.STATIC),
    BuildTarget(image = "linux-arm64", classifier = "aarch64"),
    BuildTarget(image = "linux-riscv32", classifier = "riscv32"),
    BuildTarget(image = "linux-riscv64", classifier = "riscv64"),
    BuildTarget(image = "android-arm", classifier = "android-arm"),
    BuildTarget(image = "android-arm64", classifier = "android-arm64"),
    BuildTarget(image = "android-x86_64", classifier = "android-x86_64"),
    BuildTarget(image = "android-x86", classifier = "android-x86_32"),
)

val compileNativeAll by tasks.registering(DefaultTask::class) {
    group = nativeGroup
}

val compileNativeAllExceptAndroid by tasks.registering(DefaultTask::class) {
    group = nativeGroup
}

val isRelease = !project.version.toString().endsWith("-SNAPSHOT")

fun Project.dockcrossProp(prop: String, classifier: String, or: () -> String) = findProperty("dockcross.$prop.${classifier}")?.toString() ?: or()

for (target in targets) {
    val classifier = target.classifier
    val dockcrossVersion = "20240418-88c04a4"
    val dockcrossImage = project.dockcrossProp(prop = "image", classifier) { "docker.io/dockcross/${target.image}:$dockcrossVersion" }
    val (repo, tag) = dockcrossImage.split(":", limit = 2)
    val linkMode = project.dockcrossProp(prop = "link-mode", classifier) { target.mode.name }.uppercase().let(NativeLinkMode::valueOf)

    val buildOutputDir = project.layout.buildDirectory.dir("dockcross/$classifier")
    val taskSuffix = classifier.split("[_-]".toRegex()).joinToString(separator = "") { it.capitalized() }

    val compileNative = tasks.register("compileNativeFor$taskSuffix", DockcrossRunTask::class) {
        group = nativeGroup
        inputs.dir(project.rootProject.layout.projectDirectory.dir("core/src/include"))
        inputs.dir(project.layout.projectDirectory.dir("src/main/c"))

        dependsOn(tasks.compileJava)

        val toolchainHome = javaToolchains.launcherFor(java.toolchain).map { it.metadata.installationPath }
        mountSource = project.rootProject.layout.projectDirectory.asFile
        javaHome = toolchainHome
        dockcrossRepository = repo
        dockcrossTag = tag
        image = dockcrossImage
        output = buildOutputDir.get().dir("native")

        val relativePathToProject = output.get().asFile.toPath().relativize(project.layout.projectDirectory.asFile.toPath()).toString()
        val projectVersionOption = "-DPROJECT_VERSION=${project.version}"
        val releaseOption = "-DIS_RELEASE=${if (isRelease) "1" else "0"}"
        val linkStaticallyOption = "-DLINK_STATICALLY=${if (linkMode == NativeLinkMode.STATIC) "1" else "0"}"
        script = listOf(
            listOf("cmake", relativePathToProject, projectVersionOption, releaseOption, linkStaticallyOption),
            listOf("make", "-j${project.gradle.startParameter.maxWorkerCount}"),
        )
    }

    val packageNative = tasks.register("packageNativeFor$taskSuffix", Jar::class) {
        group = nativeGroup

        dependsOn(compileNative)

        from(buildOutputDir) {
            include("native/*.so")
        }

        archiveClassifier = classifier
    }

    publishing.publications.withType<MavenPublication>().configureEach {
        artifact(packageNative)
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
