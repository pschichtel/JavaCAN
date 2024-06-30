import org.gradle.configurationcache.extensions.capitalized
import tel.schich.dockcross.tasks.DockcrossRunTask

plugins {
    id("tel.schich.javacan.convention.published")
    id("tel.schich.dockcross")
}

val archDetectConfiguration by configurations.registering {
    isCanBeConsumed = true
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
    val image: String?,
    val classifier: String,
    val mode: NativeLinkMode,
    val archDetect: Boolean,
)

val nativeGroup = "native"
val targets = buildList {
    fun MutableList<BuildTarget>.add(
        image: String,
        classifier: String,
        mode: NativeLinkMode = NativeLinkMode.DYNAMIC,
        archDetect: Boolean = false,
    ) = add(BuildTarget(image, classifier, mode, archDetect))

    add(image = "linux-x64", classifier = "x86_64", archDetect = true)
    add(image = "linux-x86", classifier = "x86_32", archDetect = true)
    add(image = "linux-armv5", classifier = "armv5")
    add(image = "linux-armv6", classifier = "armv6", archDetect = true)
    add(image = "linux-armv7", classifier = "armv7", archDetect = true)
    add(image = "linux-armv7a", classifier = "armv7a", archDetect = true)
    add(image = "linux-armv7l-musl", classifier = "armv7l", mode = NativeLinkMode.STATIC, archDetect = true)
    add(image = "linux-arm64", classifier = "aarch64", archDetect = true)
    add(image = "linux-riscv32", classifier = "riscv32", archDetect = true)
    add(image = "linux-riscv64", classifier = "riscv64", archDetect = true)
    add(image = "android-arm", classifier = "android-arm")
    add(image = "android-arm64", classifier = "android-arm64")
    add(image = "android-x86_64", classifier = "android-x86_64")
    add(image = "android-x86", classifier = "android-x86_32")

    project.findProperty("javacan.extra-archs")
        ?.toString()
        ?.ifEmpty { null }
        ?.split(",")
        ?.forEach {
            add(BuildTarget(image = null, classifier = it, mode = NativeLinkMode.DYNAMIC, archDetect = false))
        }
}

val compileNativeAll by tasks.registering(DefaultTask::class) {
    group = nativeGroup
}

val compileNativeAllExceptAndroid by tasks.registering(DefaultTask::class) {
    group = nativeGroup
}

val buildReleaseBinaries = project.findProperty("javacan.build-release-binaries")
    ?.toString()
    ?.ifEmpty { null }
    ?.toBooleanStrictOrNull()
    ?: !project.version.toString().endsWith("-SNAPSHOT")

fun Project.dockcrossProp(prop: String, classifier: String) = findProperty("dockcross.$prop.${classifier}")?.toString()

for (target in targets) {
    val classifier = target.classifier
    val dockcrossVersion = "20240418-88c04a4"
    val dockcrossImage = project.dockcrossProp(prop = "image", classifier)
        ?: target.image?.let{ "docker.io/dockcross/$it:$dockcrossVersion" }
        ?: error("No image configured for target: $target")

    val (repo, tag) = dockcrossImage.split(":", limit = 2)
    val linkMode = (project.dockcrossProp(prop = "link-mode", classifier) ?: target.mode.name)
        .uppercase().let(NativeLinkMode::valueOf)

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
        containerName = "dockcross-${project.name}-$classifier"
        output = buildOutputDir.get().dir("native")

        val relativePathToProject = output.get().asFile.toPath().relativize(project.layout.projectDirectory.asFile.toPath()).toString()
        val projectVersionOption = "-DPROJECT_VERSION=${project.version}"
        val releaseOption = "-DIS_RELEASE=${if (buildReleaseBinaries) "1" else "0"}"
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

    if (target.archDetect) {
        artifacts.add(archDetectConfiguration.name, packageNative)
    }
}
