package array.gui

import array.gui.settings.ReturnBehaviour
import array.gui.settings.Settings
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.control.*
import javafx.scene.text.Font
import javafx.stage.Modality
import javafx.stage.Window
import javafx.util.Callback

class SettingsDialog(owner: Window, val prevData: Settings) : Dialog<Settings>() {
    lateinit var fontFamilyInput: ComboBox<String>
    lateinit var fontSizeInput: TextField
    lateinit var returnBehaviour: ChoiceBox<String>

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
        prevData.copy(
            fontFamily = fontFamilyInput.value,
            fontSize = Integer.parseInt(fontSizeInput.textProperty().valueSafe),
            newlineBehaviour = ReturnBehaviour.values()[returnBehaviour.selectionModel.selectedIndex])

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

        val returnBehaviourOptions = ReturnBehaviour.values().map(this::nameFromReturnBehaviour)
        returnBehaviour.items.setAll(returnBehaviourOptions)
        if (prevData.newlineBehaviour == null) {
            returnBehaviour.selectionModel.select(0)
        } else {
            returnBehaviour.selectionModel.select(returnBehaviourIndex(prevData.newlineBehaviour))
        }
    }

    private fun returnBehaviourIndex(defaultReturnBehaviour: ReturnBehaviour): Int {
        ReturnBehaviour.values().forEachIndexed { i, v -> if (v === defaultReturnBehaviour) return i }
        error("Unexpected return behaviour value: ${defaultReturnBehaviour}")
    }

    private fun nameFromReturnBehaviour(returnBehaviour: ReturnBehaviour): String {
        return when (returnBehaviour) {
            ReturnBehaviour.CLEAR_INPUT -> "Clear input"
            ReturnBehaviour.PRESERVE -> "Preserve content"
        }
    }
}
