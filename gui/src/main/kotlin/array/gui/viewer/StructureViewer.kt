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
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.shape.Line
import javafx.stage.Stage
import java.lang.Double.max


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
        graphContentPane.children.clear()
        val node = GraphNodeRegistry.buildNode(GraphNodeRegistry.GraphNodeContext(graphContentPane), instr)
        return node
    }

    companion object {
        fun open(client: Client) {
            val loader = FXMLLoader(StructureViewer::class.java.getResource("structure-viewer.fxml"))
            val root: Parent = loader.load()
            val controller: StructureViewer = loader.getController()
            controller.client = client

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
data class Coord(val x: Double, val y: Double)

private const val NODE_SPACING_HORIZ = 50.0
private const val NODE_SPACING_VERT = 50.0

abstract class KNode {
    abstract fun bounds(): BoundsDimensions
    abstract fun computePosition(x: Double, y: Double)
    abstract fun connectPosition(): Coord
}

object GraphNodeRegistry {
    fun <T : Instruction> buildNode(context: GraphNodeContext, instr: T): KNode {
        return when (instr) {
            is FunctionCall1Arg -> makeFunctionCall1ArgNode(context, instr)
            is FunctionCall2Arg -> FunctionCall2ArgGraphNode(context, instr.fn, instr.leftArgs, instr.rightArgs)
            is LiteralInteger -> LiteralIntegerGraphNode(context, instr)
            is LiteralDouble -> LiteralDoubleGraphNode(context, instr)
            is LiteralComplex -> LiteralComplexGraphNode(context, instr)
            is VariableRef -> VariableRefGraphNode(context, instr)
            is Literal1DArray -> Literal1DArrayGraphNode(context, instr)
            else -> throw CreateGraphException("Unable to render instruction: ${instr}")
        }
    }

    private fun makeFunctionCall1ArgNode(context: GraphNodeContext, instr: FunctionCall1Arg): KNode {
        return when (val fn = instr.fn) {
            is FunctionCallChain.Chain3 -> Chain3GraphNode1Arg(context, fn, instr.rightArgs)
            else -> FunctionCall1ArgGraphNode(context, instr.fn, instr.rightArgs)
        }
    }

    class GraphNodeContext(val panel: Pane)
}

class LeftArgLine(x1: Double, y1: Double, x2: Double, y2: Double) : Line(x1, y1, x2, y2) {
    init {
        styleClass.add("left-arg-line")
    }
}

class RightArgLine(x1: Double, y1: Double, x2: Double, y2: Double) : Line(x1, y1, x2, y2) {
    init {
        styleClass.add("right-arg-line")
    }
}

open class FunctionCall1ArgGraphNode(val context: GraphNodeRegistry.GraphNodeContext, fn: APLFunction, rightArgs: Instruction) : KNode() {
    val label = LabelledContainer("Call1", Label(fnName(fn))).apply { styleClass.addAll("call1") }
    val rightNode = GraphNodeRegistry.buildNode(context, rightArgs)

    init {
        context.panel.children.add(label)
    }

    override fun bounds(): BoundsDimensions {
        val rBounds = rightNode.bounds()
        val width = max(rBounds.width, label.width)
        val height = rBounds.height + label.height + NODE_SPACING_VERT
        return BoundsDimensions(width, height)
    }

    override fun computePosition(x: Double, y: Double) {
        val bounds = bounds()
        label.layoutX = x + (bounds.width - label.width) / 2
        label.layoutY = y
        rightNode.computePosition(x + (bounds.width - rightNode.bounds().width) / 2, y + label.height + NODE_SPACING_VERT)

        val (rx, ry) = rightNode.connectPosition()
        context.panel.children.add(RightArgLine(label.layoutX + label.width / 2, label.layoutY + label.height, rx, ry))
    }

    override fun connectPosition() = Coord(label.layoutX + label.width / 2, label.layoutY)
}

open class FunctionCall2ArgGraphNode(
    val context: GraphNodeRegistry.GraphNodeContext,
    fn: APLFunction,
    leftArgs: Instruction,
    rightArgs: Instruction
) : KNode() {
    val label = LabelledContainer("Call2", Label(fnName(fn))).apply { styleClass.addAll("call2") }
    val leftNode = GraphNodeRegistry.buildNode(context, leftArgs)
    val rightNode = GraphNodeRegistry.buildNode(context, rightArgs)

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
        rightNode.bounds()

        label.layoutX = x + (bounds().width - label.width) / 2
        label.layoutY = y
        val argYPos = y + label.height + NODE_SPACING_VERT
        leftNode.computePosition(x, argYPos)
        val rightNodeX = x + lBounds.width + NODE_SPACING_HORIZ
        rightNode.computePosition(rightNodeX, argYPos)

        val (lx, ly) = leftNode.connectPosition()
        val line1 = LeftArgLine(label.layoutX + label.width / 2, label.layoutY + label.height, lx, ly)
        context.panel.children.add(line1)

        val (rx, ry) = rightNode.connectPosition()
        val line2 = RightArgLine(label.layoutX + label.width / 2, label.layoutY + label.height, rx, ry)
        context.panel.children.add(line2)
    }

    override fun connectPosition() = Coord(label.layoutX + label.width / 2, label.layoutY)
}

abstract class SimpleLabelNode(val context: GraphNodeRegistry.GraphNodeContext, val label: Region, style: String) : KNode() {
    init {
        label.styleClass.add(style)
        context.panel.children.add(label)
    }

    override fun bounds() = BoundsDimensions(label.width, label.height)

    override fun computePosition(x: Double, y: Double) {
        label.layoutX = x
        label.layoutY = y
    }

    override fun connectPosition() = Coord(label.layoutX + label.width / 2, label.layoutY)
}

class LiteralIntegerGraphNode(context: GraphNodeRegistry.GraphNodeContext, instr: LiteralInteger) :
    SimpleLabelNode(context, LabelledContainer("Integer", Label(instr.value.toString())), "number-label")

class LiteralDoubleGraphNode(context: GraphNodeRegistry.GraphNodeContext, instr: LiteralDouble) :
    SimpleLabelNode(context, LabelledContainer("Double", Label(instr.value.toString())), "number-label")

class LiteralComplexGraphNode(context: GraphNodeRegistry.GraphNodeContext, instr: LiteralComplex) :
    SimpleLabelNode(context, LabelledContainer("Complex", Label("${instr.value.real}J${instr.value.imaginary}")), "number-label")

class VariableRefGraphNode(context: GraphNodeRegistry.GraphNodeContext, instr: VariableRef) :
    SimpleLabelNode(context, LabelledContainer("Variable Lookup", Label(instr.name.nameWithNamespace())), "variable-ref-label")

class Literal1DArrayGraphNode(context: GraphNodeRegistry.GraphNodeContext, instr: Literal1DArray) : KNode() {
    override fun bounds(): BoundsDimensions {
        TODO("not implemented")
    }

    override fun computePosition(x: Double, y: Double) {
        TODO("not implemented")
    }

    override fun connectPosition(): Coord {
        TODO("not implemented")
    }
}

class Chain3GraphNode1Arg(context: GraphNodeRegistry.GraphNodeContext, fn: FunctionCallChain.Chain3, rightArgs: Instruction) : KNode() {
    override fun bounds(): BoundsDimensions {
        TODO("not implemented")
    }

    override fun computePosition(x: Double, y: Double) {
        TODO("not implemented")
    }

    override fun connectPosition(): Coord {
        TODO("not implemented")
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
