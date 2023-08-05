package array.gui.arrayedit

import array.*
import array.csv.CsvParseException
import array.csv.readCsv
import array.gui.Client
import array.gui.displayErrorWithStage
import array.msofficereader.readExcelFile
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
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import org.controlsfx.control.spreadsheet.SpreadsheetCell
import org.jsoup.Jsoup
import java.io.File
import java.io.IOException
import kotlin.math.max

class ArrayEditor {
    private lateinit var stage: Stage
    lateinit var client: Client
    lateinit var table: ArrayEditSpreadsheetView
    lateinit var variableField: TextField
    lateinit var axisGrid: GridPane
    lateinit var axisEditPanel: HBox

    private val axisInputFields = ArrayList<Spinner<Int>>()
    private var defaultInsertionType = InsertStyleOption.RESHAPE

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
        client.calculationQueue.pushWriteVariableRequest(name, table.content.makeAPLArray()) { result ->
            if (result != null) {
                displayError("Error writing result to variable", result.message)
            }
        }
    }


    private fun displayError(title: String, details: String? = null) {
        displayErrorWithStage(stage, title, details)
    }

    fun loadArray(value: APLValue) {
        val d = value.dimensions
        if (d.size == 0) {
            throw IllegalArgumentException("Scalars cannot be loaded")
        }

        val content = MutableAPLValue(value)

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
                field.valueProperty().addListener { _, _, _ -> fillInItemsFromAxisFields(content) }
                addCol(field, "axisGrid-edit")
                axisInputFields.add(field)
            }
        }

        if (d.size > 1) {
            addCol(Label(d[d.size - 2].toString()), "axisGrid-axisLabel")
        }
        addCol(Label(d[d.size - 1].toString()), "axisGrid-axisLabel")

        fillInItemsFromAxisFields(content)
    }

    private fun axisFieldsToPosition(content: MutableAPLValue): IntArray {
        val d = content.dimensions
        assertx(axisInputFields.size == max(d.size - 2, 0))
        return IntArray(d.size) { i ->
            if (i < d.size - 2) {
                axisInputFields[i].value
            } else {
                0
            }
        }
    }

    private fun fillInItemsFromAxisFields(content: MutableAPLValue) {
        val position = axisFieldsToPosition(content)
        table.replaceContent(content, position)
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

    private fun displayInsertExpressionMenu() {
        val selection = SelectedArea.computeSelectedArea(table.selectionModel.selectedCells)
        println("Selected area: ${selection}")
        if (selection == null || selection.width == 0 || selection.height == 0) {
            return
        }

        val stage = Stage(StageStyle.UTILITY).apply {
            initModality(Modality.WINDOW_MODAL)
            title = "Expression"
        }

        val loader = FXMLLoader(ArrayEditor::class.java.getResource("insertexpression.fxml"))
        val root = loader.load<Parent>()
        val controller = loader.getController<InsertExpression>()

        controller.updateInsertionType(defaultInsertionType)

        controller.ok.onAction = EventHandler {
            sendExpressionRequest(controller.styledArea.text, selection, controller.insertStyleSelector.value.option)
            defaultInsertionType = controller.insertStyleSelector.value.option
            stage.close()
        }

        controller.cancel.onAction = EventHandler {
            stage.close()
        }

        stage.scene = Scene(root)
        stage.show()
        stage.requestFocus()
        controller.styledArea.requestFocus()
    }

    private fun sendExpressionRequest(text: String, selection: SelectedArea, insertionStyle: InsertStyleOption) {
        println("Sending request for: ${text}")
        val position = axisFieldsToPosition(table.content)
        val selectedValues = selection.computeSelectedValuesFromTable(table, position)
        val b = listOf(Pair("kap", "⍵") to selectedValues)
        client.calculationQueue.pushRequest(StringSourceLocation(text), variableBindings = b) { result ->
            println("Got result: ${result}")
            Platform.runLater {
                when (result) {
                    is Either.Left -> insertExpressionResult(position, selection, result.value.value, insertionStyle)
                    is Either.Right -> displayError("Error evaluating expression", formatExceptionDescription(result.value))
                }
            }
        }
    }

    private fun insertExpressionResult(position: IntArray, selection: SelectedArea, value: APLValue, insertionStyle: InsertStyleOption) {
        fun updateCellValue(currRow: ObservableList<SpreadsheetCell>, row: Int, col: Int, newValue: APLValue) {
            val p = updatePositionForInnerPos(position, row, col)
            val index = table.content.dimensions.indexFromPosition(p)
            table.content.updateValueAt(index, newValue)
//            val rowValue = table.grid.rows[row]
            currRow[col] = KapValueSpreadsheetCellType.createCell(table.content, row, col, index)
        }

        // This stuff is only needed because SpreadsheetView does not update its content
        // immediately unless the entire row is marked as updated.
        // If this is not done, then the visual representation of the spreadsheet
        // will remain unchanged until the selection is changed.
        fun copyRow(y: Int): ObservableList<SpreadsheetCell> {
            val newRow = FXCollections.observableArrayList<SpreadsheetCell>()
            newRow.addAll(table.grid.rows[y])
            return newRow
        }

        val dimensions = value.dimensions
        if (insertionStyle == InsertStyleOption.ERROR && (dimensions.size != 2 || dimensions[0] != selection.height || dimensions[1] != selection.width)) {
            displayError("Error inserting result", "Dimensions of result does not match size of selection")
            return
        }

        val size = dimensions.contentSize()
        val resultWidth = if (dimensions.size >= 2) dimensions[1] else 1

        fun computeValue(row: Int, col: Int): APLValue {
            return when (insertionStyle) {
                InsertStyleOption.REPEAT -> value
                InsertStyleOption.RESHAPE -> value.valueAt((row * selection.width + col).mod(size))
                InsertStyleOption.REPLICATE -> {
                    if (row < selection.height && col < selection.width) {
                        value.valueAt(row * resultWidth + col)
                    } else {
                        APLLONG_0
                    }
                }
                InsertStyleOption.ERROR -> if (row < selection.height && col < selection.width) {
                    value.valueAt(row * resultWidth + col)
                } else {
                    throw IllegalStateException("Dimensions doesn't match")
                }

            }
        }

        if (selection.isSingleElement) {
            val newRow = copyRow(selection.row)
            updateCellValue(newRow, selection.row, selection.column, value)
            table.grid.rows[selection.row] = newRow
        } else {
            var index = 0
            repeat(selection.height) { rowIndex ->
                val newRow = copyRow(rowIndex + selection.row)
                repeat(selection.width) { colIndex ->
                    val v = computeValue(rowIndex, colIndex)
                    index++
                    updateCellValue(newRow, rowIndex + selection.row, colIndex + selection.column, v)
                }
                table.grid.rows[rowIndex + selection.row] = newRow
            }
        }
    }

    // The warning is caused by a bug in IDEA
    // See https://youtrack.jetbrains.com/issue/KTIJ-20744 for details
    @Suppress("KotlinConstantConditions", "USELESS_IS_CHECK")
    private fun formatExceptionDescription(e: Exception) = when (e) {
        is APLGenericException -> e.formattedError()
        else -> e.message ?: "no information available"
    }

    fun openClicked(@Suppress("UNUSED_PARAMETER") actionEvent: ActionEvent) {
        val fileSelector = FileChooser().apply {
            title = "Open file"
            extensionFilters.setAll(
                FileChooser.ExtensionFilter("All", "*.*"),
                FileChooser.ExtensionFilter("CSV", "*.csv"),
                FileChooser.ExtensionFilter("Excel", "*.xls", "*.xlsx"))
        }
        val file = fileSelector.showOpenDialog(stage)
        if (file != null) {
            loadFile(file)
        }
    }

    private fun loadFile(file: File) {
        when (file.extension) {
            "csv" -> loadCsv(file)
            "xls", "xlsx" -> loadXls(file)
            else -> displayError("Can't open file", "Unknown file type: ${file.extension}")
        }
    }

    private fun loadCsv(file: File) {
        try {
            val content = readCsv(openInputCharFile(file.absolutePath))
            loadArray(content)
        } catch (e: CsvParseException) {
            displayError("Error reading CSV", "Parse error while reading ${file.name}: ${e.message}")
        }
    }

    private fun loadXls(file: File) {
        try {
            val content = readExcelFile(file.absolutePath)
            loadArray(content)
        } catch (e: IOException) {
            displayError("Error reading Excel file", "Error reading ${file.name}: ${e.message}")
        }
    }

    data class SelectedArea(val column: Int, val row: Int, val width: Int, val height: Int) {
        val isSingleElement get() = width == 1 && height == 1

        fun computeSelectedValuesFromTable(table: ArrayEditSpreadsheetView, position: IntArray): APLValue {
            return if (isSingleElement) {
                val p = updatePositionForInnerPos(position, row, column)
                table.content.valueAt(table.content.dimensions.indexFromPosition(p))
            } else {
                val result = ArrayList<APLValue>()
                repeat(height) { rowIndex ->
                    repeat(width) { colIndex ->
                        val p = updatePositionForInnerPos(position, rowIndex + row, colIndex + column)
                        result.add(table.content.valueAt(table.content.dimensions.indexFromPosition(p)))
                    }
                }
                APLArrayList(dimensionsOfSize(height, width), result)
            }
        }

        companion object {
            fun computeSelectedArea(selectedCells: ObservableList<TablePosition<*, *>>): SelectedArea? {
                if (selectedCells.size == 0) {
                    return null
                }

                val minCol = selectedCells.minOf { it.column }
                val maxCol = selectedCells.maxOf { it.column }
                val minRow = selectedCells.minOf { it.row }
                val maxRow = selectedCells.maxOf { it.row }

                val numCols = (maxCol - minCol) + 1
                val numRows = (maxRow - minRow) + 1
                val markers = IntArray(numRows * numCols)
                selectedCells.forEach { p ->
                    markers[((p.row - minRow) * numCols) + (p.column - minCol)]++
                }
                if (!markers.all { it == 1 }) {
                    return null
                }

                return SelectedArea(minCol, minRow, numCols, numRows)
            }
        }
    }

    companion object {
        private fun updatePositionForInnerPos(position: IntArray, row: Int, col: Int): IntArray {
            val p = IntArray(position.size) { i ->
                when (i) {
                    position.size - 1 -> col
                    position.size - 2 -> row
                    else -> position[i]
                }
            }
            return p
        }

        private fun makeArrayEditor(client: Client): ArrayEditor {
            val loader = FXMLLoader(ArrayEditor::class.java.getResource("arrayeditor.fxml"))
            val root = loader.load<Parent>()
            val controller = loader.getController<ArrayEditor>()

            controller.client = client
            controller.stage = Stage()
            val scene = Scene(root, 900.0, 850.0)
            controller.stage.title = "Array Editor"
            controller.stage.scene = scene

//            controller.table.contextMenu = ContextMenu(MenuItem("Paste").apply { onAction = EventHandler { controller.pasteToTable() } })

            controller.table.insertExpressionCallback = controller::displayInsertExpressionMenu
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
