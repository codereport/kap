package array.gui

import array.options.ArgParser
import array.options.Option
import javafx.application.Application
import javafx.stage.Stage

class ClientApplication : Application() {
    private var client: Client? = null

    override fun start(stage: Stage) {
        val parser = ArgParser(Option("lib-path", true, "Path to add to search path"))
        val options = parser.parse(parameters.raw.toTypedArray())
        val path = options["lib-path"]
        val extraPaths = if (path == null) emptyList() else listOf(path)
        client = Client(stage, extraPaths)
    }

    override fun stop() {
        client!!.stopRequest()
        super.stop()
    }
}
