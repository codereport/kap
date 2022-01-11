package array.gui

import array.*
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.cell.CheckBoxTableCell
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.layout.BorderPane
import javafx.util.Callback
import org.controlsfx.control.tableview2.FilteredTableColumn
import org.controlsfx.control.tableview2.filter.popupfilter.PopupStringFilter
import java.util.*

class FunctionListController(engine: Engine) {
    lateinit var borderPane: BorderPane
    lateinit var table: TableView<FunctionWrapper>

    val node get() = borderPane
    val items: ObservableList<FunctionWrapper> = FXCollections.observableArrayList()

    init {
        val loader = FXMLLoader(FunctionListController::class.java.getResource("functionlist.fxml"))
        loader.setController(this)
        loader.load<Parent>()

        val namespaceNameColumn = FilteredTableColumn<FunctionWrapper, String>("N")
        namespaceNameColumn.cellValueFactory = PropertyValueFactory("namespaceName")
        PopupStringFilter(namespaceNameColumn).let { f ->
            namespaceNameColumn.setOnFilterAction { f.showPopup() }
        }

        val symbolNameColumn = FilteredTableColumn<FunctionWrapper, String>("Name")
        symbolNameColumn.cellValueFactory = PropertyValueFactory("symbolName")
        PopupStringFilter(symbolNameColumn).let { f ->
            symbolNameColumn.setOnFilterAction { f.showPopup() }
        }

        val userFunctionColumn = FilteredTableColumn<FunctionWrapper, Boolean>("U")
        userFunctionColumn.cellValueFactory = PropertyValueFactory("userFunction")
        userFunctionColumn.cellFactory = makeCheckBoxFactory()

        val exportedFunctionColumn = FilteredTableColumn<FunctionWrapper, Boolean>("E")
        exportedFunctionColumn.cellValueFactory = PropertyValueFactory("exported")
        exportedFunctionColumn.cellFactory = makeCheckBoxFactory()

        table.columns.setAll(namespaceNameColumn, symbolNameColumn, userFunctionColumn, exportedFunctionColumn)

        fillFunctionList(engine)

        table.items = items

        val listener = FunctionListener()
        engine.addFunctionDefinitionListener(listener)
    }

    private fun makeCheckBoxFactory() =
        Callback<TableColumn<FunctionWrapper, Boolean>, TableCell<FunctionWrapper, Boolean>> { tableColumn ->
            CheckBoxTableCell { i ->
                if (i == null) {
                    null
                } else {
                    SimpleBooleanProperty(tableColumn.getCellData(i))
                }
            }
        }

    private fun fillFunctionList(engine: Engine) {
        val fnList =
            engine.getFunctions().sortedBy(Pair<Symbol, APLFunctionDescriptor>::first)
                .map { (name, fn) -> FunctionWrapper(name, fn) }
        items.setAll(fnList)
    }

    private inner class FunctionListener : FunctionDefinitionListener {
        override fun functionDefined(name: Symbol, fn: APLFunctionDescriptor) {
            Platform.runLater {
                val functionRow = FunctionWrapper(name, fn)
                val result = Collections.binarySearch(items, functionRow) { a, b -> a.sym.compareTo(b.sym) }
                if (result >= 0) {
                    items[result] = functionRow
                } else {
                    items.add(-result - 1, functionRow)
                }
            }
        }

        override fun functionRemoved(name: Symbol) {
            Platform.runLater {
                val result = items.indexOfFirst { name == it.sym }
                if (result >= 0) {
                    items.removeAt(result)
                }
            }
        }
    }
}

@Suppress("unused")
class FunctionWrapper(val sym: Symbol, val fn: APLFunctionDescriptor) {
    val namespaceName get() = sym.namespace.name
    val symbolName get() = sym.symbolName
    val userFunction get() = fn is APLParser.UpdateableFunction
    val exported get() = sym.namespace.findSymbol(sym.symbolName, includePrivate = false) != null
}
