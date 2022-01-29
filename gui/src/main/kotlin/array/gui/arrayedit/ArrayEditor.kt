package array.gui.arrayedit

import array.*
import array.gui.Client
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
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
import org.controlsfx.control.spreadsheet.Grid
import org.controlsfx.control.spreadsheet.GridBase
import org.controlsfx.control.spreadsheet.SpreadsheetCell
import org.controlsfx.control.spreadsheet.SpreadsheetView
import org.jsoup.Jsoup
import kotlin.math.max

class ArrayEditor {
    private lateinit var stage: Stage
    lateinit var client: Client
    lateinit var table: SpreadsheetView
    lateinit var variableField: TextField
    lateinit var axisGrid: GridPane
    lateinit var axisEditPanel: HBox

    private val axisInputFields = ArrayList<Spinner<Int>>()
    private var content: MutableAPLValue = MutableAPLValue(APLArrayImpl(dimensionsOfSize(0, 2), emptyArray()))

    fun show() {
        stage.show()
    }

    fun loadFromVariable(@Suppress("UNUSED_PARAMETER") event: ActionEvent) {
        loadFromField()
    }

    fun getClicked(@Suppress("UNUSED_PARAMETER") event: ActionEvent) {
        loadFromField()
    }

    fun putClicked(@Suppress("UNUSED_PARAMETER") event: ActionEvent) {
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
        unless(TokenGenerator.isValidSymbolName(name)) {
            displayError("Not a valid symbol name: ${name}")
            return
        }
        client.calculationQueue.pushWriteVariableRequest(name, content.makeAPLArray()) { result ->
            if (result != null) {
                displayError("Error writing result to variable", result.message)
            }
        }
    }

    private fun displayError(title: String, details: String? = null) {
        val dialog = Alert(Alert.AlertType.ERROR, title, ButtonType.CLOSE)
        dialog.initOwner(stage)
        if (details != null) {
            dialog.dialogPane.contentText = details
        }
        dialog.showAndWait()
    }

    fun loadArray(value: APLValue) {
        val d = value.dimensions
        if (d.size == 0) {
            throw IllegalArgumentException("Scalars cannot be loaded")
        }

        content = MutableAPLValue(value)

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
                val field = Spinner(SpinnerValueFactory.IntegerSpinnerValueFactory(0, max(d[i] - 1, 0), 0))
                field.prefWidth = 70.0
                field.valueProperty().addListener { _, _, _ -> fillInItemsFromAxisFields() }
                addCol(field, "axisGrid-edit")
                axisInputFields.add(field)
            }
        }

        if (d.size > 1) {
            addCol(Label(d[d.size - 2].toString()), "axisGrid-axisLabel")
        }
        addCol(Label(d[d.size - 1].toString()), "axisGrid-axisLabel")

        fillInItemsFromAxisFields()
    }

    private fun fillInItemsFromAxisFields() {
        val d = content.dimensions
        assert(axisInputFields.size == max(d.size - 2, 0))
        val position = IntArray(d.size) { i ->
            if (i < d.size - 2) {
                axisInputFields[i].value
            } else {
                0
            }
        }
        table.grid = makeGridBase(position)
    }

    private fun makeGridBase(position: IntArray): Grid {
        val d = content.dimensions
        assert(d.size == position.size)
        assert(position[position.size - 1] == 0)
        if (d.size > 1) {
            assert(position[position.size - 2] == 0)
        }
        val baseIndex = if (d.contentSize() == 0) 0 else d.indexFromPosition(position)
        val (numRows, numCols) = if (d.size == 1) Pair(d[0], 1) else Pair(d[d.size - 2], d[d.size - 1])
        val gridBase = GridBase(numRows, numCols)
        val rows = FXCollections.observableArrayList<ObservableList<SpreadsheetCell>>()
        repeat(numRows) { rowIndex ->
            val row = FXCollections.observableArrayList<SpreadsheetCell>()
            repeat(numCols) { colIndex ->
                row.add(
                    KapValueSpreadsheetCellType.createCell(
                        content,
                        rowIndex,
                        colIndex,
                        baseIndex + (rowIndex * numCols) + colIndex))
            }
            rows.add(row)
        }
        gridBase.setRows(rows)

        val colHeaders = if (d.size == 1) {
            listOf("0")
        } else {
            val columnLabels = content.labels?.labels?.get(d.size - 1)
            (0 until numCols).mapIndexed { i, v ->
                columnLabels?.get(i)?.title ?: i.toString()
            }
        }
        gridBase.columnHeaders.setAll(colHeaders)

        val rowLabels = content.labels?.labels?.get(if (d.size == 1) 0 else d.size - 2)
        val rowHeaders = (0 until numRows).mapIndexed { i, v ->
            rowLabels?.get(i)?.title ?: i.toString()
        }
        gridBase.rowHeaders.setAll(rowHeaders)

        return gridBase
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
