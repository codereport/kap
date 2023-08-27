package array.gui.arrayedit

import array.*
import javafx.beans.value.ChangeListener
import javafx.util.StringConverter
import org.controlsfx.control.spreadsheet.*

object KapValueSpreadsheetCellType : SpreadsheetCellType<APLValueSpreadsheetCell>(KapValueStringConverter) {
    override fun toString(obj: APLValueSpreadsheetCell): String {
        return converter.toString(obj)
    }

    override fun createEditor(view: SpreadsheetView): SpreadsheetCellEditor {
        return KapValueEditor(view)
    }

    override fun match(value: Any, vararg options: Any): Boolean {
        if (value !is String) {
            println("Attempt to match non-string value")
            return false
        }
        val result = KapValueStringConverter.fromStringOrNull(value)
        return result != null
    }

    override fun convertValue(value: Any?): APLValueSpreadsheetCell? {
        return when (value) {
            null -> null
            is APLValueSpreadsheetCell -> value
            else -> converter.fromString(value.toString())
        }
    }

    fun createCell(value: MutableAPLValue, row: Int, col: Int, index: Int): SpreadsheetCell {
        return SpreadsheetCellBase(row, col, 1, 1, this).apply {
            item = APLValueSpreadsheetCell(value.elements[index])
            val l = ChangeListener<Any> { observable, oldValue, newValue ->
                value.elements[index] =
                    if (newValue == null) APLNullValue.APL_NULL_INSTANCE else (newValue as APLValueSpreadsheetCell).value
            }
            itemProperty().addListener(l)
        }
    }
}

object KapValueStringConverter : StringConverter<APLValueSpreadsheetCell>() {
    override fun toString(obj: APLValueSpreadsheetCell): String {
        return obj.value.formatted(FormatStyle.READABLE)
    }

    fun fromStringOrNull(string: String): APLValue? {
        val tokeniser = TokenGenerator(Engine(), StringSourceLocation(string))
        try {
            val token = tokeniser.nextToken()
            if (token == EndOfFile) {
                return null
            }
            tokeniser.nextToken().let { next ->
                unless(next == EndOfFile) {
                    return null
                }
            }
            val value = when (token) {
                is StringToken -> APLString(token.value)
                is ParsedLong -> token.value.makeAPLNumber()
                is ParsedDouble -> token.value.makeAPLNumber()
                is ParsedComplex -> token.value.makeAPLNumber()
                else -> throw Exception("Illegal input")
            }
            return value
        } catch (e: ParseException) {
            return null
        }
    }

    override fun fromString(string: String): APLValueSpreadsheetCell {
        val result = fromStringOrNull(string)
        if (result == null) {
            throw IllegalStateException("Unable convert value from string")
        }
        return APLValueSpreadsheetCell(result)
    }
}

class APLValueSpreadsheetCell(val value: APLValue) {
    override fun toString(): String {
        return when {
            value.isStringValue() -> "\"${value.toStringValue()}\""
            value is APLLong -> value.formatted(FormatStyle.READABLE)
            value is APLDouble -> value.formatted(FormatStyle.READABLE)
            value is APLComplex -> value.formatted(FormatStyle.READABLE)
            else -> "\"cannot parse\""
        }
    }
}
