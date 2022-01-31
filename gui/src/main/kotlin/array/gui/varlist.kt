package array.gui

import array.*
import array.csv.writeCsv
import array.msofficereader.saveExcelFile
import array.rendertext.renderStringValue
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.layout.BorderPane
import javafx.util.Callback
import org.controlsfx.control.tableview2.FilteredTableColumn
import org.controlsfx.control.tableview2.FilteredTableView
import org.controlsfx.control.tableview2.filter.popupfilter.PopupStringFilter
import java.io.FileWriter
import kotlin.math.min


class VariableListController(val client: Client) {
    lateinit var borderPane: BorderPane
    lateinit var table: FilteredTableView<ValueWrapper>

    val node get() = borderPane
    val content = FXCollections.observableArrayList<ValueWrapper>()

    init {
        val loader = FXMLLoader(VariableListController::class.java.getResource("varlist.fxml"))
        loader.setController(this)
        loader.load<Parent>()

//        val expanderColumn: TableRowExpanderColumn<ValueWrapper> =
//            TableRowExpanderColumn<ValueWrapper> { dataFeatures -> createEditor(dataFeatures.value.valueInt) }

        val namespaceNameColumn = FilteredTableColumn<ValueWrapper, String>("N")
        namespaceNameColumn.cellValueFactory = PropertyValueFactory("namespaceName")
        PopupStringFilter(namespaceNameColumn).let { f ->
            namespaceNameColumn.setOnFilterAction { f.showPopup() }
        }
        namespaceNameColumn.cellFactory = VarListCellFactory(client)

        val symbolNameColumn = FilteredTableColumn<ValueWrapper, String>("Name")
        symbolNameColumn.cellValueFactory = PropertyValueFactory("symbolName")
        PopupStringFilter(symbolNameColumn).let { f ->
            symbolNameColumn.setOnFilterAction { f.showPopup() }
        }
        symbolNameColumn.cellFactory = VarListCellFactory(client)

        val valueColumn = TableColumn<ValueWrapper, String>("Value")
        valueColumn.cellValueFactory = PropertyValueFactory("value")
        valueColumn.cellFactory = VarListCellFactory(client)

        table.columns.setAll(/*expanderColumn,*/ namespaceNameColumn, symbolNameColumn, valueColumn)

        content.setAll(loadVariableContent(client.engine.rootContext.findVariables()))
        client.calculationQueue.addTaskCompletedHandler { engine ->
            val updated = engine.rootContext.findVariables()
            Platform.runLater {
                content.clear()
                content.addAll(loadVariableContent(updated))
            }
        }

        table.items = content
    }

    private fun loadVariableContent(updated: List<Pair<Symbol, APLValue?>>): ArrayList<ValueWrapper> {
        val result = ArrayList<ValueWrapper>()
        updated.forEach { (k, v) ->
            if (v != null) {
                result.add(ValueWrapper(k, v))
            }
        }
        return result
    }

    private fun createEditor(value: APLValue): Node {
        return Label("foo")
    }
}

class VarListCellFactory(val client: Client) : Callback<TableColumn<ValueWrapper, String>, TableCell<ValueWrapper, String>> {
    override fun call(param: TableColumn<ValueWrapper, String>?): TableCell<ValueWrapper, String> {
        return object : TableCell<ValueWrapper, String>() {
            init {
                contextMenu = ContextMenu(
                    MenuItem("Export to Excel").apply { onAction = EventHandler { exportToExcel(client, tableRow.item.valueInt) } },
                    MenuItem("Export to CSV").apply { onAction = EventHandler { exportToCsv(client, tableRow.item.valueInt) } })
            }

            override fun updateItem(item: String?, empty: Boolean) {
                super.updateItem(item, empty)
                text = if (empty) null else item
                graphic = null
            }
        }
    }
}

private fun exportToExcel(client: Client, value: APLValue) {
    val file = client.selectFile(forSave = true, nameHeader = "Select file")
    if (file != null) {
        client.withErrorDialog("Export to Excel") {
            saveExcelFile(value, file.path)
        }
    }
}

private fun exportToCsv(client: Client, value: APLValue) {
    val file = client.selectFile(forSave = true, nameHeader = "Select file")
    if (file != null) {
        client.withErrorDialog("Export to CSV") {
            WriterCharacterConsumer(FileWriter(file, Charsets.UTF_8)).use { dest ->
                writeCsv(dest, value)
            }
        }
    }
}

class ValueWrapper(val sym: Symbol, val valueInt: APLValue) {
    @Suppress("unused")
    val namespaceName
        get() = sym.namespace.name

    @Suppress("unused")
    val symbolName
        get() = sym.symbolName

    val value
        get() = valueInt.dimensions.let { d ->
            renderSimple(valueInt)
                ?: when (d.size) {
                    0 -> valueInt.toString()
                    1 -> StringBuilder().let { buf ->
                        (0 until min(5, d[0])).forEach { i ->
                            if (i > 0) {
                                buf.append(" ")
                            }
                            val v = valueInt.valueAt(i)
                            buf.append(renderSimple(valueInt.valueAt(i)) ?: "(dimensions: ${v.dimensions})")
                        }
                        if (d[0] >= 5) {
                            buf.append(" (${d[0]} elements)")
                        }
                        buf.toString()
                    }
                    else -> "(array: ${d})"
                }
        }

    private fun renderSimple(value: APLValue): String? {
        value.dimensions
        return when {
            isNullValue(value) -> "⍬"
            value is APLSingleValue -> value.formatted(FormatStyle.PLAIN)
            value.isStringValue() -> renderStringValue(value, FormatStyle.PRETTY)
            else -> null
        }
    }
}
