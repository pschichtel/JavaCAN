import tel.schich.dockcross.execute.DockerRunner
import tel.schich.dockcross.execute.NonContainerRunner
import tel.schich.dockcross.tasks.DockcrossRunTask

plugins {
    id("tel.schich.javacan.convention.published")
    id("tel.schich.dockcross")
}

val ci = System.getenv("CI") != null

val archDetectConfiguration by configurations.registering {
    isCanBeConsumed = true
    isCanBeResolved = false
}

val jniGluePath: Directory = project.layout.buildDirectory.get().dir("jni/${project.name}")
tasks.compileJava.configure {
    options.compilerArgs.addAll(listOf("-Agenerate.jni.headers=true"))
    options.headerOutputDirectory = jniGluePath
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

val dockcrossVersion = "20250109-7bf589c"
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

fun DockcrossRunTask.baseConfigure(linkMode: NativeLinkMode, outputTo: Directory) {
    group = nativeGroup

    inputs.dir(project.rootProject.layout.projectDirectory.dir("core/src/include"))
    inputs.dir(project.layout.projectDirectory.dir("src/main/c"))
    inputs.dir(jniGluePath)
    mountSource = project.rootProject.layout.projectDirectory.asFile

    dependsOn(tasks.compileJava)

    javaHome = javaToolchains.launcherFor(java.toolchain).map { it.metadata.installationPath }
    output = outputTo.dir("native")

    val relativePathToProject = output.get().asFile.toPath().relativize(project.layout.projectDirectory.asFile.toPath()).toString()
    val projectVersionOption = "-DPROJECT_VERSION=${project.version}"
    val releaseOption = "-DIS_RELEASE=${if (buildReleaseBinaries) "1" else "0"}"
    val linkStaticallyOption = "-DLINK_STATICALLY=${if (linkMode == NativeLinkMode.STATIC) "1" else "0"}"
    script = listOf(
        listOf("cmake", relativePathToProject, projectVersionOption, releaseOption, linkStaticallyOption),
        listOf("make", "-j${project.gradle.startParameter.maxWorkerCount}"),
    )
}

fun Jar.baseConfigure(compileTask: TaskProvider<DockcrossRunTask>, buildOutputDir: Directory) {
    group = nativeGroup

    dependsOn(compileTask)

    from(buildOutputDir) {
        include("native/*.so")
    }
}

for (target in targets) {
    val classifier = target.classifier
    val dockcrossImage = project.dockcrossProp(prop = "image", classifier)
        ?: target.image?.let{ "docker.io/dockcross/$it:$dockcrossVersion" }
        ?: error("No image configured for target: $target")

    val (repo, tag) = dockcrossImage.split(":", limit = 2)
    val linkMode = (project.dockcrossProp(prop = "link-mode", classifier) ?: target.mode.name)
        .uppercase().let(NativeLinkMode::valueOf)

    val buildOutputDir = project.layout.buildDirectory.dir("dockcross/$classifier")
    val taskSuffix = classifier.split("[_-]".toRegex()).joinToString(separator = "") { it.lowercase().replaceFirstChar(Char::uppercase) }

    val compileNative = tasks.register("compileNativeFor$taskSuffix", DockcrossRunTask::class) {
        baseConfigure(linkMode, buildOutputDir.get())

        dockcrossRepository = repo
        dockcrossTag = tag
        image = dockcrossImage
        containerName = "dockcross-${project.name}-$classifier"

        if (ci) {
            runner(DockerRunner())
            doLast {
                providers.exec {
                    commandLine("docker", "image", "rm", "$repo:$tag")
                }.result.get()
            }
        }
    }


    val packageNative = tasks.register("packageNativeFor$taskSuffix", Jar::class) {
        baseConfigure(compileNative, buildOutputDir.get())

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

val nativeForHostOutputDir: Directory = project.layout.buildDirectory.dir("dockcross/host").get()
val compileNativeForHost by tasks.registering(DockcrossRunTask::class) {
    baseConfigure(NativeLinkMode.DYNAMIC, nativeForHostOutputDir)
    image = "host"
    runner(NonContainerRunner)
}

val packageNativeForHost by tasks.registering(Jar::class) {
    baseConfigure(compileNativeForHost, nativeForHostOutputDir)
    archiveClassifier = "host"
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    "tel.schich:jni-access-generator:1.1.8".also {
        annotationProcessor(it)
        compileOnly(it)
    }

    files(packageNativeForHost).also {
        testImplementation(it)
        testFixturesApi(it)
    }
}
