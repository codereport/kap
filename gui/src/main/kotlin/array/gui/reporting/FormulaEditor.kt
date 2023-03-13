package array.gui.reporting

import array.gui.styledarea.InputFieldStyledArea
import javafx.event.ActionEvent
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.stage.Stage
import kotlin.math.exp

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
            val parent: Parent = loader.load()
            val editor: FormulaEditor = loader.getController()

            val stage = Stage()
            val scene = Scene(parent, 800.0, 600.0)
            stage.title = "Edit Formula"
            stage.scene = scene
            stage.show()
        }
    }
}
