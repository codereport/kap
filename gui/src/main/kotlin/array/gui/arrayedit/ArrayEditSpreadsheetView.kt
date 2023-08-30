package array.gui.arrayedit

import array.*
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.control.SeparatorMenuItem
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import org.controlsfx.control.spreadsheet.GridBase
import org.controlsfx.control.spreadsheet.SpreadsheetCell
import org.controlsfx.control.spreadsheet.SpreadsheetView
import java.util.*

class ArrayEditSpreadsheetView : SpreadsheetView() {
    var content: MutableAPLValue
    var displayedPosition: IntArray
    var insertExpressionCallback: (() -> Unit)? = null

    init {
        content = MutableAPLValue(APLArrayImpl(dimensionsOfSize(2, 2), arrayOf(APLLONG_1, APLLONG_1, APLString("Foo"), APLString("test"))))
        displayedPosition = intArrayOf(0, 0)
        updateGridBase()
    }

    fun replaceContent(newContent: MutableAPLValue, position: IntArray) {
        content = newContent
        displayedPosition = position
        updateGridBase()
    }

    private fun updateGridBase() {
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

        grid = gridBase
        setGrid(grid)
        columns.forEach { col ->
            col.setPrefWidth(50.0)
        }
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
            val cell = KapValueSpreadsheetCellType.createCell(content, rowIndex, 0, rowIndex)
            grid.rows.add(rowIndex, FXCollections.observableArrayList(cell))
        } else {
            content.insert(d.size - 2, rowIndex, 1)
            val d0 = content.dimensions
            val w = d0[d0.size - 1]
            val p = d0.indexFromPosition(displayedPosition) + (w * rowIndex)
            val cells = FXCollections.observableArrayList<SpreadsheetCell>()
            repeat(w) { i ->
                val cell = KapValueSpreadsheetCellType.createCell(content, rowIndex, i, p + i)
                cells.add(cell)
            }
            grid.rows.add(rowIndex, FXCollections.observableArrayList(cells))
        }
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
            val d0 = content.dimensions
            val w = d0[d0.size - 1]
            val h = d0[d0.size - 2]
            val p = d0.indexFromPosition(displayedPosition)
            val rows = grid.rows
            repeat(h) { i ->
                val cell = KapValueSpreadsheetCellType.createCell(content, i, colIndex, p + (i * w) + colIndex)
                val row = rows[i]
                row.add(colIndex, cell)
                rows[i] = row
            }
        }
    }

    private fun groupByAscending(list: List<Int>): List<Pair<Int, Int>> {
        if (list.isEmpty()) {
            return emptyList()
        } else {
            var start = list.first()
            var curr = start
            val res = ArrayList<Pair<Int, Int>>()
            list.rest().forEach { v ->
                if (v <= curr) {
                    throw IllegalArgumentException("List is not strictly increasing")
                }
                if (v != curr + 1) {
                    res.add(Pair(start, curr - start + 1))
                    start = v
                }
                curr = v
            }
            res.add(Pair(start, curr - start + 1))
            return res
        }
    }

    private fun deleteRows() {
        val rowIndexes = TreeSet<Int>()
        selectionModel.selectedCells.forEach { cell ->
            rowIndexes.add(cell.row)
        }
        groupByAscending(rowIndexes.toList()).asReversed().forEach { (index, n) ->
            content.remove(0, index, n)
        }
        updateGridBase()
    }

    private fun deleteCols() {
        val colIndexes = TreeSet<Int>()
        selectionModel.selectedCells.forEach { cell ->
            colIndexes.add(cell.column)
        }
        groupByAscending(colIndexes.toList()).asReversed().forEach { (index, n) ->
            content.remove(1, index, n)
        }
        updateGridBase()
    }

    override fun getSpreadsheetViewContextMenu(): ContextMenu {
        val menu = super.getSpreadsheetViewContextMenu()
        menu.items.add(MenuItem("Insert expression").apply {
            onAction = EventHandler {
                insertExpressionCallback!!()
            }
            accelerator = KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN)
        })
        menu.items.add(SeparatorMenuItem())
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
        menu.items.add(SeparatorMenuItem())
        menu.items.add(MenuItem("Delete rows").apply {
            onAction = EventHandler {
                deleteRows()
            }
        })
        menu.items.add(MenuItem("Delete columns").apply {
            onAction = EventHandler {
                deleteCols()
            }
        })
        return menu
    }
}
