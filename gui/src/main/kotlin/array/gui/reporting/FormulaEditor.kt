package array.gui.reporting

import array.gui.styledarea.InputFieldStyledArea
import javafx.event.ActionEvent
import javafx.fxml.FXMLLoader
import javafx.scene.control.Dialog
import javafx.scene.control.DialogPane
import javafx.scene.control.TextField

class Formula

class FormulaEditor {
    lateinit var nameField: TextField
    lateinit var expressionField: InputFieldStyledArea

    fun okClicked(@Suppress("UNUSED_PARAMETER") actionEvent: ActionEvent) {
        println("adding formula. name=${nameField.text}, expr=${expressionField.text}")
    }

    fun cancelClicked(@Suppress("UNUSED_PARAMETER") actionEvent: ActionEvent) {

    }

    companion object {
        fun loader() = FXMLLoader(FormulaEditor::class.java.getResource("formula-editor.fxml"))

        fun open() {
            val loader = loader()
            val parent: DialogPane = loader.load()
            val editor: FormulaEditor = loader.getController()

            val dialog = Dialog<Formula>()
            dialog.dialogPane = parent

            val res = dialog.showAndWait()
            println("res = ${res}")
        }
    }
}
