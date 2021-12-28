package array.gui.viewer

import array.*
import array.gui.Client
import javafx.event.ActionEvent
import javafx.fxml.FXMLLoader
import javafx.scene.Group
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
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.stage.Stage
import kotlin.math.*

class StructureViewer {
    lateinit var graphContentPane: Pane
    lateinit var client: Client
    lateinit var expressionField: TextField
    lateinit var borderPane: BorderPane

    fun showClicked(@Suppress("UNUSED_PARAMETER") actionEvent: ActionEvent) {
        parseExpression(expressionField.text)
    }

    private fun parseExpression(text: String) {
        val text = "1 ((+-*)%!) foo"
        val instr = client.engine.parse(StringSourceLocation(text))
        graphContentPane.children.clear()
        val graph = createGraph(instr)
        val root = graph.rootNode
        if (root != null) {
            graphContentPane.applyCss()
            graphContentPane.layout()
            graph.updateNodes()
            val bounds = root.bounds()
            graphContentPane.setPrefSize(bounds.width, bounds.height)
        }
    }

    private fun createGraph(instr: Instruction): Graph {
        val graph = Graph(graphContentPane)
        val node = makeNodeFromInstr(graph, instr)
        graph.rootNode = node
        return graph
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

class LabelledContainer(labelText: String, node: Node) : VBox() {
    init {
        styleClass.add("labelled-container")
        children.add(Label(labelText).apply { styleClass.add("labelled-container-title") })
        children.add(node)
    }
}

class Graph(val pane: Pane) {
    val nodes = ArrayList<KNode>()
    val links = ArrayList<KNodeLink>()
    var rootNode: KNode? = null

    fun updateNodes() {
        val n = rootNode
        if (n != null) {
            n.computePosition(0.0, 0.0)
            links.forEach { link ->
                val (x1, y1) = link.upValue.downPos()
                val (x2, y2) = link.downValue.upPos()
                val l = link.makeLine(x1, y1, x2, y2)
                pane.children.add(l)
                l.viewOrder = 10.0
            }
        }
    }
}

class CreateGraphException(message: String) : Exception(message)

data class BoundsDimensions(val width: Double, val height: Double)
data class Coord(val x: Double, val y: Double)

private const val NODE_SPACING_HORIZ = 50.0
private const val NODE_SPACING_VERT = 50.0

abstract class KNode(val graph: Graph) {
    abstract fun bounds(): BoundsDimensions
    abstract fun computePosition(x: Double, y: Double)
    abstract fun downPos(): Coord
    abstract fun upPos(): Coord
}

abstract class KNode1Arg(graph: Graph) : KNode(graph)
abstract class KNode2Arg(graph: Graph) : KNode(graph)

abstract class KNodeLink(val upValue: KNode, val downValue: KNode) {
    abstract fun makeLine(x1: Double, y1: Double, x2: Double, y2: Double): Node
}

class LeftArgLink(upValue: KNode, downValue: KNode) : KNodeLink(upValue, downValue) {
    override fun makeLine(x1: Double, y1: Double, x2: Double, y2: Double): Node {
        return Line(x1, y1, x2, y2).apply {
            styleClass.add("left-arg-line")
        }
    }
}

class RightArgLink(upValue: KNode, downValue: KNode) : KNodeLink(upValue, downValue) {
    override fun makeLine(x1: Double, y1: Double, x2: Double, y2: Double): Node {
        // Make sure line is top-to-bottom
        assert(y1 <= y2)

        val line = Line(x1, y1, x2, y2).apply {
            styleClass.add("right-arg-line")
        }
        val label = Text("Right").apply {
            font = Font.font(8.0)

            val a = atan2(y2 - y1, x2 - x1)
            rotate = (if (x2 < x1) (a + PI).rem(PI * 2) else a) * (360.0 / (PI * 2))
            relocate(
                min(x1, x2) + abs((x2 - x1) / 2),
                min(y1, y2) + abs((y2 - y1) / 2))
        }
//        val l2 = Text("Ref").apply {
//            relocate(
//                min(x1, x2) + abs((x2 - x1) / 2),
//                min(y1, y2) + abs((y2 - y1) / 2))
//        }
        return Group(line, label)
    }
}

fun makeNodeFromInstr(graph: Graph, instr: Instruction): KNode {
    val node = when (instr) {
        is LiteralInteger -> LiteralIntegerGraphNode(graph, instr)
        is VariableRef -> VarRefGraphNode(graph, instr)
        is FunctionCall2Arg -> makeFunctionCall2ArgGraphNodeFromInstruction(graph, instr.fn, instr.leftArgs, instr.rightArgs)
        else -> throw CreateGraphException("Unsupported instruction type: ${instr}")
    }
    graph.nodes.add(node)
    return node
}

abstract class SimpleGraphNode(graph: Graph, val label: Region) : KNode(graph) {
    init {
        graph.pane.children.add(label)
    }

    override fun bounds() = BoundsDimensions(label.width, label.height)

    override fun computePosition(x: Double, y: Double) {
        label.layoutX = x
        label.layoutY = y
    }

    override fun downPos(): Coord {
        return Coord(label.layoutX + label.width / 2, label.layoutY + label.height)
    }

    override fun upPos(): Coord {
        return Coord(label.layoutX + label.width / 2, label.layoutY)
    }
}

class LiteralIntegerGraphNode(graph: Graph, instr: LiteralInteger) :
    SimpleGraphNode(graph, LabelledContainer("Integer", Label(instr.value.toString())))

class VarRefGraphNode(graph: Graph, instr: VariableRef) :
    SimpleGraphNode(graph, LabelledContainer("Variable", Label(instr.name.nameWithNamespace())))

private fun fnName(fn: APLFunction): String {
    val pos = fn.pos
    return when {
        pos.callerName != null -> pos.callerName!!
        pos.name != null -> pos.name!!
        else -> fn::class.simpleName ?: "unnamed"
    }
}

fun makeFunctionCall2ArgGraphNodeFromInstruction(graph: Graph, fn: APLFunction, leftArgs: Instruction, rightArgs: Instruction): KNode {
    val leftArgsNode = makeNodeFromInstr(graph, leftArgs)
    val rightArgsNode = makeNodeFromInstr(graph, rightArgs)
    val fnNode = makeFunctionCall2ArgGraphNodeFromFunction(fn, graph, leftArgsNode, rightArgsNode)
    return ContainerGraphNode(graph, fnNode, leftArgsNode, rightArgsNode)
}

private fun makeFunctionCall2ArgGraphNodeFromFunction(
    fn: APLFunction,
    graph: Graph,
    leftArgsNode: KNode,
    rightArgsNode: KNode
): KNode {
    val fnNode = when (fn) {
        is FunctionCallChain.Chain3 -> Chain3GraphNode(graph, fn, leftArgsNode, rightArgsNode)
        else -> FunctionCall2ArgGraphNode(graph, fn, leftArgsNode, rightArgsNode)
    }
    return fnNode
}

class ContainerGraphNode(
    graph: Graph, val fnNode: KNode, vararg val subNodes: KNode
) : KNode(graph) {

    init {
        assert(subNodes.isNotEmpty())
    }

    override fun bounds(): BoundsDimensions {
        val fnNodeBounds = fnNode.bounds()
        val subNodesBounds = subNodes.map(KNode::bounds)
        val width = max(
            fnNodeBounds.width,
            subNodesBounds.map(BoundsDimensions::width).sum() + NODE_SPACING_HORIZ * (max(0, subNodesBounds.size - 1)))
        val height = fnNodeBounds.height + NODE_SPACING_VERT + subNodesBounds.maxValueBy(BoundsDimensions::height)
        return BoundsDimensions(width, height)
    }

    override fun computePosition(x: Double, y: Double) {
        val b = bounds()
        val fnBounds = fnNode.bounds()
        fnNode.computePosition(x + (b.width - fnBounds.width) / 2, y)
        val subNodesY = y + fnBounds.height + NODE_SPACING_VERT
        val widths = subNodes.map { n -> n.bounds().width }
        val subNodesWidth = widths.sum() + NODE_SPACING_HORIZ * max(0, widths.size - 1)
        var nodeXPos = (b.width - subNodesWidth) / 2
        subNodes.forEachIndexed { i, n ->
            n.computePosition(x + nodeXPos, subNodesY)
            nodeXPos += widths[i] + NODE_SPACING_HORIZ
        }
    }

    override fun downPos(): Coord {
        TODO("not implemented")
    }

    override fun upPos(): Coord {
        return fnNode.upPos()
    }
}

class FunctionCall2ArgGraphNode(
    graph: Graph, fn: APLFunction, leftArgLink: KNode, rightArgLink: KNode
) : KNode2Arg(graph) {

    private val label = LabelledContainer("Call2", Label(fnName(fn)))

    init {
        graph.pane.children.add(label)
        graph.links.add(LeftArgLink(this, leftArgLink))
        graph.links.add(RightArgLink(this, rightArgLink))
    }

    override fun bounds() = BoundsDimensions(label.width, label.height)

    override fun computePosition(x: Double, y: Double) {
        label.layoutX = x
        label.layoutY = y
    }

    override fun downPos(): Coord {
        return Coord(label.layoutX + label.width / 2, label.layoutY + label.height)
    }

    override fun upPos(): Coord {
        return Coord(label.layoutX + label.width / 2, label.layoutY)
    }
}

class Chain3GraphNode(graph: Graph, fn: FunctionCallChain.Chain3, leftArgLink: KNode, rightArgLink: KNode) : KNode(graph) {
    val leftNode = makeFunctionCall2ArgGraphNodeFromFunction(fn.fn0, graph, leftArgLink, rightArgLink)
    val rightNode = makeFunctionCall2ArgGraphNodeFromFunction(fn.fn2, graph, leftArgLink, rightArgLink)
    val middleNode = makeFunctionCall2ArgGraphNodeFromFunction(fn.fn1, graph, leftNode, rightNode)

    override fun bounds(): BoundsDimensions {
        val mBounds = middleNode.bounds()
        val lBounds = leftNode.bounds()
        val rBounds = rightNode.bounds()
        val width = max(mBounds.width, lBounds.width + rBounds.width + NODE_SPACING_HORIZ)
        val height = mBounds.height + NODE_SPACING_VERT + max(lBounds.height, rBounds.height)
        return BoundsDimensions(width, height)
    }

    override fun computePosition(x: Double, y: Double) {
        val b = bounds()
        val mBounds = middleNode.bounds()
        val lBounds = leftNode.bounds()
        val rBounds = rightNode.bounds()
        val bottomWidth = lBounds.width + rBounds.width + NODE_SPACING_HORIZ
        middleNode.computePosition(x + (b.width - mBounds.width) / 2, y)
        val yOffset = y + mBounds.height + NODE_SPACING_VERT
        val lx = x + (b.width - bottomWidth) / 2
        leftNode.computePosition(lx, yOffset)
        rightNode.computePosition(lx + lBounds.width + NODE_SPACING_HORIZ, yOffset)
    }

    override fun downPos(): Coord {
        TODO("not implemented")
    }

    override fun upPos() = middleNode.upPos()
}
