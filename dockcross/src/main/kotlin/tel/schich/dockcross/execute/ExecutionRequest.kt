package tel.schich.dockcross.execute

import java.nio.file.Path

data class ExecutionRequest(
    val image: String,
    val command: List<String>,
    val runAs: Pair<Int, Int>?,
    val mountSource: Path,
    val workdir: Path,
    val toolchainHome: Path?,
    val containerName: String?,
)
