package array.gui.arrayedit

import array.FormatStyle
import javafx.event.EventHandler
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import org.controlsfx.control.spreadsheet.SpreadsheetCellEditor
import org.controlsfx.control.spreadsheet.SpreadsheetView


class KapValueEditor(view: SpreadsheetView?) : SpreadsheetCellEditor(view) {
    private val textField = TextField()

    /***************************************************************************
     * * Public Methods * *
     */
    override fun startEdit(value: Any?, format: String, vararg options: Any) {
        if (value is APLValueSpreadsheetCell) {
            textField.text = value.value.formatted(FormatStyle.READABLE)
        }
        attachEnterEscapeEventHandler()
        textField.requestFocus()
        textField.selectAll()
    }

    override fun getControlValue(): String {
        return textField.text
    }

    override fun end() {
        textField.onKeyPressed = null
    }

    override fun getEditor(): TextField {
        return textField
    }

    private fun attachEnterEscapeEventHandler() {
        textField.onKeyPressed = EventHandler { event ->
            if (event.getCode() === KeyCode.ENTER) {
                endEdit(true)
            } else if (event.getCode() === KeyCode.ESCAPE) {
                endEdit(false)
            }
        }
    }
}
