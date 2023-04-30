package array.gui

import array.APLEvalException
import array.Position
import array.StorageStack
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.util.Callback

class StackTrace {
    lateinit var borderPane: Parent
    lateinit var stackTraceTable: TableView<StackTraceRow>
    lateinit var stackTraceLevelColumn: TableColumn<StackTraceRow, Int>
    lateinit var stackTraceNameColumn: TableColumn<StackTraceRow, String>

    fun updateException(ex: APLEvalException) {
        val rows = ArrayList<StackTraceRow>().apply {
            add(StackTraceRow(0, CallStackEntry(ex.formattedError(), ex.pos), ex.formattedError()))
            ex.callStack?.let { callStack ->
                callStack.asReversed().forEachIndexed { i, element ->
                    add(StackTraceRow(i + 1, CallStackEntry(element), null))
                }
            }
        }
        stackTraceTable.items.setAll(rows)
    }

    companion object {
        fun makeStackTraceWindow(client: Client): StackTrace {
            val loader = FXMLLoader(StackTrace::class.java.getResource("stack.fxml"))
            loader.load<Parent>()
            val controller = loader.getController<StackTrace>()

            controller.stackTraceTable.items = FXCollections.observableArrayList()
            controller.stackTraceLevelColumn.apply {
                cellValueFactory = StackTraceLevelCellValueFactory()
                cellFactory = StackTraceLevelCellFactory()
            }
            controller.stackTraceNameColumn.apply {
                cellValueFactory = StackTraceNameCellValueFactory()
                cellFactory = StackTraceNameCellFactory()
            }
            @Suppress("UNUSED_ANONYMOUS_PARAMETER")
            val listener = ChangeListener<StackTraceRow> { observable, oldValue, newValue ->
                if (newValue != null) {
                    val entryPos = newValue.entry.pos
                    if (entryPos != null) {
                        client.highlightSourceLocation(entryPos, newValue.message)
                    }
                }
            }
            controller.stackTraceTable.selectionModel.selectedItemProperty().addListener(listener)

            return controller
        }
    }
}

data class CallStackEntry(val description: String, val pos: Position?) {
    constructor(frame: StorageStack.StackFrameDescription) : this(frame.name, frame.pos)
}

data class StackTraceRow(val level: Int, val entry: CallStackEntry, val message: String?)

/////////////////////////////////////////////
// Level
/////////////////////////////////////////////

class StackTraceLevelCellFactory : Callback<TableColumn<StackTraceRow, Int>, TableCell<StackTraceRow, Int>> {
    override fun call(param: TableColumn<StackTraceRow, Int>): TableCell<StackTraceRow, Int> {
        return StackTraceLevelCell()
    }
}

class StackTraceLevelCellValueFactory : Callback<TableColumn.CellDataFeatures<StackTraceRow, Int>, ObservableValue<Int>> {
    override fun call(param: TableColumn.CellDataFeatures<StackTraceRow, Int>): ObservableValue<Int> {
        return SimpleObjectProperty(param.value.level)
    }
}

class StackTraceLevelCell : TableCell<StackTraceRow, Int>() {
    override fun updateItem(item: Int?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            text = null
            graphic = null
        } else {
            text = item.toString()
        }
    }
}

/////////////////////////////////////////////
// Name
/////////////////////////////////////////////

class StackTraceNameCellFactory : Callback<TableColumn<StackTraceRow, String>, TableCell<StackTraceRow, String>> {
    override fun call(param: TableColumn<StackTraceRow, String>): TableCell<StackTraceRow, String> {
        return StackTraceNameCell()
    }
}

class StackTraceNameCellValueFactory : Callback<TableColumn.CellDataFeatures<StackTraceRow, String>, ObservableValue<String>> {
    override fun call(param: TableColumn.CellDataFeatures<StackTraceRow, String>): ObservableValue<String> {
        return SimpleObjectProperty(param.value.entry.description)
    }
}

class StackTraceNameCell : TableCell<StackTraceRow, String>() {
    override fun updateItem(item: String?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            text = null
            graphic = null
        } else {
            text = item
        }
    }
}
