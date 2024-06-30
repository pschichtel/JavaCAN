package tel.schich.dockcross.execute

import java.nio.file.Paths

class DockerRunner(binary: String = "docker") : DockerLikeRunner(binary)
data class PodmanRunner(val binary: String = "podman") : DockerLikeRunner(binary)

abstract class DockerLikeRunner(private val mainCommand: String) : ContainerRunner {
    final override fun run(cli: CliDispatcher, request: ExecutionRequest) {
        val mountPoint = "/work"
        val workdir = request.mountSource.relativize(request.workdir)
        fun MutableList<String>.bindMount(from: String, to: String = from, readOnly: Boolean = false) {
            val roFlag = if (readOnly) ":ro" else ""
            add("-v")
            add("$from:$to$roFlag")
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
            bindMount(request.mountSource.toString(), mountPoint)
            request.toolchainHome?.let {
                val path = "/java-toolchain"
                bindMount(it.toString(), path, readOnly = true)
                env("JAVA_HOME", path)
            }
            add("--workdir")
            add("$mountPoint/$workdir")
            add(request.image)
            addAll(request.command)
        }
        println("Command: ${command.joinToString(" ")}")
        cli.execute(Paths.get("."), command)
    }
}
