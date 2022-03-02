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
        assert(d.size == displayedPosition.size)
        assert(displayedPosition[displayedPosition.size - 1] == 0)
        if (d.size > 1) {
            assert(displayedPosition[displayedPosition.size - 2] == 0)
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
        val row = selectionModel.selectedCells.minOf { it.row }
        insertRow(row)
    }

    private fun insertRowBelow() {
        val row = selectionModel.selectedCells.maxOf { it.row }
        insertRow(row + 1)
    }

    private fun insertRow(rowIndex: Int) {
        val d = content.dimensions
        if (d.size == 1) {
            content.insert(0, rowIndex, 1)
            grid.rows.add(
                rowIndex,
                FXCollections.observableArrayList(KapValueSpreadsheetCellType.createCell(content, rowIndex, 0, rowIndex)))
        } else {
            content.insert(d.size - 2, rowIndex, 1)
            val rowData = FXCollections.observableArrayList<SpreadsheetCell>()
            val p = displayedPosition.copyOf()
            p[d.size - 2] = rowIndex
            val baseOffset = d.indexFromPosition(p)
            repeat(d.lastDimension()) { i ->
                rowData.add(KapValueSpreadsheetCellType.createCell(content, rowIndex, i, baseOffset + i))
            }
            grid.rows.add(rowIndex, rowData)
        }
    }

    private fun insertColLeft() {
        val col = selectionModel.selectedCells.minOf { it.column }
        insertCol(col)
    }

    private fun insertColRight() {
        val col = selectionModel.selectedCells.maxOf { it.column }
        insertCol(col + 1)
    }

    private fun insertCol(colIndex: Int) {
        val d = content.dimensions
        if (d.size > 1) {
            content.insert(d.size - 1, colIndex, 1)
            val p = displayedPosition.copyOf()
            val baseOffset = d.indexFromPosition(p)
            val multipliers = d.multipliers()
            val stride = multipliers[1]
            val oldGrid = grid
            grid.columnHeaders.add(colIndex, "new")
            grid.rows.forEachIndexed { i, row ->
                row.add(colIndex, KapValueSpreadsheetCellType.createCell(content, i, colIndex, baseOffset + i * stride + colIndex))
            }
            setGrid(oldGrid)
        }
    }

    override fun getSpreadsheetViewContextMenu(): ContextMenu {
        println("Creating menu")
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
