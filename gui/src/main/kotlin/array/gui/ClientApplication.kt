package array.gui

import array.options.ArgParser
import array.options.Option
import javafx.application.Application
import javafx.stage.Stage

class ClientApplication : Application() {
    private var client: Client? = null

    override fun start(stage: Stage) {
        val parser = ArgParser(Option("lib-path", true, "Path to add to search paths"))
        val options = parser.parse(parameters.raw.toTypedArray())
        val extraPaths = ArrayList<String>()
        System.getProperty("kap.installPath")?.let { appPath ->
            extraPaths.add("${appPath}/standard-lib")
        }
        options["lib-path"]?.let { path ->
            extraPaths.add(path)
        }
        client = Client(stage, extraPaths)
    }
}
