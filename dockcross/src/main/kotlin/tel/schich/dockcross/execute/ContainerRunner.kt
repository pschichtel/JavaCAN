package tel.schich.dockcross.execute

interface ContainerRunner {
    fun run(cli: CliDispatcher, request: ExecutionRequest)
}
