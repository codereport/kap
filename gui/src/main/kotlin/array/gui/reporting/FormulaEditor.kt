package array.gui.reporting

import array.gui.styledarea.InputFieldStyledArea
import javafx.fxml.FXMLLoader
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.control.DialogPane
import javafx.scene.control.TextField

class FormulaEditor {
    lateinit var reportingClient: ReportingClient
    lateinit var nameField: TextField
    lateinit var expressionField: InputFieldStyledArea

    fun formula(): FormulaEditorResult? {
        val name = nameField.text.trim()
        val expr = expressionField.text.trim()
        return if (name.isNotEmpty() && expr.isNotEmpty()) {
            FormulaEditorResult(name, expr)
        } else {
            null
        }
    }

    companion object {
        fun loader() = FXMLLoader(FormulaEditor::class.java.getResource("formula-editor.fxml"))

        fun open(reportingClient: ReportingClient): FormulaEditorResult? {
            val loader = loader()
            val parent: DialogPane = loader.load()
            val editor: FormulaEditor = loader.getController()
            editor.reportingClient = reportingClient

            val dialog = Dialog<ButtonType>()
            dialog.dialogPane = parent

            val res = dialog.showAndWait()
            return if (res.isPresent && res.get() == ButtonType.OK) {
                editor.formula()
            } else {
                null
            }
        }
    }

    data class FormulaEditorResult(val name: String, val expr: String)
}
