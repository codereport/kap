package array.gui

import javafx.application.Platform
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.control.*
import javafx.scene.text.Font
import javafx.stage.Modality
import javafx.stage.Window
import javafx.util.Callback

class SettingsDialog(owner: Window, val prevData: SettingsData) : Dialog<SettingsData>() {
    lateinit var fontFamilyInput: ComboBox<String>
    lateinit var fontSizeInput: TextField

    init {
        val loader = FXMLLoader(SettingsDialog::class.java.getResource("settings.fxml"))
        loader.setController(this)
        val pane = loader.load<DialogPane>()
        initOwner(owner)
        initModality(Modality.APPLICATION_MODAL)
        isResizable = true
        title = "Settings"
        dialogPane = pane

        initFields()

        onShowing = EventHandler { Platform.runLater { fontSizeInput.requestFocus() } }

        resultConverter = Callback { buttonType ->
            when (buttonType.buttonData) {
                ButtonBar.ButtonData.OK_DONE -> makeSettingsData()
                else -> null
            }
        }
    }

    private fun makeSettingsData() =
        prevData.copy(fontFamily = fontFamilyInput.value, fontSize = Integer.parseInt(fontSizeInput.textProperty().valueSafe))

    private fun initFields() {
        val familyList = Font.getFamilies().sorted()
        fontFamilyInput.items.clear()
        fontFamilyInput.items.addAll(familyList)
        familyList.indexOf(prevData.fontFamily).let { index ->
            if (index != -1) {
                fontFamilyInput.selectionModel.select(index)
            }
        }

        fontSizeInput.text = prevData.fontSize.toString()
    }
}

data class SettingsData(val fontFamily: String, val fontSize: Int)
