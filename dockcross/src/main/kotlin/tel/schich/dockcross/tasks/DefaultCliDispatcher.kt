package tel.schich.dockcross.tasks

import java.nio.file.Path

class DefaultCliDispatcher() : CliDispatcher {
    override fun execute(workdir: Path, command: List<String>) {
        val process = ProcessBuilder(command)
            .inheritIO()
            .directory(workdir.toFile())
            .start()
        val result = process.waitFor()
        if (result != 0) {
            error("Command failed: $result")
        }
    }
}
