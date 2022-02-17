package array.gui

import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.geometry.HPos
import javafx.geometry.VPos
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.scene.text.Font
import javafx.stage.Stage

class KeyboardHelpWindow(renderContext: ClientRenderContext) {
    private val stage = Stage()

    init {
        val loader = FXMLLoader(javaClass.getResource("keyboard.fxml"))
        val root: Parent = loader.load()
        val controller: KeyboardHelp = loader.getController()
        controller.init(renderContext)
        val scene = Scene(root, 800.0, 300.0)
        stage.title = "Keyboard Layout"
        stage.scene = scene
    }

    fun show() {
        stage.show()
    }
}

class KeyboardHelp {
    lateinit var borderPane: BorderPane
    lateinit var gridPane: GridPane
    lateinit var renderContext: ClientRenderContext

    fun init(context: ClientRenderContext) {
        renderContext = context
        forEachKeyLabel { label ->
            label.initLabel(renderContext.font())
        }
        updateShortcuts()
    }

    fun updateShortcuts() {
        renderContext.extendedInput().keymap.forEach { (key, value) ->
            forEachKeyLabel key@{ label ->
                if (label.upperLabel == key.character) {
                    label.setAltUpperLabel(value)
                    return@key
                }
                if (label.lowerLabel == key.character) {
                    label.setAltLowerLabel(value)
                    return@key
                }
            }
        }
    }

    fun forEachKeyLabel(fn: (KeyboardButtonLabel) -> Unit) {
        gridPane.children.forEach { label ->
            if (label is KeyboardButtonLabel) {
                fn(label)
            }
        }
    }
}

class KeyboardButtonLabel : AnchorPane() {
    private val altUpperFx = Label()
    private val altLowerFx = Label()
    private val upperFx = Label()
    private val lowerFx = Label()

    private var altUpperLabel: String = ""
    private var altLowerLabel: String = ""
    var clickable: Boolean = true

    init {
        styleClass.addAll("keyboard-button-label", "kapfont")
//        background = Background(BackgroundFill(Color(0.95, 0.95, 0.95, 1.0), CornerRadii.EMPTY, Insets.EMPTY))
//        padding = Insets(2.0, 2.0, 2.0, 2.0)
        maxWidth = Double.MAX_VALUE
        maxHeight = Double.MAX_VALUE
        GridPane.setHalignment(this, HPos.CENTER)
        GridPane.setValignment(this, VPos.CENTER)
        GridPane.setHgrow(this, Priority.ALWAYS)
        GridPane.setVgrow(this, Priority.ALWAYS)

        children.add(altUpperFx)
        children.add(altLowerFx)
        children.add(upperFx)
        children.add(lowerFx)

        val margin = 2.0
        setTopAnchor(altUpperFx, margin)
        setLeftAnchor(altUpperFx, margin)
        setBottomAnchor(altLowerFx, margin)
        setLeftAnchor(altLowerFx, margin)
        setTopAnchor(upperFx, margin)
        setRightAnchor(upperFx, margin)
        setBottomAnchor(lowerFx, margin)
        setRightAnchor(lowerFx, margin)
    }

    var upperLabel: String = ""
        get() = field
        set(s) {
            upperFx.text = s
            field = s
        }

    var lowerLabel: String = ""
        get() = field
        set(s) {
            lowerFx.text = s
            field = s
        }

    private fun handleClick(s: String) {
        println("send '${s}' to window")
    }

    fun setAltUpperLabel(s: String) {
        altUpperLabel = s
        altUpperFx.text = s
    }

    fun setAltLowerLabel(s: String) {
        altLowerLabel = s
        altLowerFx.text = s
    }

    fun initLabel(font: Font) {
//        altUpperFx.font = font
//        altLowerFx.font = font
//        upperFx.font = font
//        lowerFx.font = font

        if (clickable) {
            altUpperFx.apply {
                styleClass.add("keyboard-button-label-clickable")
                onMouseClicked = EventHandler { handleClick(altUpperLabel) }
            }
            altLowerFx.apply {
                styleClass.add("keyboard-button-label-clickable")
                onMouseClicked = EventHandler { handleClick(altLowerLabel) }
            }
            upperFx.apply {
                styleClass.add("keyboard-button-label-clickable")
                onMouseClicked = EventHandler { handleClick(upperLabel) }
            }
            lowerFx.apply {
                styleClass.add("keyboard-button-label-clickable")
                onMouseClicked = EventHandler { handleClick(lowerLabel) }
            }
        } else {
            styleClass.setAll("keyboard-button-label")
        }
    }
}
