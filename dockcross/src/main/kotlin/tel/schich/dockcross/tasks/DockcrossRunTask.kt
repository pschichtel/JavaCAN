package tel.schich.dockcross.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import org.gradle.process.ExecOperations
import java.io.File
import java.nio.file.Paths
import javax.inject.Inject

abstract class DockcrossRunTask @Inject constructor(private val execOps: ExecOperations) : DefaultTask() {
    @get:Input
    val mountSource: Property<File> = project.objects.property()

    @get:Input
    val containerName: Property<String> = project.objects.property()

    @get:Input
    val image: Property<String> = project.objects.property()

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
        mountSource.convention(project.layout.projectDirectory.asFile)
        dockcrossTag.convention("latest")
        dockcrossRepository.convention("docker.io/dockcross/{image}")
        image.convention("linux-x64")
        output.convention(project.layout.buildDirectory)
        containerName.convention("")
    }

    @TaskAction
    fun run() {
        output.get().asFile.mkdirs()
        val dispatcher = DefaultCliDispatcher(execOps)
        val toolchainHome = javaHome.orNull?.asFile?.toPath()
            ?: System.getenv("JAVA_HOME")?.ifEmpty { null }?.let { Paths.get(it) }


        val arch = image.get()
        val repo = dockcrossRepository.get().replace("{image}", arch)
        val image = "$repo:${dockcrossTag.get()}"
        for (command in script.get()) {
            val request = ExecutionRequest(
                image = image,
                containerName = containerName.orNull?.ifEmpty { null },
                command = command,
                runAs = null,
                mountSource = mountSource.get().toPath(),
                workdir = output.get().asFile.toPath(),
                toolchainHome = toolchainHome
            )
            runner.run(dispatcher, request)
        }
    }
}
