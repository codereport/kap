package array.gui

import array.APLValue
import array.Symbol
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Label
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.layout.BorderPane
import org.controlsfx.control.table.TableRowExpanderColumn
import org.controlsfx.control.tableview2.FilteredTableColumn
import org.controlsfx.control.tableview2.filter.popupfilter.PopupStringFilter


class VariableListController(val client: Client) {
    lateinit var borderPane: BorderPane
    lateinit var table: TableView<ValueWrapper>

    val node get() = borderPane
    val content = FXCollections.observableArrayList<ValueWrapper>()

    init {
        val loader = FXMLLoader(FunctionListController::class.java.getResource("varlist.fxml"))
        loader.setController(this)
        loader.load<Parent>()

        val expanderColumn: TableRowExpanderColumn<ValueWrapper> =
            TableRowExpanderColumn<ValueWrapper> { dataFeatures -> createEditor(dataFeatures.value.value) }

        val namespaceNameColumn = FilteredTableColumn<ValueWrapper, String>("N")
        namespaceNameColumn.cellValueFactory = PropertyValueFactory("namespaceName")
        PopupStringFilter(namespaceNameColumn).let { f ->
            namespaceNameColumn.setOnFilterAction { f.showPopup() }
        }

        val symbolNameColumn = FilteredTableColumn<ValueWrapper, String>("Name")
        symbolNameColumn.cellValueFactory = PropertyValueFactory("symbolName")
        PopupStringFilter(symbolNameColumn).let { f ->
            symbolNameColumn.setOnFilterAction { f.showPopup() }
        }

        val valueColumn = TableColumn<ValueWrapper, APLValue>("Value")
        valueColumn.cellValueFactory = PropertyValueFactory("value")

        table.columns.setAll(expanderColumn, namespaceNameColumn, symbolNameColumn, valueColumn)

        content.clear()
        content.addAll(loadVariableContent(client.engine.rootContext.findVariables()))
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

@Suppress("unused")
class ValueWrapper(val sym: Symbol, val value: APLValue) {
    val namespaceName get() = sym.namespace.name
    val symbolName get() = sym.symbolName
}
