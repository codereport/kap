package array.gui.viewer

import array.Instruction
import array.Position
import array.StringSourceLocation
import array.gui.Client
import array.gui.styledarea.InputFieldStyledArea
import array.gui.styledarea.TextStyle
import javafx.event.ActionEvent
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCombination
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.stage.Stage
import org.fxmisc.wellbehaved.event.EventPattern
import org.fxmisc.wellbehaved.event.InputMap
import org.fxmisc.wellbehaved.event.Nodes

class StructureViewer {
    lateinit var graphContentPane: Pane
    lateinit var client: Client
    lateinit var expressionField: InputFieldStyledArea
    lateinit var borderPane: BorderPane

    fun initialize() {
        val returnMapping =
            InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER, KeyCombination.CONTROL_DOWN)) { displayExpressionFromInput() }
        Nodes.addInputMap(expressionField, returnMapping)
    }

    fun showClicked(@Suppress("UNUSED_PARAMETER") actionEvent: ActionEvent) {
        displayExpressionFromInput()
    }

    fun displayExpressionFromInput() {
        parseExpression(expressionField.text)
    }

    private fun parseExpression(text: String) {
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
        val graph = Graph(this, graphContentPane)
        val node = makeNodeFromInstr(graph, instr)
        graph.rootNode = node
        return graph
    }

    fun highlightPosition(pos: Position) {
        expressionField.clearStyles()
        expressionField.setStyleForRange(pos.line, pos.col, pos.computedEndLine, pos.computedEndCol, TextStyle(TextStyle.Type.SINGLE_CHAR_HIGHLIGHT))
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
