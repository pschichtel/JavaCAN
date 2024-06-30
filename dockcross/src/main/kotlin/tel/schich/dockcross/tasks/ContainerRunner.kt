package tel.schich.dockcross.tasks

interface ContainerRunner {
    fun run(cli: CliDispatcher, request: ExecutionRequest)
}
