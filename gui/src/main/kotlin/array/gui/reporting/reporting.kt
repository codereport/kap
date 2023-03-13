package array.gui.reporting

import javafx.event.ActionEvent
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.ListView
import javafx.stage.Stage

class ReportingClient {
    lateinit var variableList: ListView<Any>

    fun addFormulaClicked(@Suppress("UNUSED_PARAMETER") actionEvent: ActionEvent) {
        FormulaEditor.open()
    }

    companion object {
        fun open() {
            val loader = FXMLLoader(ReportingClient::class.java.getResource("reporting-client.fxml"))
            val root: Parent = loader.load()
            val controller: ReportingClient = loader.getController()
            println("varlist = ${controller.variableList}")

            val stage = Stage()
            val scene = Scene(root, 800.0, 800.0)
            stage.title = "Report"
            stage.scene = scene
            stage.show()
        }
    }
}
