package tel.schich.dockcross.tasks

import java.nio.file.Path

interface CliDispatcher {
    fun execute(workdir: Path, command: List<String>)
}
