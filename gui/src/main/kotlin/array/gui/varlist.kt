package array.gui

import array.*
import array.rendertext.renderStringValue
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
import java.lang.Integer.min


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
            TableRowExpanderColumn<ValueWrapper> { dataFeatures -> createEditor(dataFeatures.value.valueInt) }

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

        table.columns.setAll(/*expanderColumn,*/ namespaceNameColumn, symbolNameColumn, valueColumn)

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
