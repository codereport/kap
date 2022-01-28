package array.gui.arrayedit

import array.APLValue

class ArrayEditorRow(value: APLValue, baseIndex: Int, rowIndex: Int, numCols: Int) {
    val values = Array(numCols) { i -> value.valueAt(baseIndex + (rowIndex * numCols + i)) }
}
