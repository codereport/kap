package array.gui.arrayedit

import array.*
import javafx.beans.value.ChangeListener
import javafx.util.StringConverter
import org.controlsfx.control.spreadsheet.*

object KapValueSpreadsheetCellType : SpreadsheetCellType<APLValue>(KapValueStringConverter) {
    override fun toString(obj: APLValue): String {
        return converter.toString(obj)
    }

    override fun createEditor(view: SpreadsheetView): SpreadsheetCellEditor {
        return SpreadsheetCellEditor.StringEditor(view)
    }

    override fun match(value: Any, vararg options: Any): Boolean {
        return true
    }

    override fun convertValue(value: Any?): APLValue? {
        return when (value) {
            null -> null
            is APLValue -> value
            else -> converter.fromString(value.toString())
        }
    }

    fun createCell(value: MutableAPLValue, row: Int, col: Int, index: Int): SpreadsheetCell {
        return SpreadsheetCellBase(row, col, 1, 1, this).apply {
            item = value.elements[index]
            val l = ChangeListener<Any> { observable, oldValue, newValue ->
                value.elements[index] = if (newValue == null) APLNullValue.APL_NULL_INSTANCE else newValue as APLValue
            }
            itemProperty().addListener(l)
        }
    }
}

object KapValueStringConverter : StringConverter<APLValue>() {
    override fun toString(obj: APLValue): String {
        return obj.formatted(FormatStyle.READABLE)
    }

    override fun fromString(string: String): APLValue {
        val tokeniser = TokenGenerator(Engine(), StringSourceLocation(string))
        try {
            val token = tokeniser.nextToken()
            if (token == EndOfFile) {
                throw Exception("Empty input")
            }
            tokeniser.nextToken().let { next ->
                unless(next == EndOfFile) {
                    throw Exception("Expected EndOfFile, found: ${next}")
                }
            }
            return when (token) {
                is StringToken -> APLString(token.value)
                is ParsedLong -> token.value.makeAPLNumber()
                is ParsedDouble -> token.value.makeAPLNumber()
                is ParsedComplex -> token.value.makeAPLNumber()
                else -> throw Exception("Illegal input")
            }
        } catch (e: ParseException) {
            throw Exception("Parse exception", e)
        }
    }
}
