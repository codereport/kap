package array.gui.arrayedit

import array.APLLONG_0
import array.APLValue
import array.FormatStyle
import javafx.util.StringConverter
import org.controlsfx.control.spreadsheet.*

object KapValueSpreadsheetCellType : SpreadsheetCellType<APLValue>(KapValueStringConverter) {
    override fun toString(obj: APLValue): String {
        return converter.toString(obj)
    }

    override fun createEditor(view: SpreadsheetView): SpreadsheetCellEditor {
        TODO("not implemented")
    }

    override fun match(value: Any, vararg options: Any): Boolean {
        return true
    }

    override fun convertValue(value: Any?): APLValue? {
        return if (value == null) {
            null
        } else {
            converter.fromString(value.toString())
        }
    }

    fun createCell(row: Int, col: Int, value: APLValue): SpreadsheetCell {
        return SpreadsheetCellBase(row, col, 1, 1, this).apply {
            item = value
        }
    }
}

object KapValueStringConverter : StringConverter<APLValue>() {
    override fun toString(obj: APLValue): String {
        return obj.formatted(FormatStyle.READABLE)
    }

    override fun fromString(string: String): APLValue {
        return APLLONG_0
    }
}
