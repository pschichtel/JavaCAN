package tel.schich.dockcross.execute

import org.gradle.process.ExecOperations
import java.nio.file.Path

class DefaultCliDispatcher(private val execOps: ExecOperations) : CliDispatcher {
    override fun execute(workdir: Path, command: List<String>) {
        val result = execOps.exec {
            commandLine(command)
            workingDir(workdir)
        }
        if (result.exitValue != 0) {
            error("Command failed: $result")
        }
    }
}
