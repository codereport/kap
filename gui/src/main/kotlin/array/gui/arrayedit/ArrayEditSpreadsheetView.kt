package array.gui.arrayedit

import array.APLArrayImpl
import array.APLLONG_1
import array.APLString
import array.dimensionsOfSize
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.control.TablePosition
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import org.controlsfx.control.spreadsheet.Grid
import org.controlsfx.control.spreadsheet.GridBase
import org.controlsfx.control.spreadsheet.SpreadsheetCell
import org.controlsfx.control.spreadsheet.SpreadsheetView

class ArrayEditSpreadsheetView : SpreadsheetView() {
    var content: MutableAPLValue
    var insertExpressionCallback: (() -> Unit)? = null

    init {
        content = MutableAPLValue(APLArrayImpl(dimensionsOfSize(2, 2), arrayOf(APLLONG_1, APLLONG_1, APLString("Foo"), APLString("test"))))
        grid = makeGridBase(intArrayOf(0, 0))
    }

    fun replaceContent(newContent: MutableAPLValue, position: IntArray) {
        content = newContent
        grid = makeGridBase(position)
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

    private fun insertRowAbove() {
        selectionModel.selectedCells.minOf { it.row }.let { row ->
            content.insert(0, row, 1)
        }
    }

    private fun insertRowBelow() {

    }

    override fun getSpreadsheetViewContextMenu(): ContextMenu {
        println("Creating menu")
        val menu = super.getSpreadsheetViewContextMenu()
        menu.items.add(MenuItem("Insert expression").apply {
            onAction = EventHandler { event ->
                insertExpressionCallback!!()
            }
            accelerator = KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN)
        })
        menu.items.add(MenuItem("Insert row above").apply {
            onAction = EventHandler { event ->
                insertRowAbove()
            }
        })
        menu.items.add(MenuItem("Insert row below").apply {
            onAction = EventHandler { event ->
                insertRowBelow()
            }
        })
        return menu
    }
}
