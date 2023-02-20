package array.gui.viewer

import array.*
import array.builtins.ComposeFunctionDescriptor
import array.builtins.OverDerivedFunctionDescriptor
import array.builtins.ReverseComposeFunctionDescriptor
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.shape.Line
import kotlin.math.max

class LabelledContainer(labelText: String, node: Node) : VBox() {
    init {
        styleClass.add("labelled-container")
        children.add(Label(labelText).apply { styleClass.add("labelled-container-title") })
        children.add(node)
    }
}

class Graph(val structureViewer: StructureViewer, val pane: Pane) {
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

    fun registerClickListener(component: Region, pos: Position?) {
        component.addEventHandler(MouseEvent.MOUSE_CLICKED) { event ->
            if (pos != null && event.button == MouseButton.PRIMARY) {
                structureViewer.highlightPosition(pos)
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

    open fun downPos(): Coord {
        throw IllegalStateException("No down position for this node type")
    }

    abstract fun upPos(): Coord
}

abstract class KNode1Arg(graph: Graph) : KNode(graph)
abstract class KNode2Arg(graph: Graph) : KNode(graph)

abstract class KNodeLink(val upValue: KNode, val downValue: KNode) {
    abstract fun makeLine(x1: Double, y1: Double, x2: Double, y2: Double): Node
}

open class KNodeLinkLine(upValue: KNode, downValue: KNode, val label: String, val style: String) : KNodeLink(upValue, downValue) {
    override fun makeLine(x1: Double, y1: Double, x2: Double, y2: Double): Node {
        return makeLabelLine(x1, y1, x2, y2, label).apply {
            styleClass.add(style)
        }
    }
}

class LiteralValueArgLink(upValue: KNode, downValue: KNode) : KNodeLinkLine(upValue, downValue, "Literal", "literal-value-link")
class LeftArgLink(upValue: KNode, downValue: KNode) : KNodeLinkLine(upValue, downValue, "Left", "left-arg-line")
class RightArgLink(upValue: KNode, downValue: KNode) : KNodeLinkLine(upValue, downValue, "Right", "right-arg-line")

private fun makeLabelLine(x1: Double, y1: Double, x2: Double, y2: Double, label: String): Node {
    // Make sure line is top-to-bottom
    assertx(y1 <= y2)

    val line = Line(x1, y1, x2, y2).apply {
        styleClass.add("node-line")
    }
//    val m1: Node
//    val m2: Node
//    val label = Text(label).apply {
//        styleClass.add("node-line-label")
//
//        val a = atan2(y2 - y1, x2 - x1)
//        transforms.add(Rotate((if (x2 < x1) (a + PI).rem(PI * 2) else a) * (360.0 / (PI * 2)), 0.0,0.0))
//        relocate(
//            min(x1, x2) + abs((x2 - x1) / 2),
//            min(y1, y2) + abs((y2 - y1) / 2))
////        m1 = Line(
////            min(x1, x2) + abs((x2 - x1) / 2) - 5,
////            min(y1, y2) + abs((y2 - y1) / 2) - 5,
////            min(x1, x2) + abs((x2 - x1) / 2) + 5,
////            min(y1, y2) + abs((y2 - y1) / 2) + 5)
////        m2 = Line(
////            min(x1, x2) + abs((x2 - x1) / 2) + 5,
////            min(y1, y2) + abs((y2 - y1) / 2) - 5,
////            min(x1, x2) + abs((x2 - x1) / 2) - 5,
////            min(y1, y2) + abs((y2 - y1) / 2) + 5)
//    }
    return Group(line)
}

fun makeNodeFromInstr(graph: Graph, instr: Instruction): KNode {
    val node = when (instr) {
        is LiteralInteger -> LiteralIntegerGraphNode(graph, instr.value, instr.pos)
        is LiteralDouble -> LiteralDoubleGraphNode(graph, instr.value, instr.pos)
        is VariableRef -> VarRefGraphNode(graph, instr)
        is Literal1DArray -> LiteralArrayGraphNode.create(graph, instr.values)
        is LiteralLongArray -> LiteralArrayGraphNode.createFromLongArray(graph, instr)
        is LiteralDoubleArray -> LiteralArrayGraphNode.createFromDoubleArray(graph, instr)
        is LiteralCharacter -> LiteralCharacterArrayNode(graph, instr.valueInt.value, instr.pos)
        is LiteralStringValue -> LiteralStringGraphNode(graph, instr.s, instr.pos)
        is FunctionCall1Arg -> makeFunctionCall1ArgGraphNodeFromInstruction(graph, instr)
        is FunctionCall2Arg -> makeFunctionCall2ArgGraphNodeFromInstruction(graph, instr)
        else -> throw CreateGraphException("Unsupported instruction type: ${instr}")
    }
    graph.nodes.add(node)
    return node
}

open class SimpleGraphNode(graph: Graph, val label: Region, val pos: Position?) : KNode(graph) {
    init {
        graph.pane.children.add(label)
        graph.registerClickListener(label, pos)
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

class LiteralIntegerGraphNode(graph: Graph, value: Long, pos: Position?) :
        SimpleGraphNode(graph, LabelledContainer("Integer", Label(value.toString())), pos)

class LiteralDoubleGraphNode(graph: Graph, value: Double, pos: Position?) :
        SimpleGraphNode(graph, LabelledContainer("Double", Label(value.toString())), pos)

class VarRefGraphNode(graph: Graph, instr: VariableRef) :
        SimpleGraphNode(graph, LabelledContainer("Variable", Label(instr.name.nameWithNamespace)), instr.pos)

fun makeCharacterLabel(value: Int): Node {
    val vbox = VBox()
    vbox.children.add(Label(String.format("Codepoint: U+%04X", value)))
    vbox.children.add(Label("Name: ${Character.getName(value) ?: "invalid"}"))
    return vbox
}

class LiteralCharacterArrayNode(graph: Graph, value: Int, pos: Position?) :
        SimpleGraphNode(graph, LabelledContainer("Character", makeCharacterLabel(value)), pos)

class LiteralStringGraphNode(graph: Graph, value: String, pos: Position?) :
        SimpleGraphNode(graph, LabelledContainer("String", Label("'${value}'").apply { style = "monospaced-label" }), pos)

class LiteralArrayGraphNode private constructor(graph: Graph, valueList: List<KNode>, pos: Position?) : KNode(graph) {
    val label = LabelledContainer("Literal array", Label("Size: ${valueList.size}"))
    val topNode = SimpleGraphNode(graph, label, null)
    val subnodes = valueList.toTypedArray()
    val container = ContainerGraphNode(graph, topNode, *subnodes)

    init {
        subnodes.forEach { node ->
            graph.links.add(LiteralValueArgLink(topNode, node))
        }
        graph.registerClickListener(label, pos)
    }

    override fun bounds() = container.bounds()
    override fun computePosition(x: Double, y: Double) = container.computePosition(x, y)
    override fun upPos() = container.upPos()

    companion object {
        fun create(graph: Graph, instrList: List<Instruction>): LiteralArrayGraphNode {
            val valueList = instrList.map { instr -> makeNodeFromInstr(graph, instr) }
            val newPos = when (instrList.size) {
                0 -> throw java.lang.IllegalStateException("Explicit array with zero elements")
                1 -> instrList.first().pos
                else -> instrList.first().pos.expandToEnd(instrList.last().pos)
            }
            return LiteralArrayGraphNode(graph, valueList, newPos)
        }

        fun createFromLongArray(graph: Graph, instr: LiteralLongArray): KNode {
            val valueList = instr.value.map { v -> LiteralIntegerGraphNode(graph, v, null) }
            return LiteralArrayGraphNode(graph, valueList, instr.pos)
        }

        fun createFromDoubleArray(graph: Graph, instr: LiteralDoubleArray): KNode {
            val valueList = instr.value.map { v -> LiteralDoubleGraphNode(graph, v, null) }
            return LiteralArrayGraphNode(graph, valueList, instr.pos)
        }
    }
}

private fun formatFnName(callerName: String?, name: String?): String {
    return when {
        callerName != null && name != null -> "${callerName} [${name}]"
        callerName != null -> callerName
        name != null -> name
        else -> "unnamed"
    }
}

private fun fnName1(fn: APLFunction) = formatFnName(fn.pos.callerName, fn.name1Arg)
private fun fnName2(fn: APLFunction) = formatFnName(fn.pos.callerName, fn.name2Arg)

private fun makeFunctionCall1ArgGraphNodeFromInstruction(graph: Graph, instr: FunctionCall1Arg): KNode {
    val rightArgsNode = makeNodeFromInstr(graph, instr.rightArgs)
    val fnNode = makeFunctionCall1ArgGraphNodeFromFunction(instr.fn, graph, rightArgsNode)
    return ContainerGraphNode(graph, fnNode, rightArgsNode)
}

private fun makeFunctionCall1ArgGraphNodeFromFunction(fn: APLFunction, graph: Graph, rightArgsNode: KNode): KNode {
    return when (fn) {
        is FunctionCallChain.Chain2 -> Chain2A1GraphNode(graph, fn, rightArgsNode)
        is FunctionCallChain.Chain3 -> Chain3A1GraphNode(graph, fn, rightArgsNode)
        is ComposeFunctionDescriptor.ComposeFunctionImpl -> ComposeA1GraphNode(graph, fn, rightArgsNode)
        is ReverseComposeFunctionDescriptor.ReverseComposeFunctionImpl -> ReverseComposeA1GraphNode(graph, fn, rightArgsNode)
        is OverDerivedFunctionDescriptor.OverDerivedFunctionImpl -> OverA1GraphNode(graph, fn, rightArgsNode)
        else -> FunctionCall1ArgGraphNode(graph, fn, rightArgsNode)
    }
}

private fun makeFunctionCall2ArgGraphNodeFromInstruction(graph: Graph, instr: FunctionCall2Arg): KNode {
    val leftArgsNode = makeNodeFromInstr(graph, instr.leftArgs)
    val rightArgsNode = makeNodeFromInstr(graph, instr.rightArgs)
    val fnNode = makeFunctionCall2ArgGraphNodeFromFunction(instr.fn, graph, leftArgsNode, rightArgsNode)
    return ContainerGraphNode(graph, fnNode, leftArgsNode, rightArgsNode)
}

private fun makeFunctionCall2ArgGraphNodeFromFunction(
    fn: APLFunction,
    graph: Graph,
    leftArgsNode: KNode,
    rightArgsNode: KNode
): KNode {
    return when (fn) {
        is FunctionCallChain.Chain2 -> Chain2A2GraphNode(graph, fn, leftArgsNode, rightArgsNode)
        is FunctionCallChain.Chain3 -> Chain3A2GraphNode(graph, fn, leftArgsNode, rightArgsNode)
        is ComposeFunctionDescriptor.ComposeFunctionImpl -> ComposeA2GraphNode(graph, fn, leftArgsNode, rightArgsNode)
        is ReverseComposeFunctionDescriptor.ReverseComposeFunctionImpl -> ReverseComposeA2GraphNode(graph, fn, leftArgsNode, rightArgsNode)
        is OverDerivedFunctionDescriptor.OverDerivedFunctionImpl -> OverA2GraphNode(graph, fn, leftArgsNode, rightArgsNode)
        else -> FunctionCall2ArgGraphNode(graph, fn, leftArgsNode, rightArgsNode)
    }
}

class ContainerGraphNode(
    graph: Graph, val fnNode: KNode, vararg val subNodes: KNode
) : KNode(graph) {

    init {
        assertx(subNodes.isNotEmpty())
    }

    override fun bounds(): BoundsDimensions {
        val fnNodeBounds = fnNode.bounds()
        val subNodesBounds = subNodes.map(KNode::bounds)
        val width = max(
            fnNodeBounds.width,
            subNodesBounds.map(BoundsDimensions::width).sum() + NODE_SPACING_HORIZ * max(0, subNodesBounds.size - 1))
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

    override fun upPos(): Coord {
        return fnNode.upPos()
    }
}

class FunctionCall1ArgGraphNode(graph: Graph, fn: APLFunction, rightArgLink: KNode) : KNode1Arg(graph) {
    private val label = LabelledContainer("Call1", Label(fnName1(fn)))

    init {
        graph.pane.children.add(label)
        graph.links.add(RightArgLink(this, rightArgLink))
        graph.registerClickListener(label, fn.pos)
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

class FunctionCall2ArgGraphNode(graph: Graph, fn: APLFunction, leftArgLink: KNode, rightArgLink: KNode) : KNode2Arg(graph) {

    private val label = LabelledContainer("Call2", Label(fnName2(fn)))

    init {
        graph.pane.children.add(label)
        graph.links.add(LeftArgLink(this, leftArgLink))
        graph.links.add(RightArgLink(this, rightArgLink))
        graph.registerClickListener(label, fn.pos)
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

abstract class AbstractCallSeq2A1GraphNode(graph: Graph, fn0: APLFunction, fn1: APLFunction, rightArgLink: KNode) : KNode(graph) {
    val rightNode = makeFunctionCall1ArgGraphNodeFromFunction(fn1, graph, rightArgLink)
    val leftNode = makeFunctionCall1ArgGraphNodeFromFunction(fn0, graph, rightNode)

    override fun bounds(): BoundsDimensions {
        val lBounds = leftNode.bounds()
        val rBounds = rightNode.bounds()
        return BoundsDimensions(max(lBounds.width, rBounds.width), lBounds.height + NODE_SPACING_VERT + rBounds.height)
    }

    override fun computePosition(x: Double, y: Double) {
        val b = bounds()
        val lBounds = leftNode.bounds()
        val rBounds = rightNode.bounds()
        leftNode.computePosition(x + (b.width - lBounds.width) / 2, y)
        rightNode.computePosition(x + (b.width - rBounds.width) / 2, y + rBounds.height + NODE_SPACING_VERT)
    }

    override fun upPos() = leftNode.upPos()
}

class Chain2A1GraphNode(graph: Graph, fn: FunctionCallChain.Chain2, rightArgsNode: KNode) :
        AbstractCallSeq2A1GraphNode(graph, fn.fn0, fn.fn1, rightArgsNode)


class Chain2A2GraphNode(graph: Graph, fn: FunctionCallChain.Chain2, leftArgLink: KNode, rightArgLink: KNode) : KNode(graph) {
    val rightNode = makeFunctionCall2ArgGraphNodeFromFunction(fn.fn1, graph, leftArgLink, rightArgLink)
    val leftNode = makeFunctionCall1ArgGraphNodeFromFunction(fn.fn0, graph, rightNode)

    override fun bounds(): BoundsDimensions {
        val lBounds = leftNode.bounds()
        val rBounds = rightNode.bounds()
        return BoundsDimensions(max(lBounds.width, rBounds.width), lBounds.height + NODE_SPACING_VERT + rBounds.height)
    }

    override fun computePosition(x: Double, y: Double) {
        val b = bounds()
        val lBounds = leftNode.bounds()
        val rBounds = rightNode.bounds()
        leftNode.computePosition(x + (b.width - lBounds.width) / 2, y)
        rightNode.computePosition(x + (b.width - rBounds.width) / 2, y + lBounds.height + NODE_SPACING_VERT)
    }

    override fun upPos() = leftNode.upPos()
}

class Chain3A1GraphNode(graph: Graph, fn: FunctionCallChain.Chain3, rightArgLink: KNode) : KNode(graph) {
    val leftNode = makeFunctionCall1ArgGraphNodeFromFunction(fn.fn0, graph, rightArgLink)
    val rightNode = makeFunctionCall1ArgGraphNodeFromFunction(fn.fn2, graph, rightArgLink)
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

    override fun upPos() = middleNode.upPos()
}

class Chain3A2GraphNode(graph: Graph, fn: FunctionCallChain.Chain3, leftArgLink: KNode, rightArgLink: KNode) : KNode(graph) {
    val leftNode = makeFunctionCall2ArgGraphNodeFromFunction(fn.fn0, graph, leftArgLink, rightArgLink)
    val rightNode = makeFunctionCall2ArgGraphNodeFromFunction(fn.fn2, graph, leftArgLink, rightArgLink)
    val middleNode = makeFunctionCall2ArgGraphNodeFromFunction(fn.fn1, graph, leftNode, rightNode)
    val container = ContainerGraphNode(graph, middleNode, leftNode, rightNode)

    override fun bounds() = container.bounds()
    override fun computePosition(x: Double, y: Double) = container.computePosition(x, y)
    override fun upPos() = container.upPos()
}

class ComposeA1GraphNode(graph: Graph, fn: ComposeFunctionDescriptor.ComposeFunctionImpl, rightArgsNode: KNode) : KNode(graph) {
    val rightNode = makeFunctionCall1ArgGraphNodeFromFunction(fn.fn1, graph, rightArgsNode)
    val middleNode = makeFunctionCall2ArgGraphNodeFromFunction(fn.fn0, graph, rightArgsNode, rightNode)

    override fun bounds(): BoundsDimensions {
        val rb = rightNode.bounds()
        val mb = middleNode.bounds()
        return BoundsDimensions(rb.width + NODE_SPACING_HORIZ + rb.width, mb.height + NODE_SPACING_VERT + rb.height)
    }

    override fun computePosition(x: Double, y: Double) {
        val mb = middleNode.bounds()
        val rb = rightNode.bounds()
        rightNode.computePosition(x + rb.width + NODE_SPACING_HORIZ, y + mb.height + NODE_SPACING_VERT)
        middleNode.computePosition(x, y)
    }

    override fun upPos(): Coord {
        return middleNode.upPos()
    }
}

class ReverseComposeA1GraphNode(graph: Graph, fn: ReverseComposeFunctionDescriptor.ReverseComposeFunctionImpl, rightArgsNode: KNode) :
        KNode(graph) {
    val leftNode = makeFunctionCall1ArgGraphNodeFromFunction(fn.fn0, graph, rightArgsNode)
    val middleNode = makeFunctionCall2ArgGraphNodeFromFunction(fn.fn1, graph, leftNode, rightArgsNode)

    override fun bounds(): BoundsDimensions {
        val lb = leftNode.bounds()
        val mb = middleNode.bounds()
        return BoundsDimensions(lb.width + NODE_SPACING_HORIZ + mb.width, mb.height + NODE_SPACING_VERT + lb.height)
    }

    override fun computePosition(x: Double, y: Double) {
        val mb = middleNode.bounds()
        val lb = leftNode.bounds()
        leftNode.computePosition(x, y + mb.height + NODE_SPACING_VERT)
        middleNode.computePosition(x + lb.width + NODE_SPACING_HORIZ, y)
    }

    override fun upPos(): Coord {
        return middleNode.upPos()
    }
}

class ComposeA2GraphNode(graph: Graph, fn: ComposeFunctionDescriptor.ComposeFunctionImpl, leftNode: KNode, rightArgLink: KNode) :
        KNode(graph) {
    val rightNode = makeFunctionCall1ArgGraphNodeFromFunction(fn.fn1, graph, rightArgLink)
    val middleNode = makeFunctionCall2ArgGraphNodeFromFunction(fn.fn0, graph, leftNode, rightNode)
    val container = ContainerGraphNode(graph, middleNode, leftNode, rightNode)

    override fun bounds() = container.bounds()
    override fun computePosition(x: Double, y: Double) = container.computePosition(x, y)
    override fun upPos() = container.upPos()
}

class ReverseComposeA2GraphNode(
    graph: Graph,
    fn: ReverseComposeFunctionDescriptor.ReverseComposeFunctionImpl,
    leftArgLink: KNode,
    rightNode: KNode
) : KNode(graph) {
    val leftNode = makeFunctionCall1ArgGraphNodeFromFunction(fn.fn0, graph, leftArgLink)
    val middleNode = makeFunctionCall2ArgGraphNodeFromFunction(fn.fn1, graph, leftNode, rightNode)
    val container = ContainerGraphNode(graph, middleNode, leftNode, rightNode)

    override fun bounds() = container.bounds()
    override fun computePosition(x: Double, y: Double) = container.computePosition(x, y)
    override fun upPos() = container.upPos()
}

class OverA1GraphNode(graph: Graph, fn: OverDerivedFunctionDescriptor.OverDerivedFunctionImpl, rightArgsNode: KNode) :
        AbstractCallSeq2A1GraphNode(graph, fn.fn0, fn.fn1, rightArgsNode)

class OverA2GraphNode(graph: Graph, fn: OverDerivedFunctionDescriptor.OverDerivedFunctionImpl, leftArgsNode: KNode, rightArgsNode: KNode) :
        KNode(graph) {
    val leftNode = makeFunctionCall1ArgGraphNodeFromFunction(fn.fn1, graph, leftArgsNode)
    val rightNode = makeFunctionCall1ArgGraphNodeFromFunction(fn.fn1, graph, rightArgsNode)
    val middleNode = makeFunctionCall2ArgGraphNodeFromFunction(fn.fn0, graph, leftNode, rightNode)
    val container = ContainerGraphNode(graph, middleNode, leftNode, rightNode)

    override fun bounds() = container.bounds()
    override fun computePosition(x: Double, y: Double) = container.computePosition(x, y)
    override fun upPos() = container.upPos()
}
