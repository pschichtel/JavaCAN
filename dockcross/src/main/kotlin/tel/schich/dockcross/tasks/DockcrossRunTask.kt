package tel.schich.dockcross.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import java.nio.file.Path
import java.nio.file.Paths

data class ExecutionRequest(
    val image: String,
    val command: List<String>,
    val runAs: Pair<Int, Int>?,
    val mountSource: Path,
    val workdir: Path,
    val toolchainHome: Path?,
    val containerName: String?,
)

interface CliDispatcher {
    fun execute(workdir: Path, command: List<String>)
}

class DefaultCliDispatcher() : CliDispatcher {
    override fun execute(workdir: Path, command: List<String>) {
        val process = ProcessBuilder(command).apply {
            directory(workdir.toFile())
            inheritIO()
        }.start()
        val result = process.waitFor()
        if (result != 0) {
            error("Command failed: $result")
        }
    }
}

interface ContainerRunner {
    fun run(cli: CliDispatcher, request: ExecutionRequest)
}

abstract class DockerLikeRunner(private val mainCommand: String) : ContainerRunner {
    final override fun run(cli: CliDispatcher, request: ExecutionRequest) {
        val workdir = request.mountSource.relativize(request.workdir)
        fun MutableList<String>.bindMount(from: String, to: String = from) {
            add("-v")
            add("$from:$to")
        }
        fun MutableList<String>.env(name: String, value: String) {
            add("-e")
            add("$name=$value")
        }
        val command = buildList {
            add(mainCommand)
            add("run")
            add("--rm")
            add("--tty")
            request.containerName?.let {
                add("--name")
                add(it)
            }
            request.runAs?.let { (uid, gid) ->
                add("-u")
                add("$uid:$gid")
            }
            bindMount(request.mountSource.toString(), "/work")
            request.toolchainHome?.let {
                bindMount(it.toString())
                env("JAVA_HOME", it.toString())
            }
            add("--workdir")
            add("/work/$workdir")
            add(request.image)
            addAll(request.command)
        }
        println("Command: ${command.joinToString(" ")}")
        cli.execute(Paths.get("."), command)
    }
}

class DockerRunner(binary: String = "docker") : DockerLikeRunner(binary)
data class PodmanRunner(val binary: String = "podman") : DockerLikeRunner(binary)

abstract class DockcrossRunTask : DefaultTask() {
    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFiles
    val mountSource: DirectoryProperty = project.objects.directoryProperty()

    @get:Input
    val containerName: Property<String> = project.objects.property()

    @get:Input
    val architecture: Property<String> = project.objects.property()

    @get:Input
    val dockcrossTag: Property<String> = project.objects.property()

    @get:Input
    val dockcrossRepository: Property<String> = project.objects.property()

    @get:Input
    val script: ListProperty<List<String>> = project.objects.listProperty()

    @get:InputDirectory
    val javaHome: DirectoryProperty = project.objects.directoryProperty()

    @get:OutputDirectory
    val output: DirectoryProperty = project.objects.directoryProperty()

    private var runner: ContainerRunner = PodmanRunner()

    init {
        mountSource.convention(project.layout.projectDirectory)
        dockcrossTag.convention("latest")
        dockcrossRepository.convention("docker.io/dockcross/linux-{arch}")
        architecture.convention("x64")
        output.convention(project.layout.buildDirectory)
        containerName.convention("")
    }

    @TaskAction
    fun run() {
        output.get().asFile.mkdirs()
        val dispatcher = DefaultCliDispatcher()
        val toolchainHome = javaHome.orNull?.asFile?.toPath()
            ?: System.getenv("JAVA_HOME")?.ifEmpty { null }?.let { Paths.get(it) }


        val arch = architecture.get()
        val repo = dockcrossRepository.get().replace("{arch}", arch)
        val image = "$repo:${dockcrossTag.get()}"
        for (command in script.get()) {
            val request = ExecutionRequest(
                image = image,
                containerName = containerName.orNull?.ifEmpty { null },
                command = command,
                runAs = null,
                mountSource = mountSource.get().asFile.toPath(),
                workdir = output.get().asFile.toPath(),
                toolchainHome = toolchainHome
            )
            runner.run(dispatcher, request)
        }
    }
}
