package tel.schich.dockcross.execute

import java.nio.file.Path

interface CliDispatcher {
    fun execute(workdir: Path, command: List<String>, extraEnv: Map<String, String>)
}
