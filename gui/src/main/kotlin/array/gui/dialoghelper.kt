package array.gui

import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.stage.Stage

fun displayErrorWithStage(stage: Stage?, title: String, details: String? = null) {
    val dialog = Alert(Alert.AlertType.ERROR, title, ButtonType.CLOSE)
    if (stage != null) {
        dialog.initOwner(stage)
    }
    if (details != null) {
        dialog.dialogPane.contentText = details
    }
    dialog.showAndWait()
}
