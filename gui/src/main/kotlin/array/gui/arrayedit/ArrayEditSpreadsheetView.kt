package array.gui.arrayedit

import array.*
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import org.controlsfx.control.spreadsheet.Grid
import org.controlsfx.control.spreadsheet.GridBase
import org.controlsfx.control.spreadsheet.SpreadsheetCell
import org.controlsfx.control.spreadsheet.SpreadsheetView

class ArrayEditSpreadsheetView : SpreadsheetView() {
    var content: MutableAPLValue
    var displayedPosition: IntArray
    var insertExpressionCallback: (() -> Unit)? = null

    init {
        content = MutableAPLValue(APLArrayImpl(dimensionsOfSize(2, 2), arrayOf(APLLONG_1, APLLONG_1, APLString("Foo"), APLString("test"))))
        displayedPosition = intArrayOf(0, 0)
        grid = makeGridBase()
    }

    fun replaceContent(newContent: MutableAPLValue, position: IntArray) {
        content = newContent
        displayedPosition = position
        grid = makeGridBase()
    }

    private fun makeGridBase(): Grid {
        val d = content.dimensions
        assertx(d.size == displayedPosition.size)
        assertx(displayedPosition[displayedPosition.size - 1] == 0)
        if (d.size > 1) {
            assertx(displayedPosition[displayedPosition.size - 2] == 0)
        }
        val baseIndex = if (d.contentSize() == 0) 0 else d.indexFromPosition(displayedPosition)
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
            (0 until numCols).map { i ->
                columnLabels?.get(i)?.title ?: i.toString()
            }
        }
        gridBase.columnHeaders.setAll(colHeaders)

        val rowLabels = content.labels?.labels?.get(if (d.size == 1) 0 else d.size - 2)
        val rowHeaders = (0 until numRows).map { i ->
            rowLabels?.get(i)?.title ?: i.toString()
        }
        gridBase.rowHeaders.setAll(rowHeaders)

        return gridBase
    }

    private fun insertRowAbove() {
        val row = selectionModel.selectedCells.minOfOrNull { it.row }
        insertRow(row ?: 0)
    }

    private fun insertRowBelow() {
        val row = selectionModel.selectedCells.maxOfOrNull { it.row }
        insertRow(if (row == null) 0 else row + 1)
    }

    private fun insertRow(rowIndex: Int) {
        val d = content.dimensions
        if (d.size == 1) {
            content.insert(0, rowIndex, 1)
        } else {
            content.insert(d.size - 2, rowIndex, 1)
        }
        grid = makeGridBase()
        setGrid(grid)
    }

    private fun insertColLeft() {
        val col = selectionModel.selectedCells.minOfOrNull { it.column }
        insertCol(col ?: 0)
    }

    private fun insertColRight() {
        val col = selectionModel.selectedCells.maxOfOrNull { it.column }
        insertCol(if (col == null) 0 else col + 1)
    }

    private fun insertCol(colIndex: Int) {
        val d = content.dimensions
        if (d.size > 1) {
            content.insert(d.size - 1, colIndex, 1)
            grid = makeGridBase()
            setGrid(grid)
        }
    }

    override fun getSpreadsheetViewContextMenu(): ContextMenu {
        val menu = super.getSpreadsheetViewContextMenu()
        menu.items.add(MenuItem("Insert expression").apply {
            onAction = EventHandler {
                insertExpressionCallback!!()
            }
            accelerator = KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN)
        })
        menu.items.add(MenuItem("Insert row above").apply {
            onAction = EventHandler {
                insertRowAbove()
            }
        })
        menu.items.add(MenuItem("Insert row below").apply {
            onAction = EventHandler {
                insertRowBelow()
            }
        })
        menu.items.add(MenuItem("Insert column left").apply {
            onAction = EventHandler {
                insertColLeft()
            }
        })
        menu.items.add(MenuItem("Insert column right").apply {
            onAction = EventHandler {
                insertColRight()
            }
        })
        return menu
    }
}
