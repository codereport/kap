package array.gui.viewer

import array.*
import array.gui.Client
import javafx.event.ActionEvent
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import java.lang.Double.max
import kotlin.reflect.KClass


class StructureViewer {
    lateinit var graphContentPane: Pane
    lateinit var client: Client
    lateinit var expressionField: TextField
    lateinit var borderPane: BorderPane

    var rootNode: KNode? = null

    fun showClicked(@Suppress("UNUSED_PARAMETER") actionEvent: ActionEvent) {
        parseExpression(expressionField.text)
    }

    private fun parseExpression(text: String) {
        val instr = client.engine.parse(StringSourceLocation(text))
        val node = createGraph(instr)
        rootNode = node
        graphContentPane.applyCss()
        graphContentPane.layout()
        node.computePosition(0.0, 0.0)
        val bounds = node.bounds()
        graphContentPane.setPrefSize(bounds.width, bounds.height)
    }

    private fun createGraph(instr: Instruction): KNode {
        val node = GraphNodeRegistry.buildNode(GraphNodeRegistry.GraphNodeContext(graphContentPane), instr)
        return node
    }

    companion object {
        fun open(client: Client) {
            val loader = FXMLLoader(StructureViewer::class.java.getResource("structure-viewer.fxml"))
            val root: Parent = loader.load()
            val controller: StructureViewer = loader.getController()
            controller.client = client;

            val stage = Stage()
            val scene = Scene(root, 800.0, 800.0)
            stage.title = "Structure Viewer"
            stage.scene = scene
            stage.show()
        }
    }
}

class CreateGraphException(message: String) : Exception(message)

data class BoundsDimensions(val width: Double, val height: Double)

private const val NODE_SPACING_HORIZ = 50.0
private const val NODE_SPACING_VERT = 50.0

abstract class KNode {
    abstract fun bounds(): BoundsDimensions
    abstract fun computePosition(x: Double, y: Double)
}

object GraphNodeRegistry {
    private val nodeBuilders = HashMap<KClass<*>, (GraphNodeContext, Any) -> KNode>()

    private inline fun <reified T : Instruction> addNodeBuilder(noinline builder: (GraphNodeContext, T) -> KNode) {
        @Suppress("UNCHECKED_CAST")
        nodeBuilders[T::class] = builder as (GraphNodeContext, Any) -> KNode
    }

    fun <T : Instruction> buildNode(context: GraphNodeContext, instr: T): KNode {
        @Suppress("UNCHECKED_CAST")
        val builder = nodeBuilders[instr::class] as ((GraphNodeContext, T) -> KNode)?
        if (builder == null) {
            throw CreateGraphException("Unable to render instruction: ${instr}")
        }
        return builder(context, instr)
    }

    init {
        addNodeBuilder<FunctionCall2Arg> { context, instr -> FunctionCall2ArgGraphNode(context, instr) }
        addNodeBuilder<LiteralInteger> { context, instr -> LiteralIntegerGraphNode(context, instr) }
        addNodeBuilder<VariableRef> { context, instr -> VariableRefGraphNode(context, instr) }
    }

    class GraphNodeContext(val panel: Pane)
}

class FunctionCall2ArgGraphNode(context: GraphNodeRegistry.GraphNodeContext, instr: FunctionCall2Arg) : KNode() {
    val label = Label("Call2:${fnName(instr.fn)}").apply { styleClass.addAll("structure-label", "call2") }
    val leftNode = GraphNodeRegistry.buildNode(context, instr.leftArgs)
    val rightNode = GraphNodeRegistry.buildNode(context, instr.rightArgs)

    init {
        context.panel.children.add(label)
    }

    override fun bounds(): BoundsDimensions {
        val lBounds = leftNode.bounds()
        val rBounds = rightNode.bounds()
        val width = max(lBounds.width + rBounds.width + NODE_SPACING_HORIZ, label.width)
        val height = max(lBounds.height, rBounds.height) + label.height + NODE_SPACING_VERT
        return BoundsDimensions(width, height)
    }

    override fun computePosition(x: Double, y: Double) {
        val lBounds = leftNode.bounds()
        val rBounds = rightNode.bounds()

        label.layoutX = x + (bounds().width - label.width) / 2
        label.layoutY = y
        val argYPos = y + label.height + NODE_SPACING_VERT
        leftNode.computePosition(x, argYPos)
        rightNode.computePosition(x + lBounds.width + NODE_SPACING_HORIZ, argYPos)
    }
}

class LiteralIntegerGraphNode(val context: GraphNodeRegistry.GraphNodeContext, instr: LiteralInteger) : KNode() {
    val label = LabelledContainer("Integer", Label(instr.value.toString())).apply { styleClass.add("number-label") }

    init {
        context.panel.children.add(label)
    }

    override fun bounds() = BoundsDimensions(label.width, label.height)

    override fun computePosition(x: Double, y: Double) {
        label.layoutX = x
        label.layoutY = y
    }
}

class VariableRefGraphNode(val context: GraphNodeRegistry.GraphNodeContext, instr: VariableRef) : KNode() {
    //val label = Label("Var:${instr.name.nameWithNamespace()}").apply { styleClass.addAll("structureLabel", "variableRefLabel") }

    var label = LabelledContainer("Variable Lookup", Label(instr.name.nameWithNamespace()))

    init {
        context.panel.children.add(label)
    }

    override fun bounds() = BoundsDimensions(label.width, label.height)

    override fun computePosition(x: Double, y: Double) {
        label.layoutX = x
        label.layoutY = y
    }
}

private fun fnName(fn: APLFunction): String {
    val pos = fn.pos
    return when {
        pos.callerName != null -> pos.callerName!!
        pos.name != null -> pos.name!!
        else -> fn::class.simpleName ?: "unnamed"
    }
}

class LabelledContainer(labelText: String, node: Node) : VBox() {
    init {
        styleClass.add("labelled-container")
        children.add(Label(labelText).apply { styleClass.add("labelled-container-title") })
        children.add(node)
    }
}
