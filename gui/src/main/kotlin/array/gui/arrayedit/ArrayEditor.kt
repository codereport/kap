package array.gui.arrayedit

import array.*
import array.gui.Client
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.input.Clipboard
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.stage.Stage
import org.jsoup.Jsoup
import kotlin.math.max

class ArrayEditor {
    private lateinit var stage: Stage
    lateinit var client: Client
    lateinit var table: TableView<ArrayEditorRow>
    lateinit var variableField: TextField
    lateinit var axisGrid: GridPane
    lateinit var axisEditPanel: HBox

    private val axisInputFields = ArrayList<Spinner<Int>>()

    fun show() {
        stage.show()
    }

    fun loadFromVariable(@Suppress("UNUSED_PARAMETER") event: ActionEvent) {
        loadFromField()
    }

    @Suppress("UNUSED_PARAMETER")
    fun getClicked(event: ActionEvent) {
        loadFromField()
    }

    @Suppress("UNUSED_PARAMETER")
    fun putClicked(event: ActionEvent) {
        saveFromField()
    }

    private fun loadFromField() {
        println("Variable name: ${variableField.text}")
        val name = variableField.text.trim()
        client.calculationQueue.pushReadVariableRequest(name) { result ->
            if (result != null) {
                val v = result.collapse()
                Platform.runLater {
                    loadArray(v)
                }
            }
        }
    }

    private fun saveFromField() {
        println("Variable name: ${variableField.text}")
        val name = variableField.text.trim()
        client.calculationQueue.pushWriteVariableRequest(name, makeArrayContent()) { result ->
            result?.printStackTrace()
        }
    }

    fun loadArray(value: APLValue) {
        val d = value.dimensions
        if (d.size == 0) {
            throw IllegalArgumentException("Scalars cannot be loaded")
        }

        axisGrid.children.clear()
        axisInputFields.clear()

        var col = 0

        fun addCol(node: Node, styleClassName: String) {
            Label(d[col].toString()).let { label ->
                label.styleClass.addAll("axisGrid-entry", "axisGrid-header")
                GridPane.setConstraints(label, col, 0)
                axisGrid.children.add(label)
            }
            node.styleClass.addAll("axisGrid-entry", styleClassName)
            GridPane.setConstraints(node, col, 1)
            axisGrid.children.add(node)
            col++
        }


        if (d.size > 2) {
            repeat(d.size - 2) { i ->
                val field = Spinner(SpinnerValueFactory.IntegerSpinnerValueFactory(0, d[i], 0))
                field.prefWidth = 70.0
                field.valueProperty().addListener { _, _, _ -> fillInItemsFromAxisFields(value) }
                addCol(field, "axisGrid-edit")
                axisInputFields.add(field)
            }
        }

        if (d.size > 1) {
            addCol(Label(d[d.size - 2].toString()), "axisGrid-axisLabel")
        }
        addCol(Label(d[d.size - 1].toString()), "axisGrid-axisLabel")

        val colList = if (d.size < 2) {
            listOf(TableColumn<ArrayEditorRow, ArrayEditorCell>().apply {
                cellValueFactory = ArrayEditorCellValueFactory(0)
                cellFactory = ArrayEditorCellFactory()
                text = "0"
            })
        } else {
            val columnLabels = value.labels?.labels?.get(1)
            (0 until d.lastDimension()).map { index ->
                TableColumn<ArrayEditorRow, ArrayEditorCell>().apply {
                    cellValueFactory = ArrayEditorCellValueFactory(index)
                    cellFactory = ArrayEditorCellFactory()
                    text = columnLabels?.get(index)?.title ?: index.toString()
                }
            }
        }
        table.columns.setAll(colList)

        table.items = FXCollections.observableArrayList()
        fillInItemsFromAxisFields(value)
    }

    private fun fillInItemsFromAxisFields(value: APLValue) {
        val d = value.dimensions
        assert(axisInputFields.size == max(d.size - 2, 0))
        val position = IntArray(d.size) { i ->
            if (i < d.size - 2) {
                axisInputFields[i].value
            } else {
                0
            }
        }
        fillInItems(value, position)
    }

    private fun fillInItems(value: APLValue, position: IntArray) {
        val d = value.dimensions
        assert(d.size == position.size)
        assert(position[position.size - 1] == 0)
        if (d.size > 1) {
            assert(position[position.size - 2] == 0)
        }
        val baseIndex = if (d.contentSize() == 0) 0 else d.indexFromPosition(position)
        val (numRows, numCols) = if (d.size == 1) Pair(d[0], 1) else Pair(d[d.size - 2], d[d.size - 1])
        val rows = Array(numRows) { i -> ArrayEditorRow(value, baseIndex, i, numCols) }
        table.items.setAll(*rows)
    }

    private fun makeArrayContent(): APLValue {
        val items = table.items
        val numRows = items.size
        val numCols = table.columns.size
        val content = ArrayList<APLValue>()
        items.forEach { row ->
            content.addAll(row.values)
        }
        assert(items.size == numRows * numCols)
        return APLArrayList(dimensionsOfSize(numRows, numCols), content)
    }

    private fun pasteToTable() {
        val clipboard = Clipboard.getSystemClipboard()
        when {
            clipboard.hasHtml() -> {
                pasteHtml(clipboard.html)
            }
        }
    }

    private fun pasteHtml(html: String) {
        val doc = Jsoup.parse(html)
        val result = htmlTableToArray(doc)
        if (result != null) {
            loadArray(result)
        }
    }

    companion object {
        private fun makeArrayEditor(client: Client): ArrayEditor {
            val loader = FXMLLoader(ArrayEditor::class.java.getResource("arrayeditor.fxml"))
            val root = loader.load<Parent>()
            val controller = loader.getController<ArrayEditor>()

            controller.client = client
            controller.stage = Stage()
            val scene = Scene(root, 800.0, 300.0)
            controller.stage.title = "Array Editor"
            controller.stage.scene = scene

            controller.table.contextMenu = ContextMenu(MenuItem("Paste").apply { onAction = EventHandler { controller.pasteToTable() } })

            controller.loadArray(makeDefaultContent())

            return controller
        }

        private fun makeDefaultContent(): APLArray {
            return APLArrayImpl(dimensionsOfSize(0, 5), Array(0) { 0.makeAPLNumber() })
        }

        fun open(client: Client, value: APLValue? = null): ArrayEditor {
            return makeArrayEditor(client).apply {
                if (value != null) {
                    loadArray(value)
                }
                show()
            }
        }
    }
}
