package array.gui.graph

import array.*
import javafx.application.Platform
import javafx.scene.Scene
import javafx.stage.Stage

class LineGraphFunction : APLFunctionDescriptor {
    class LineGraphFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val data = GraphController.makeSeriesList(a, pos)
            Platform.runLater {
                GraphController.openWindowWithData(data)
            }
            return a
        }
    }

    override fun make(pos: Position) = LineGraphFunctionImpl(pos)
}


class GraphModule : KapModule {
    override val name get() = "graph"

    override fun init(engine: Engine) {
        val ns = engine.makeNamespace("graph")
        engine.registerFunction(ns.internAndExport("lineGraph"), LineGraphFunction())
    }
}
